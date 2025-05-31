#!/bin/bash

# Version Bump Script for Android Nomad Gateway
# Usage: ./version_bump.sh [major|minor|patch] [message]
# Example: ./version_bump.sh minor "Add new SMS forwarding features"

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BUILD_GRADLE_FILE="app/build.gradle"
CHANGELOG_FILE="CHANGELOG.md"

# Function to print colored output
print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [major|minor|patch] [commit_message]"
    echo ""
    echo "Version bump types:"
    echo "  major  - Increment major version (1.0.0 -> 2.0.0)"
    echo "  minor  - Increment minor version (1.0.0 -> 1.1.0)"
    echo "  patch  - Increment patch version (1.0.0 -> 1.0.1)"
    echo ""
    echo "Examples:"
    echo "  $0 patch \"Fix SMS forwarding bug\""
    echo "  $0 minor \"Add push notification support\""
    echo "  $0 major \"Complete UI redesign\""
    echo ""
    exit 1
}

# Function to get current version from build.gradle
get_current_version() {
    local version_code=$(grep "versionCode" $BUILD_GRADLE_FILE | sed 's/.*versionCode = \([0-9]*\).*/\1/')
    local version_name=$(grep "versionName" $BUILD_GRADLE_FILE | sed 's/.*versionName = "\([^"]*\)".*/\1/')
    
    echo "$version_code|$version_name"
}

# Function to parse semantic version
parse_version() {
    local version=$1
    local major minor patch
    
    if [[ $version =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
        major=${BASH_REMATCH[1]}
        minor=${BASH_REMATCH[2]}
        patch=${BASH_REMATCH[3]}
    elif [[ $version =~ ^([0-9]+)\.([0-9]+)$ ]]; then
        major=${BASH_REMATCH[1]}
        minor=${BASH_REMATCH[2]}
        patch=0
    elif [[ $version =~ ^([0-9]+)$ ]]; then
        major=${BASH_REMATCH[1]}
        minor=0
        patch=0
    else
        print_error "Invalid version format: $version"
        exit 1
    fi
    
    echo "$major|$minor|$patch"
}

# Function to increment version
increment_version() {
    local bump_type=$1
    local current_version=$2
    
    IFS='|' read -r major minor patch <<< "$(parse_version $current_version)"
    
    case $bump_type in
        "major")
            major=$((major + 1))
            minor=0
            patch=0
            ;;
        "minor")
            minor=$((minor + 1))
            patch=0
            ;;
        "patch")
            patch=$((patch + 1))
            ;;
        *)
            print_error "Invalid bump type: $bump_type"
            exit 1
            ;;
    esac
    
    echo "$major.$minor.$patch"
}

# Function to update build.gradle
update_build_gradle() {
    local new_version_code=$1
    local new_version_name=$2
    
    print_info "Updating $BUILD_GRADLE_FILE..."
    
    # Create backup
    cp $BUILD_GRADLE_FILE "${BUILD_GRADLE_FILE}.backup"
    
    # Update versionCode and versionName
    sed -i.tmp "s/versionCode = [0-9]*/versionCode = $new_version_code/" $BUILD_GRADLE_FILE
    sed -i.tmp "s/versionName = \"[^\"]*\"/versionName = \"$new_version_name\"/" $BUILD_GRADLE_FILE
    
    # Remove temporary file
    rm "${BUILD_GRADLE_FILE}.tmp"
    
    print_success "Updated version in $BUILD_GRADLE_FILE"
}

# Function to create or update CHANGELOG.md
update_changelog() {
    local version=$1
    local message=$2
    local date=$(date +"%Y-%m-%d")
    
    print_info "Updating $CHANGELOG_FILE..."
    
    # Create CHANGELOG.md if it doesn't exist
    if [ ! -f $CHANGELOG_FILE ]; then
        cat > $CHANGELOG_FILE << EOF
# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [$version] - $date

### Added
- $message

EOF
    else
        # Insert new version entry after "## [Unreleased]"
        local temp_file=$(mktemp)
        awk -v version="$version" -v date="$date" -v message="$message" '
        /^## \[Unreleased\]/ {
            print $0
            print ""
            print "## [" version "] - " date
            print ""
            print "### Added"
            print "- " message
            print ""
            next
        }
        { print }
        ' $CHANGELOG_FILE > $temp_file
        mv $temp_file $CHANGELOG_FILE
    fi
    
    print_success "Updated $CHANGELOG_FILE"
}

# Function to create git tag and commit
commit_and_tag() {
    local version=$1
    local message=$2
    
    print_info "Committing changes and creating git tag..."
    
    # Add files to git
    git add $BUILD_GRADLE_FILE $CHANGELOG_FILE
    
    # Commit changes
    git commit -m "chore: bump version to $version

$message

- Updated versionCode and versionName in build.gradle
- Updated CHANGELOG.md with release notes"
    
    # Create git tag
    git tag -a "v$version" -m "Release version $version

$message"
    
    print_success "Created commit and tag v$version"
}

# Function to build and verify
build_and_verify() {
    print_info "Building project to verify changes..."
    
    if ./gradlew assembleDebug --quiet --warning-mode=summary; then
        print_success "Build successful! ✨"
    else
        print_error "Build failed! Rolling back changes..."
        git reset --hard HEAD~1
        git tag -d "v$(get_current_version | cut -d'|' -f2)" 2>/dev/null || true
        exit 1
    fi
}

# Main script execution
main() {
    print_info "🚀 Android Nomad Gateway Version Bump Script"
    echo ""
    
    # Check if we're in a git repository
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        print_error "Not in a git repository!"
        exit 1
    fi
    
    # Check for uncommitted changes
    if ! git diff-index --quiet HEAD --; then
        print_warning "You have uncommitted changes. Please commit or stash them first."
        git status --short
        exit 1
    fi
    
    # Parse arguments
    local bump_type=$1
    local commit_message=$2
    
    if [ -z "$bump_type" ]; then
        show_usage
    fi
    
    if [ -z "$commit_message" ]; then
        commit_message="Version bump ($bump_type)"
    fi
    
    # Validate bump type
    if [[ ! "$bump_type" =~ ^(major|minor|patch)$ ]]; then
        print_error "Invalid bump type: $bump_type"
        show_usage
    fi
    
    # Get current version
    local current_info=$(get_current_version)
    local current_version_code=$(echo $current_info | cut -d'|' -f1)
    local current_version_name=$(echo $current_info | cut -d'|' -f2)
    
    print_info "Current version: $current_version_name (code: $current_version_code)"
    
    # Calculate new version
    local new_version_name=$(increment_version $bump_type $current_version_name)
    local new_version_code=$((current_version_code + 1))
    
    print_info "New version: $new_version_name (code: $new_version_code)"
    print_info "Bump type: $bump_type"
    print_info "Commit message: $commit_message"
    echo ""
    
    # Confirm with user
    read -p "Proceed with version bump? (y/N): " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_warning "Version bump cancelled."
        exit 0
    fi
    
    # Perform version bump
    update_build_gradle $new_version_code $new_version_name
    update_changelog $new_version_name "$commit_message"
    commit_and_tag $new_version_name "$commit_message"
    build_and_verify
    
    echo ""
    print_success "🎉 Version bump completed successfully!"
    print_info "New version: $new_version_name (code: $new_version_code)"
    print_info "Git tag: v$new_version_name"
    echo ""
    print_info "Next steps:"
    echo "  • Push changes: git push origin main"
    echo "  • Push tags: git push origin --tags"
    echo "  • Create release: gh release create v$new_version_name"
    echo ""
}

# Run main function with all arguments
main "$@" 