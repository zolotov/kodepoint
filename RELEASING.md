## Releases

Releases are tagged and published as the bare version, e.g. `2.0.0`, and cover the `kodepoint`,
`unicode` and `common` artifacts together. The changelog lives in [`CHANGELOG.md`](./CHANGELOG.md).

### Changelog

Changelog entries are written by hand, because a commit subject records what the author did to the
source tree rather than what a consumer of the library gets. Describe the observable change: the
symptom and its trigger for a fix, the migration for a breaking change, a benchmark number for a
performance claim, the Unicode version and what it adds for a data update.

Add entries to the `## [Unreleased]` section of `CHANGELOG.md` as you go, under whichever of
`### Breaking`, `### Added`, `### Changed`, `### Fixed` and `### Performance` apply. For a starting
point, `/draft-changelog` in Claude Code reads the commits and diffs since the last release and
drafts entries – the draft always needs editing before it ships.

Preview what the next release will publish:

```bash
./gradlew getChangelog --unreleased --no-header
cat build/reports/changelog/latest-release-body.md
```

### Releasing

Run the `Release` workflow (Actions → Release → Run workflow) on `main` with the new version. It
promotes the `Unreleased` section to a version section
([`patchChangelog`](https://github.com/JetBrains/gradle-changelog-plugin)), hands the same text to
the GitHub release, publishes to Maven Central, and commits the changelog together with the stamped
docs. Nothing is generated at release time, and an empty `Unreleased` section fails the release
before anything is published.

The same workflow runs `updateReadmeVersion`, which rewrites the dependency coordinates in
`README.md` and `llms.txt` to the version being released, so the install snippets always name a
version that exists.
