# Version Management & Release System

This document describes the automated version management and release system for Android Nomad Gateway.

## 📋 Overview

The project uses semantic versioning (SemVer) with automated scripts for version bumping, building, and releasing. The system includes:

- **Automated version bumping** with git commits and tags
- **Changelog generation** following Keep a Changelog format
- **Release APK building** with proper signing
- **GitHub release creation** with automated release notes
- **Rollback capabilities** in case of build failures

## 🛠️ Scripts

### 1. `version_bump.sh` - Version Bump Script

**Purpose:** Increment version numbers and create git commits/tags.

**Usage:**
```bash
./version_bump.sh [major|minor|patch] [commit_message]
```

**Examples:**
```bash
# Patch version (1.0.0 -> 1.0.1)
./version_bump.sh patch "Fix SMS forwarding bug"

# Minor version (1.0.0 -> 1.1.0)
./version_bump.sh minor "Add push notification support"

# Major version (1.0.0 -> 2.0.0)
./version_bump.sh major "Complete UI redesign"
```

**What it does:**
1. ✅ Validates git repository state (no uncommitted changes)
2. ✅ Parses current version from `app/build.gradle`
3. ✅ Increments version according to SemVer rules
4. ✅ Updates `versionCode` and `versionName` in build.gradle
5. ✅ Creates or updates `CHANGELOG.md`
6. ✅ Creates git commit with conventional commit format
7. ✅ Creates annotated git tag
8. ✅ Builds project to verify changes
9. ✅ Rolls back on build failure

### 2. `release.sh` - Complete Release Workflow

**Purpose:** Perform a complete release including APK building and GitHub release.

**Usage:**
```bash
./release.sh [major|minor|patch] [release_message]
```

**Examples:**
```bash
# Patch release
./release.sh patch "Critical bug fixes and stability improvements"

# Minor release
./release.sh minor "New Material Design 3 UI and operator settings"

# Major release
./release.sh major "Complete app redesign with modern architecture"
```

**What it does:**
1. ✅ Updates version numbers in build.gradle and CHANGELOG.md
2. ✅ Builds release APK with debug signing
3. ✅ Runs test suite
4. ✅ Generates comprehensive release notes
5. ✅ Updates releases README.md with new release info
6. ✅ Creates unified git commit with all release artifacts
7. ✅ Creates git tag for the release
8. ✅ Publishes GitHub release with APK attachment (if gh CLI available)
9. ✅ Provides next steps guidance

## 📁 File Structure

```
android-nomad-gateway/
├── version_bump.sh          # Version bump script
├── release.sh               # Complete release workflow
├── VERSION_MANAGEMENT.md    # This documentation
├── CHANGELOG.md             # Auto-generated changelog
├── releases/                # Release artifacts directory
│   ├── android-nomad-gateway-v1.0.1.apk
│   ├── release-notes-v1.0.1.md
│   └── ...
└── app/
    └── build.gradle         # Contains versionCode and versionName
```

## 🔄 Version Numbering

### Semantic Versioning (SemVer)

