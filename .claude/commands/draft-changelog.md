---
description: Draft Unreleased changelog entries from the commits since the last release
allowed-tools: Bash(git log:*), Bash(git show:*), Bash(git diff:*), Bash(git describe:*), Bash(git tag:*), Bash(gh pr view:*), Bash(gh pr list:*), Read, Edit
---

Draft changelog entries into the `## [Unreleased]` section of `CHANGELOG.md`.

This produces a **draft for a human to edit**, not final release notes. Prefer leaving a `TODO`
over guessing: an entry you cannot ground in the diff is worse than no entry.

## Gather

1. Find the last release tag: `git describe --tags --abbrev=0`. Releases are tagged with the bare
   version (`2.0.0`).
2. `git log <tag>..HEAD --oneline` for the commits since then. Also check `gradle.properties`
   (`kodepoint.unicodeVersion`) and `buildSrc/` – a Unicode data update or a generator change ships
   to users even though it may not look like a source change.
3. **Read the actual diffs**, not just the subjects. `git show <sha>` for anything that looks
   behavioural. The subject line is what makes generated changelogs useless; the diff is where the
   semantics are. Where a PR number is available, `gh pr view <n>` often explains the *why* that no
   commit records.

## Write

The `## [Unreleased]` section starts out with no group headings. Add only the `###` groups you have
entries for, drawn from **Breaking**, **Added**, **Changed**, **Fixed**, **Performance**, and keep
them in that order. Do not add a group you are going to leave empty.

Each entry states what a *consumer of the library* observes:

- **Fixed** – the symptom and the condition that triggers it, not the mechanism.
  Good: ``Fixed `codePointBefore()` throwing for an index equal to the string length.``
  Bad: `Fix off-by-one in CharSequenceExtensions`.
- **Breaking** – what breaks, plus the migration. Name the old and new symbol or coordinate. New
  `UnicodeScript` or `Category` constants are breaking for exhaustive `when` expressions.
- **Performance** – quantify from the benchmarks in `benchmarks/` or the dashboard at
  https://zolotov.github.io/kodepoint if a number exists; say which benchmark. An unquantified
  performance claim is noise.
- **Added** – the public API surface added, named. Public means the public declarations of
  `lib/src/commonMain` and the enums in `common`; generated tables and `buildSrc` are not.
- **Changed** – a Unicode version update belongs here (or under Breaking if it adds enum
  constants): say which version, how many characters, which scripts, and that JVM results still
  follow the JDK's Unicode version.
- Platform, toolchain and minimum-version changes are user-visible even when they touch no source:
  a new target, a Kotlin API-version bump, a JVM bytecode target change all belong here.

Rules:

- Reference the PR as `([#66](https://github.com/zolotov/kodepoint/pull/66))` where one exists. Do
  not fabricate numbers.
- Collapse dependency bumps into **one** entry under Changed, naming only what a consumer could
  notice. Build-time-only bumps (Gradle, KotlinPoet, benchmark harness) are omitted entirely.
- Omit CI, build-script, benchmark-harness, formatting and internal-refactor commits entirely.
  If a release contains only such commits, say so and add nothing.
- Do not touch released sections, the file title, or the link definitions at the bottom – the
  Gradle plugin owns those.

## Report

After editing, show the resulting `## [Unreleased]` section and list, separately:

- commits you deliberately omitted, with the reason (one line each), so the omissions can be
  challenged;
- entries you could not ground in a diff, marked `TODO`, with the question that needs answering.
