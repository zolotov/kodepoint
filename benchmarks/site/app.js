(function () {
  'use strict';

  var SUITE_NAMES = { jvm: 'JVM', wasmJs: 'Wasm', 'character-data': 'Character Data' };
  var SUITE_ORDER = { jvm: 0, wasmJs: 1, 'character-data': 2 };
  var SVG_NS = 'http://www.w3.org/2000/svg';

  // "?pr=<number>" switches the dashboard to a pull-request view: the same layout,
  // fed from data/prs/<n>/ where history.json holds only that PR's runs.
  var prParam = new URLSearchParams(window.location.search).get('pr');
  var activePr = prParam && /^\d+$/.test(prParam) ? prParam : null;
  var dataBase = activePr ? 'data/prs/' + activePr + '/' : 'data/';

  function suiteName(suite) {
    return SUITE_NAMES[suite] || suite;
  }

  function suiteIndex(suite) {
    return suite in SUITE_ORDER ? SUITE_ORDER[suite] : 99;
  }

  function measurementKey(m) {
    return m.suite + '::' + m.name + '::' + m.unit;
  }

  function el(tag, className, text) {
    var node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
  }

  function svgEl(tag, attrs) {
    var node = document.createElementNS(SVG_NS, tag);
    if (attrs) {
      Object.keys(attrs).forEach(function (name) {
        node.setAttribute(name, attrs[name]);
      });
    }
    return node;
  }

  function link(href, text) {
    if (!/^https?:\/\//.test(href)) return document.createTextNode(text);
    var a = document.createElement('a');
    a.href = href;
    a.textContent = text;
    return a;
  }

  // For our own static hrefs; link() deliberately rejects non-http URLs from data.
  function relativeLink(href, text) {
    var a = document.createElement('a');
    a.href = href;
    a.textContent = text;
    return a;
  }

  function formatNumber(value, fractionDigits) {
    return value.toLocaleString('en-US', {
      minimumFractionDigits: fractionDigits,
      maximumFractionDigits: fractionDigits
    });
  }

  function valueFractionDigits(m) {
    if (m.unit === 'bytes') return 0;
    var abs = Math.abs(m.value);
    if (abs >= 100) return 2;
    if (abs >= 10) return 3;
    if (abs >= 1) return 4;
    return 5;
  }

  function formatValue(m) {
    return formatNumber(m.value, valueFractionDigits(m)) + ' ' + m.unit;
  }

  function formatDelta(entry) {
    if (!entry || !entry.baseline) return 'new';
    if (entry.deltaRatio === undefined || entry.deltaRatio === null) return 'n/a';
    if (Math.abs(entry.deltaRatio) < 1e-4) return '0.00%';
    var percent = entry.deltaRatio * 100;
    var formatted = (percent >= 0 ? '+' : '') + formatNumber(percent, 2) + '%';
    return entry.change === 'unchanged' ? formatted + ' (noise)' : formatted;
  }

  function deltaClass(entry) {
    if (!entry) return 'delta-neutral';
    if (entry.change === 'improvement') return 'delta-improvement';
    if (entry.change === 'regression') return 'delta-regression';
    return 'delta-neutral';
  }

  function shortDate(isoInstant) {
    return isoInstant ? isoInstant.slice(0, 10) : '';
  }

  function loadData() {
    // data.js only embeds the main-branch payload, so PR views always fetch.
    if (!activePr && window.BENCHMARK_DATA) return Promise.resolve(window.BENCHMARK_DATA);
    var names = ['latest', 'comparison', 'history'];
    return Promise.all(names.map(function (name) {
      return fetch(dataBase + name + '.json').then(function (response) {
        if (!response.ok) throw new Error('HTTP ' + response.status + ' for ' + dataBase + name + '.json');
        return response.json();
      });
    })).then(function (parts) {
      return { latest: parts[0], comparison: parts[1], history: parts[2] };
    });
  }

  function buildHistorySeries(history) {
    var seriesByKey = new Map();
    (history.runs || []).forEach(function (run) {
      (run.measurements || []).forEach(function (m) {
        var key = measurementKey(m);
        if (!seriesByKey.has(key)) seriesByKey.set(key, []);
        seriesByKey.get(key).push({
          value: m.value,
          lower: m.lowerValue,
          upper: m.upperValue,
          run: run
        });
      });
    });
    return seriesByKey;
  }

  /* ---------- metadata ---------- */

  function metaLine(label, valueNode) {
    var row = el('div');
    row.appendChild(el('dt', null, label));
    var dd = el('dd');
    if (typeof valueNode === 'string') {
      dd.textContent = valueNode;
    } else {
      dd.appendChild(valueNode);
    }
    row.appendChild(dd);
    return row;
  }

  function renderMeta(data) {
    var list = document.getElementById('meta-list');
    var meta = data.latest;
    var baseline = data.comparison.baseline;
    if (activePr) {
      document.title += ' — PR #' + activePr;
      var heading = document.querySelector('.page-header h1');
      if (heading) heading.textContent += ' — PR #' + activePr;
      list.appendChild(metaLine('Pull request', '#' + activePr + (meta.refName ? ' (' + meta.refName + ')' : '')));
      list.appendChild(metaLine('View', relativeLink('./', 'main dashboard')));
    }
    list.appendChild(metaLine('Generated', meta.generatedAt || 'n/a'));
    if (meta.refName) list.appendChild(metaLine('Ref', meta.refName));
    if (meta.commitSha) {
      list.appendChild(metaLine('Commit', meta.commitUrl
        ? link(meta.commitUrl, meta.commitSha.slice(0, 7))
        : meta.commitSha.slice(0, 7)));
    }
    if (meta.runUrl) list.appendChild(metaLine('Run', link(meta.runUrl, 'workflow')));
    list.appendChild(metaLine('Baseline', baseline
      ? [baseline.commitSha ? baseline.commitSha.slice(0, 7) : null, baseline.generatedAt]
          .filter(Boolean).join(' · ')
      : 'none (first run)'));
    list.appendChild(metaLine('Significance', 'CI overlap' +
      (typeof data.comparison.significanceThreshold === 'number'
        ? ' (±' + formatNumber(data.comparison.significanceThreshold * 100, 1) + '% fallback)'
        : '')));
    list.appendChild(metaLine('History', (data.history.runs || []).length + ' runs'));
  }

  /* ---------- pull request index ---------- */

  function renderPrIndex() {
    var container = document.getElementById('pr-list');
    if (!container || activePr) return;
    // The index only exists once a PR run has been published; over file:// the
    // fetch fails and the section simply stays hidden.
    fetch('data/prs/index.json').then(function (response) {
      if (!response.ok) throw new Error('HTTP ' + response.status);
      return response.json();
    }).then(function (index) {
      var prs = (index.prs || []).slice().sort(function (a, b) { return b.number - a.number; });
      if (prs.length === 0) return;

      var panel = el('section', 'panel');
      var header = el('div', 'panel-header');
      header.appendChild(el('h2', null, 'Open Pull Requests'));
      header.appendChild(el('span', null, prs.length + (prs.length === 1 ? ' PR' : ' PRs')));
      panel.appendChild(header);

      var wrap = el('div', 'table-wrap');
      var table = el('table');
      var thead = el('thead');
      var headRow = el('tr');
      ['PR', 'Branch', 'Runs', 'Last run'].forEach(function (label) {
        headRow.appendChild(el('th', null, label));
      });
      thead.appendChild(headRow);
      table.appendChild(thead);
      var tbody = el('tbody');
      prs.forEach(function (pr) {
        var row = el('tr');
        var prCell = el('td');
        prCell.appendChild(relativeLink('?pr=' + pr.number, '#' + pr.number));
        row.appendChild(prCell);
        var branchCell = el('td');
        branchCell.appendChild(el('code', null, pr.refName || ''));
        row.appendChild(branchCell);
        row.appendChild(el('td', 'numeric', String(pr.runs || 0)));
        row.appendChild(el('td', 'numeric', shortDate(pr.updatedAt)));
        tbody.appendChild(row);
      });
      table.appendChild(tbody);
      wrap.appendChild(table);
      panel.appendChild(wrap);
      container.appendChild(panel);
      container.hidden = false;
    }).catch(function () { /* no published PR index */ });
  }

  /* ---------- change panels ---------- */

  function topEntries(entries, change) {
    return entries
      .filter(function (entry) {
        return entry.change === change && typeof entry.deltaRatio === 'number';
      })
      .sort(function (a, b) { return Math.abs(b.deltaRatio) - Math.abs(a.deltaRatio); })
      .slice(0, 8);
  }

  function changeTable(entries) {
    var wrap = el('div', 'table-wrap');
    var table = el('table');
    var thead = el('thead');
    var headRow = el('tr');
    ['Suite', 'Benchmark', 'Current', 'Baseline', 'Delta'].forEach(function (label) {
      headRow.appendChild(el('th', null, label));
    });
    thead.appendChild(headRow);
    table.appendChild(thead);
    var tbody = el('tbody');
    entries.forEach(function (entry) {
      var row = el('tr');
      row.appendChild(el('td', 'suite-label', suiteName(entry.current.suite)));
      var nameCell = el('td');
      nameCell.appendChild(el('code', null, entry.current.displayName));
      row.appendChild(nameCell);
      row.appendChild(el('td', 'numeric', formatValue(entry.current)));
      row.appendChild(el('td', 'numeric', entry.baseline ? formatValue(entry.baseline) : 'new'));
      var deltaCell = el('td', 'numeric');
      deltaCell.appendChild(el('span', deltaClass(entry), formatDelta(entry)));
      row.appendChild(deltaCell);
      tbody.appendChild(row);
    });
    table.appendChild(tbody);
    wrap.appendChild(table);
    return wrap;
  }

  function changePanel(title, entries, emptyText) {
    var panel = el('section', 'panel');
    var header = el('div', 'panel-header');
    header.appendChild(el('h2', null, title));
    panel.appendChild(header);
    if (entries.length === 0) {
      panel.appendChild(el('p', 'empty-state', emptyText));
    } else {
      panel.appendChild(changeTable(entries));
    }
    return panel;
  }

  function renderChanges(data) {
    var container = document.getElementById('changes');
    // Regression/improvement panels only make sense when this snapshot is a
    // comparison against a baseline: pull-request runs and local seeded runs.
    // On the published main dashboard (push/workflow_dispatch) the trend
    // charts carry that information, so the panels are dropped.
    var eventName = data.latest.eventName;
    var isComparisonContext = !eventName || eventName === 'pull_request';
    if (!isComparisonContext || !data.comparison.baseline) {
      container.hidden = true;
      return;
    }
    var entries = data.comparison.entries || [];
    container.appendChild(changePanel(
      'Largest Regressions',
      topEntries(entries, 'regression'),
      'No significant regressions relative to the baseline.'
    ));
    container.appendChild(changePanel(
      'Largest Improvements',
      topEntries(entries, 'improvement'),
      'No significant improvements relative to the baseline.'
    ));
  }

  /* ---------- kodepoint vs java.lang.Character ---------- */

  function comparisonPairs(measurements) {
    var pairs = new Map();
    measurements.forEach(function (m) {
      if (m.group !== 'JvmComparisonBenchmark') return;
      var method = m.displayName.slice(m.displayName.lastIndexOf('.') + 1);
      var base = null;
      var slot = null;
      if (method.slice(-3) === 'Jvm') {
        base = method.slice(0, -3);
        slot = 'jvm';
      } else if (method.slice(-10) === 'Codepoints') {
        base = method.slice(0, -10);
        slot = 'kodepoint';
      } else {
        return;
      }
      if (!pairs.has(base)) pairs.set(base, {});
      pairs.get(base)[slot] = m;
    });
    var rows = [];
    pairs.forEach(function (pair, base) {
      if (pair.jvm && pair.kodepoint) rows.push({ base: base, jvm: pair.jvm, kodepoint: pair.kodepoint });
    });
    rows.sort(function (a, b) { return a.base < b.base ? -1 : a.base > b.base ? 1 : 0; });
    return rows;
  }

  function ratioClass(ratio) {
    if (ratio >= 1.03) return 'delta-improvement';
    if (ratio <= 0.97) return 'delta-regression';
    return 'delta-neutral';
  }

  function comparisonHeaderCell(label, swatchClass) {
    var th = el('th');
    if (swatchClass) th.appendChild(el('span', 'key-swatch ' + swatchClass));
    th.appendChild(document.createTextNode(label));
    return th;
  }

  function renderJvmComparison(data) {
    var container = document.getElementById('jvm-comparison');
    var rows = comparisonPairs(data.latest.measurements || []);
    if (rows.length === 0) return;

    var nav = document.getElementById('suite-nav');
    var navLink = document.createElement('a');
    navLink.href = '#jvm-comparison';
    navLink.textContent = 'vs java.lang.Character';
    nav.appendChild(navLink);

    var section = el('section', 'suite-section comparison-section');
    var header = el('div', 'suite-header');
    var headerText = el('div');
    headerText.appendChild(el('p', 'eyebrow', 'JVM'));
    headerText.appendChild(el('h2', null, 'kodepoint vs java.lang.Character'));
    header.appendChild(headerText);
    var counter = el('span', null, rows.length + ' pairs');
    header.appendChild(counter);
    section.appendChild(header);
    section.dataset.total = rows.length + ' pairs';

    var tableWrap = el('div', 'table-wrap');
    var table = el('table', 'suite-table');
    var thead = el('thead');
    var headRow = el('tr');
    headRow.appendChild(comparisonHeaderCell('Benchmark', null));
    headRow.appendChild(comparisonHeaderCell('kodepoint', 'key-kodepoint'));
    headRow.appendChild(comparisonHeaderCell('java.lang.Character', 'key-jvm'));
    headRow.appendChild(comparisonHeaderCell('Ratio', null));
    headRow.appendChild(comparisonHeaderCell('Throughput', null));
    thead.appendChild(headRow);
    table.appendChild(thead);

    var tbody = el('tbody');
    rows.forEach(function (pair) {
      var row = el('tr', 'comparison-row');
      row.dataset.filterText = (pair.base + ' jvmcomparisonbenchmark').toLowerCase();

      var nameCell = el('td');
      nameCell.appendChild(el('code', null, pair.base));
      row.appendChild(nameCell);
      row.appendChild(el('td', 'numeric', formatValue(pair.kodepoint)));
      row.appendChild(el('td', 'numeric', formatValue(pair.jvm)));

      var ratio = pair.jvm.value !== 0 ? pair.kodepoint.value / pair.jvm.value : NaN;
      var ratioCell = el('td', 'numeric');
      ratioCell.appendChild(el('span', ratioClass(ratio),
        isFinite(ratio) ? formatNumber(ratio, 2) + '×' : 'n/a'));
      row.appendChild(ratioCell);

      var barsCell = el('td');
      var bars = el('div', 'compare-bars');
      var maxValue = Math.max(pair.kodepoint.value, pair.jvm.value) || 1;
      var kodepointBar = el('span', 'bar bar-kodepoint');
      kodepointBar.style.width = (pair.kodepoint.value / maxValue * 100).toFixed(1) + '%';
      kodepointBar.title = 'kodepoint: ' + formatValue(pair.kodepoint);
      var jvmBar = el('span', 'bar bar-jvm');
      jvmBar.style.width = (pair.jvm.value / maxValue * 100).toFixed(1) + '%';
      jvmBar.title = 'java.lang.Character: ' + formatValue(pair.jvm);
      bars.appendChild(kodepointBar);
      bars.appendChild(jvmBar);
      barsCell.appendChild(bars);
      row.appendChild(barsCell);

      tbody.appendChild(row);
    });
    table.appendChild(tbody);
    tableWrap.appendChild(table);
    section.appendChild(tableWrap);
    container.appendChild(section);
  }

  /* ---------- sparklines ---------- */

  function sparkline(points) {
    if (points.length < 2) {
      return el('span', 'sparkline-empty', points.length === 1 ? '1 run' : 'n/a');
    }
    var width = 152;
    var height = 36;
    var padding = 4;
    var values = points.map(function (p) { return p.value; });
    var min = Math.min.apply(null, values);
    var max = Math.max.apply(null, values);
    var range = max - min > 0 ? max - min : 1;
    var step = (width - padding * 2) / (points.length - 1);
    var coords = values.map(function (value, index) {
      var x = padding + step * index;
      var y = padding + (height - padding * 2) * (1 - (value - min) / range);
      return [x, y];
    });
    var svg = svgEl('svg', {
      'class': 'sparkline',
      viewBox: '0 0 ' + width + ' ' + height,
      role: 'img',
      'aria-label': 'Trend over ' + points.length + ' runs'
    });
    svg.appendChild(svgEl('polyline', {
      points: coords.map(function (c) { return c[0].toFixed(2) + ',' + c[1].toFixed(2); }).join(' ')
    }));
    var last = coords[coords.length - 1];
    svg.appendChild(svgEl('circle', {
      'class': 'spark-end',
      cx: last[0].toFixed(2),
      cy: last[1].toFixed(2),
      r: 3.2
    }));
    return svg;
  }

  /* ---------- history detail chart ---------- */

  function niceTicks(min, max, count) {
    if (min === max) {
      var pad = Math.abs(min) > 0 ? Math.abs(min) * 0.05 : 1;
      min -= pad;
      max += pad;
    }
    var span = max - min;
    var rawStep = span / count;
    var magnitude = Math.pow(10, Math.floor(Math.log10(rawStep)));
    var step = magnitude;
    [1, 2, 2.5, 5, 10].some(function (factor) {
      if (magnitude * factor >= rawStep) {
        step = magnitude * factor;
        return true;
      }
      return false;
    });
    var start = Math.floor(min / step) * step;
    var ticks = [];
    for (var tick = start; ; tick += step) {
      ticks.push(tick);
      if (tick >= max) break;
    }
    return ticks;
  }

  function formatTick(value, unit) {
    if (unit === 'bytes' || Math.abs(value) >= 1000) {
      if (Math.abs(value) >= 1e6) return formatNumber(value / 1e6, 1) + 'M';
      if (Math.abs(value) >= 1e3) return formatNumber(value / 1e3, 1) + 'K';
    }
    var digits = Math.abs(value) >= 100 ? 0 : Math.abs(value) >= 1 ? 1 : 3;
    return formatNumber(value, digits);
  }

  function detailChart(measurement, points) {
    var card = el('div', 'chart-card');
    var header = el('div', 'chart-card-header');
    var title = el('h3', null, measurement.displayName);
    header.appendChild(title);
    header.appendChild(el('span', null, points.length + ' runs · ' + measurement.unit));
    card.appendChild(header);

    if (points.length < 2) {
      card.appendChild(el('p', 'empty-state', 'Not enough history for a chart yet — this measurement appears in ' + points.length + ' published run(s).'));
      return card;
    }

    var W = 680;
    var H = 240;
    var margin = { top: 14, right: 18, bottom: 30, left: 64 };
    var innerW = W - margin.left - margin.right;
    var innerH = H - margin.top - margin.bottom;

    var values = [];
    points.forEach(function (p) {
      values.push(p.value);
      if (typeof p.lower === 'number') values.push(p.lower);
      if (typeof p.upper === 'number') values.push(p.upper);
    });
    var min = Math.min.apply(null, values);
    var max = Math.max.apply(null, values);
    var ticks = niceTicks(min, max, 4);
    var yMin = ticks[0];
    var yMax = ticks[ticks.length - 1];
    var xAt = function (index) {
      return margin.left + (points.length === 1 ? innerW / 2 : innerW * index / (points.length - 1));
    };
    var yAt = function (value) {
      return margin.top + innerH * (1 - (value - yMin) / (yMax - yMin || 1));
    };

    var wrap = el('div', 'chart-wrap');
    var svg = svgEl('svg', {
      viewBox: '0 0 ' + W + ' ' + H,
      role: 'img',
      tabindex: '0',
      'aria-label': measurement.displayName + ', ' + points.length + ' runs, latest ' + formatValue(measurement)
    });

    var grid = svgEl('g', { 'class': 'chart-grid' });
    var axis = svgEl('g', { 'class': 'chart-axis' });
    ticks.forEach(function (tick) {
      var y = yAt(tick);
      grid.appendChild(svgEl('line', { x1: margin.left, x2: W - margin.right, y1: y, y2: y }));
      var label = svgEl('text', { x: margin.left - 8, y: y + 4, 'text-anchor': 'end' });
      label.textContent = formatTick(tick, measurement.unit);
      axis.appendChild(label);
    });
    var firstDate = svgEl('text', { x: margin.left, y: H - 8, 'text-anchor': 'start' });
    firstDate.textContent = shortDate(points[0].run.generatedAt);
    axis.appendChild(firstDate);
    var lastDate = svgEl('text', { x: W - margin.right, y: H - 8, 'text-anchor': 'end' });
    lastDate.textContent = shortDate(points[points.length - 1].run.generatedAt);
    axis.appendChild(lastDate);
    svg.appendChild(grid);
    svg.appendChild(axis);

    var hasBand = points.some(function (p) {
      return typeof p.lower === 'number' && typeof p.upper === 'number';
    });
    if (hasBand) {
      var upperPath = points.map(function (p, index) {
        var value = typeof p.upper === 'number' ? p.upper : p.value;
        return xAt(index).toFixed(2) + ',' + yAt(value).toFixed(2);
      });
      var lowerPath = points.map(function (p, index) {
        var value = typeof p.lower === 'number' ? p.lower : p.value;
        return xAt(index).toFixed(2) + ',' + yAt(value).toFixed(2);
      }).reverse();
      svg.appendChild(svgEl('polygon', {
        'class': 'chart-band',
        points: upperPath.concat(lowerPath).join(' ')
      }));
    } else {
      var areaPoints = points.map(function (p, index) {
        return xAt(index).toFixed(2) + ',' + yAt(p.value).toFixed(2);
      });
      areaPoints.push(xAt(points.length - 1).toFixed(2) + ',' + (margin.top + innerH).toFixed(2));
      areaPoints.push(xAt(0).toFixed(2) + ',' + (margin.top + innerH).toFixed(2));
      svg.appendChild(svgEl('polygon', { 'class': 'chart-area', points: areaPoints.join(' ') }));
    }

    svg.appendChild(svgEl('polyline', {
      'class': 'chart-line',
      points: points.map(function (p, index) {
        return xAt(index).toFixed(2) + ',' + yAt(p.value).toFixed(2);
      }).join(' ')
    }));

    var lastIndex = points.length - 1;
    svg.appendChild(svgEl('circle', {
      'class': 'chart-dot',
      cx: xAt(lastIndex).toFixed(2),
      cy: yAt(points[lastIndex].value).toFixed(2),
      r: 4
    }));

    var crosshair = svgEl('line', {
      'class': 'chart-crosshair',
      y1: margin.top,
      y2: margin.top + innerH,
      visibility: 'hidden'
    });
    var hoverDot = svgEl('circle', { 'class': 'chart-dot', r: 4, visibility: 'hidden' });
    svg.appendChild(crosshair);
    svg.appendChild(hoverDot);

    var tooltip = el('div', 'chart-tooltip');
    tooltip.hidden = true;
    var tooltipValue = el('strong', 'tooltip-value');
    var tooltipMeta = el('span', 'tooltip-meta');
    tooltip.appendChild(tooltipValue);
    tooltip.appendChild(tooltipMeta);

    function showIndex(index) {
      var p = points[index];
      var x = xAt(index);
      var y = yAt(p.value);
      crosshair.setAttribute('x1', x);
      crosshair.setAttribute('x2', x);
      crosshair.setAttribute('visibility', 'visible');
      hoverDot.setAttribute('cx', x);
      hoverDot.setAttribute('cy', y);
      hoverDot.setAttribute('visibility', 'visible');
      tooltipValue.textContent = formatNumber(p.value, valueFractionDigits({ unit: measurement.unit, value: p.value })) + ' ' + measurement.unit;
      var metaParts = [shortDate(p.run.generatedAt)];
      if (p.run.commitSha) metaParts.push(p.run.commitSha.slice(0, 7));
      if (p.run.refName) metaParts.push(p.run.refName);
      tooltipMeta.textContent = metaParts.join(' · ');
      var xPercent = x / W * 100;
      tooltip.style.left = xPercent + '%';
      tooltip.style.top = (y / H * 100) + '%';
      tooltip.style.transform = xPercent < 20 ? 'translate(8px, -120%)'
        : xPercent > 80 ? 'translate(calc(-100% - 8px), -120%)'
        : 'translate(-50%, -130%)';
      tooltip.hidden = false;
      return index;
    }

    function hideHover() {
      crosshair.setAttribute('visibility', 'hidden');
      hoverDot.setAttribute('visibility', 'hidden');
      tooltip.hidden = true;
    }

    var activeIndex = lastIndex;
    svg.addEventListener('pointermove', function (event) {
      var rect = svg.getBoundingClientRect();
      var viewX = (event.clientX - rect.left) / rect.width * W;
      var ratio = (viewX - margin.left) / innerW;
      var index = Math.round(ratio * (points.length - 1));
      activeIndex = showIndex(Math.max(0, Math.min(points.length - 1, index)));
    });
    svg.addEventListener('pointerleave', function () {
      if (document.activeElement !== svg) hideHover();
    });
    svg.addEventListener('focus', function () {
      activeIndex = showIndex(activeIndex);
    });
    svg.addEventListener('blur', hideHover);
    svg.addEventListener('keydown', function (event) {
      if (event.key === 'ArrowLeft') activeIndex = showIndex(Math.max(0, activeIndex - 1));
      else if (event.key === 'ArrowRight') activeIndex = showIndex(Math.min(points.length - 1, activeIndex + 1));
      else if (event.key === 'Home') activeIndex = showIndex(0);
      else if (event.key === 'End') activeIndex = showIndex(points.length - 1);
      else if (event.key === 'Escape') hideHover();
      else return;
      event.preventDefault();
    });

    wrap.appendChild(svg);
    wrap.appendChild(tooltip);
    card.appendChild(wrap);
    card.appendChild(el('p', 'chart-hint', 'Hover or focus the chart and use ←/→ to inspect runs. The shaded band is the reported confidence interval.'));
    return card;
  }

  /* ---------- suite tables ---------- */

  function benchmarkRow(measurement, comparisonEntry, seriesPoints, columns) {
    var row = el('tr', 'benchmark-row');
    row.dataset.filterText = (measurement.displayName + ' ' + measurement.group).toLowerCase();

    var nameCell = el('td');
    var toggle = el('button', 'row-toggle');
    toggle.type = 'button';
    toggle.setAttribute('aria-expanded', 'false');
    toggle.appendChild(el('code', null, measurement.displayName));
    nameCell.appendChild(toggle);
    nameCell.appendChild(el('span', 'group-label', measurement.group));
    row.appendChild(nameCell);

    row.appendChild(el('td', 'numeric', formatValue(measurement)));
    row.appendChild(el('td', 'numeric',
      comparisonEntry && comparisonEntry.baseline ? formatValue(comparisonEntry.baseline) : 'new'));
    var deltaCell = el('td', 'numeric');
    deltaCell.appendChild(el('span', deltaClass(comparisonEntry), formatDelta(comparisonEntry)));
    row.appendChild(deltaCell);
    var trendCell = el('td');
    trendCell.appendChild(sparkline(seriesPoints));
    row.appendChild(trendCell);

    var detailRow = el('tr', 'detail-row');
    detailRow.hidden = true;
    var detailCell = el('td');
    detailCell.colSpan = columns;
    detailRow.appendChild(detailCell);

    function setExpanded(expanded) {
      toggle.setAttribute('aria-expanded', String(expanded));
      detailRow.hidden = !expanded;
      if (expanded && !detailCell.firstChild) {
        detailCell.appendChild(detailChart(measurement, seriesPoints));
      }
    }

    row.addEventListener('click', function (event) {
      if (event.target.closest('a') || event.target.closest('.detail-row')) return;
      setExpanded(detailRow.hidden);
    });
    toggle.addEventListener('click', function (event) {
      event.stopPropagation();
      setExpanded(detailRow.hidden);
    });

    return { row: row, detailRow: detailRow };
  }

  function renderSuites(data, seriesByKey) {
    var container = document.getElementById('suites');
    var nav = document.getElementById('suite-nav');
    var comparisonByKey = new Map();
    (data.comparison.entries || []).forEach(function (entry) {
      comparisonByKey.set(measurementKey(entry.current), entry);
    });

    var measurements = (data.latest.measurements || []).slice();
    var suites = measurements
      .map(function (m) { return m.suite; })
      .filter(function (suite, index, all) { return all.indexOf(suite) === index; })
      .sort(function (a, b) { return suiteIndex(a) - suiteIndex(b); });

    suites.forEach(function (suite) {
      var navLink = document.createElement('a');
      navLink.href = '#suite-' + suite;
      navLink.textContent = suiteName(suite);
      nav.appendChild(navLink);

      var suiteMeasurements = measurements.filter(function (m) { return m.suite === suite; });
      var section = el('section', 'suite-section');
      section.id = 'suite-' + suite;
      var header = el('div', 'suite-header');
      var headerText = el('div');
      headerText.appendChild(el('p', 'eyebrow', suiteName(suite)));
      headerText.appendChild(el('h2', null, suiteName(suite) + ' measurements'));
      header.appendChild(headerText);
      var counter = el('span', null, suiteMeasurements.length + ' measurements');
      header.appendChild(counter);
      section.appendChild(header);
      section.dataset.total = suiteMeasurements.length + ' measurements';

      var tableWrap = el('div', 'table-wrap');
      var table = el('table', 'suite-table');
      var thead = el('thead');
      var headRow = el('tr');
      ['Benchmark', 'Current', 'Baseline', 'Delta', 'Trend'].forEach(function (label) {
        headRow.appendChild(el('th', null, label));
      });
      thead.appendChild(headRow);
      table.appendChild(thead);
      var tbody = el('tbody');
      suiteMeasurements.forEach(function (measurement) {
        var key = measurementKey(measurement);
        var rows = benchmarkRow(
          measurement,
          comparisonByKey.get(key),
          seriesByKey.get(key) || [],
          5
        );
        tbody.appendChild(rows.row);
        tbody.appendChild(rows.detailRow);
      });
      table.appendChild(tbody);
      tableWrap.appendChild(table);
      section.appendChild(tableWrap);
      container.appendChild(section);
    });
  }

  function attachFilter() {
    var input = document.getElementById('filter-input');
    input.addEventListener('input', function () {
      var query = input.value.trim().toLowerCase();
      document.querySelectorAll('.suite-section').forEach(function (section) {
        var visible = 0;
        section.querySelectorAll('tr.benchmark-row, tr.comparison-row').forEach(function (row) {
          var matches = query === '' || row.dataset.filterText.indexOf(query) !== -1;
          row.hidden = !matches;
          var detail = row.nextElementSibling;
          if (detail && detail.classList.contains('detail-row')) {
            detail.hidden = !matches || row.querySelector('.row-toggle').getAttribute('aria-expanded') !== 'true';
          }
          if (matches) visible += 1;
        });
        var counter = section.querySelector('.suite-header span');
        counter.textContent = query === ''
          ? section.dataset.total
          : visible + ' of ' + section.dataset.total;
        section.hidden = visible === 0;
      });
    });
  }

  renderPrIndex();

  loadData().then(function (data) {
    renderMeta(data);
    renderChanges(data);
    renderJvmComparison(data);
    renderSuites(data, buildHistorySeries(data.history));
    attachFilter();
  }).catch(function (error) {
    console.error(error);
    document.getElementById('load-error').hidden = false;
    document.getElementById('toolbar').hidden = true;
  }).finally(function () {
    document.body.removeAttribute('data-loading');
  });
})();