The project follows [Semantic Versioning 2.0.0](https://semver.org/):

- **MAJOR** version: Incompatible API changes or major feature overhauls
- **MINOR** version: New functionality in a backwards compatible manner
- **PATCH** version: Backwards compatible bug fixes

### Android Version Codes

- **versionCode**: Auto-incremented integer for Google Play Store
- **versionName**: Human-readable version string (e.g., "1.2.3")

### Examples

| Change Type | Current | New | versionCode | Description |
|-------------|---------|-----|-------------|-------------|
| Patch | 1.0.0 | 1.0.1 | +1 | Bug fixes, small improvements |
| Minor | 1.0.1 | 1.1.0 | +1 | New features, UI updates |
| Major | 1.1.0 | 2.0.0 | +1 | Breaking changes, redesign |

## 📝 Changelog Format

The system automatically maintains `CHANGELOG.md` following [Keep a Changelog](https://keepachangelog.com/) format:

```markdown
# Changelog

## [Unreleased]

## [1.1.0] - 2024-01-15

### Added
- Material Design 3 UI implementation
- Operator settings for SIM management
- Enhanced permission management

### Fixed
- SMS forwarding reliability issues
- Memory leak in MainActivity

### Changed
- Updated to Android API 35
- Modernized SSL handling
```

## 🚀 Release Workflow

### Quick Release (Recommended)

For most releases, use the complete workflow:

```bash
# 1. Make your changes and commit them
git add .
git commit -m "feat: implement new feature"

# 2. Run release script
./release.sh minor "Add awesome new feature"

# 3. Push changes and tags
git push origin main --tags
```

### Manual Version Bump Only

If you only want to bump version without full release:

```bash
# Bump version and commit
./version_bump.sh patch "Fix critical bug"

# Push changes
git push origin main --tags
```

### Build Release APK Only

```bash
# Build release APK manually
./gradlew clean assembleRelease

# APK will be in: app/build/outputs/apk/release/app-release.apk
```

## 🔧 Configuration

### Customizing Scripts

Both scripts can be customized by editing the configuration variables at the top:

```bash
# In version_bump.sh and release.sh
BUILD_GRADLE_FILE="app/build.gradle"
CHANGELOG_FILE="CHANGELOG.md"
```

### Git Commit Format

The scripts use [Conventional Commits](https://www.conventionalcommits.org/) format:

```
chore: bump version to 1.1.0

Add Material Design 3 UI and operator settings

- Updated versionCode and versionName in build.gradle
- Updated CHANGELOG.md with release notes
```

### Release Notes Template

Release notes are automatically generated with:

- 🚀 What's New section
- 📱 Installation instructions
- 🔧 Technical details
- 📋 Feature list
- 🛠️ Required permissions
- 🐛 Bug report information

## 🛡️ Safety Features

### Pre-flight Checks

- ✅ Validates git repository state
- ✅ Checks for uncommitted changes
- ✅ Verifies build.gradle format
- ✅ Confirms user intent before proceeding

### Build Verification

- ✅ Builds project after version changes
- ✅ Runs tests before release
- ✅ Validates APK generation

### Rollback Capabilities

- ✅ Automatic rollback on build failure
- ✅ Git tag cleanup on errors
- ✅ Backup creation before modifications

## 🔍 Troubleshooting

### Common Issues

**"Not in a git repository"**
```bash
# Initialize git if needed
git init
git remote add origin <your-repo-url>
```

**"Uncommitted changes detected"**
```bash
# Commit or stash changes first
git add .
git commit -m "your changes"
# OR
git stash
```

**"Build failed during version bump"**
- The script automatically rolls back changes
- Check build errors with: `./gradlew assembleDebug`
- Fix issues and try again

**"GitHub CLI not found"**
```bash
# Install GitHub CLI
brew install gh
# OR skip GitHub release creation
```

### Manual Recovery

If something goes wrong, you can manually recover:

```bash
# Reset to previous commit
git reset --hard HEAD~1

# Remove created tag
git tag -d v1.1.0

# Restore backup
cp app/build.gradle.backup app/build.gradle
```

## 📚 Best Practices

### Before Releasing

1. ✅ Test the app thoroughly
2. ✅ Update documentation if needed
3. ✅ Review and commit all changes
4. ✅ Choose appropriate version bump type
5. ✅ Write descriptive release message

### Version Bump Guidelines

- **Patch**: Bug fixes, security updates, minor improvements
- **Minor**: New features, UI updates, dependency updates
- **Major**: Breaking changes, complete redesigns, architecture changes

### Release Message Guidelines

Write clear, user-focused release messages:

```bash
# Good examples
./release.sh minor "Add Material Design 3 UI with improved accessibility"
./release.sh patch "Fix SMS forwarding reliability and memory leaks"
./release.sh major "Complete app redesign with modern architecture"

# Avoid technical jargon
./release.sh minor "Refactor MainActivity and update dependencies"
```

## 🔗 Integration

### CI/CD Integration

The scripts can be integrated into CI/CD pipelines:

```yaml
# GitHub Actions example
- name: Release
  run: |
    ./release.sh minor "${{ github.event.head_commit.message }}"
    git push origin main --tags
```

### IDE Integration

Add as external tools in Android Studio:

- **Program**: `./version_bump.sh`
- **Arguments**: `patch "Quick fix"`
- **Working directory**: `$ProjectFileDir$`

## 📞 Support

For issues with the version management system:

1. Check this documentation
2. Review script output for error messages
3. Check git status and build logs
4. Create an issue in the repository

---

**Happy releasing! 🚀** 