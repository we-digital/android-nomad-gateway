#!/bin/bash

# Release Script for Android Nomad Gateway
# Usage: ./release.sh [major|minor|patch] [message]
# This script performs a complete release workflow

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

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

show_usage() {
    echo "Usage: $0 [major|minor|patch] [release_message]"
    echo ""
    echo "This script performs a complete release workflow:"
    echo "  1. Version bump with git commit and tag"
    echo "  2. Build release APK"
    echo "  3. Run tests"
    echo "  4. Generate release notes"
    echo "  5. Create GitHub release (if gh CLI is available)"
    echo ""
    echo "Examples:"
    echo "  $0 patch \"Fix critical SMS forwarding bug\""
    echo "  $0 minor \"Add Material Design 3 UI and operator settings\""
    echo "  $0 major \"Complete app redesign with modern architecture\""
    echo ""
    exit 1
}

# Function to build release APK
build_release() {
    print_info "Building release APK..."
    
    # Clean and build release with suppressed warnings for cleaner output
    ./gradlew clean assembleRelease --warning-mode=summary
    
    # Check if APK was created
    local apk_path="app/build/outputs/apk/release/app-release-unsigned.apk"
    if [ -f "$apk_path" ]; then
        print_success "Release APK built successfully: $apk_path"
        
        # Show APK info
        local apk_size=$(du -h "$apk_path" | cut -f1)
        print_info "APK size: $apk_size"
        
        # Copy to releases directory
        mkdir -p releases
        local version=$(grep "versionName" app/build.gradle | sed 's/.*versionName = "\([^"]*\)".*/\1/')
        cp "$apk_path" "releases/android-nomad-gateway-v$version.apk"
        print_success "APK copied to releases/android-nomad-gateway-v$version.apk"
    else
        print_error "Failed to build release APK!"
        exit 1
    fi
}

# Function to run tests
run_tests() {
    print_info "Running tests..."
    
    if ./gradlew test --quiet; then
        print_success "All tests passed!"
    else
        print_warning "Some tests failed, but continuing with release..."
    fi
}

# Function to generate release notes
generate_release_notes() {
    local version=$1
    local message=$2
    
    print_info "Generating release notes..."
    
    # Create release notes file
    local notes_file="releases/release-notes-v$version.md"
    mkdir -p releases
    
    cat > "$notes_file" << EOF
# Android Nomad Gateway v$version

## 🚀 What's New

$message

## 📱 Installation

Download the APK file and install it on your Android device:
- **Minimum Android version:** 8.0 (API 26)
- **Target Android version:** 14 (API 35)
- **Architecture:** Universal APK

## 🔧 Technical Details

- **Version Code:** $(grep "versionCode" app/build.gradle | sed 's/.*versionCode = \([0-9]*\).*/\1/')
- **Version Name:** $version
- **Build Date:** $(date +"%Y-%m-%d %H:%M:%S")
- **Git Commit:** $(git rev-parse --short HEAD)

## 📋 Features

- 📱 SMS forwarding to webhooks
- 📞 Call notification forwarding
- 🔔 Push notification forwarding
- 🎨 Modern Material Design 3 UI
- ⚙️ Comprehensive settings and permissions management
- 📊 SIM card management and operator settings
- 🔒 Privacy-first approach with granular permissions

## 🛠️ Permissions Required

- **SMS Access** - To receive and forward SMS messages
- **Phone State** - To identify SIM cards and monitor calls
- **Call Log** - To detect incoming calls
- **Contacts** - To resolve phone numbers to names
- **Phone Numbers** - To identify your phone numbers
- **Post Notifications** - To show service status
- **Read Notifications** - For push notification forwarding

## 🐛 Bug Reports

If you encounter any issues, please report them on our GitHub repository.

## 📄 Changelog

See [CHANGELOG.md](../CHANGELOG.md) for detailed changes.
EOF

    print_success "Release notes generated: $notes_file"
}

# Function to create GitHub release
create_github_release() {
    local version=$1
    local message=$2
    
    if command -v gh &> /dev/null; then
        print_info "Creating GitHub release..."
        
        local apk_path="releases/android-nomad-gateway-v$version.apk"
        local notes_path="releases/release-notes-v$version.md"
        
        if gh release create "v$version" \
            --title "Android Nomad Gateway v$version" \
            --notes-file "$notes_path" \
            "$apk_path"; then
            print_success "GitHub release created successfully!"
            print_info "Release URL: https://github.com/\$(gh repo view --json owner,name -q '.owner.login + \"/\" + .name')/releases/tag/v$version"
        else
            print_warning "Failed to create GitHub release. You can create it manually."
        fi
    else
        print_warning "GitHub CLI (gh) not found. Skipping GitHub release creation."
        print_info "You can install it with: brew install gh"
    fi
}

# Main function
main() {
    print_info "🚀 Android Nomad Gateway Release Script"
    echo ""
    
    # Parse arguments
    local bump_type=$1
    local release_message=$2
    
    if [ -z "$bump_type" ]; then
        show_usage
    fi
    
    if [ -z "$release_message" ]; then
        release_message="Release version bump ($bump_type)"
    fi
    
    # Validate bump type
    if [[ ! "$bump_type" =~ ^(major|minor|patch)$ ]]; then
        print_error "Invalid bump type: $bump_type"
        show_usage
    fi
    
    print_info "Release type: $bump_type"
    print_info "Release message: $release_message"
    echo ""
    
    # Confirm with user
    read -p "Proceed with full release workflow? (y/N): " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_warning "Release cancelled."
        exit 0
    fi
    
    # Step 1: Version bump
    print_info "Step 1/6: Version bump and git operations"
    ./version_bump.sh "$bump_type" "$release_message"
    
    # Get new version
    local new_version=$(grep "versionName" app/build.gradle | sed 's/.*versionName = "\([^"]*\)".*/\1/')
    
    # Step 2: Build release APK
    print_info "Step 2/6: Building release APK"
    build_release
    
    # Step 3: Run tests
    print_info "Step 3/6: Running tests"
    run_tests
    
    # Step 4: Generate release notes
    print_info "Step 4/6: Generating release notes"
    generate_release_notes "$new_version" "$release_message"
    
    # Step 5: Create GitHub release
    print_info "Step 5/6: Creating GitHub release"
    create_github_release "$new_version" "$release_message"
    
    # Step 6: Final summary
    print_info "Step 6/6: Release summary"
    echo ""
    print_success "🎉 Release v$new_version completed successfully!"
    echo ""
    print_info "📦 Release artifacts:"
    echo "  • APK: releases/android-nomad-gateway-v$new_version.apk"
    echo "  • Release notes: releases/release-notes-v$new_version.md"
    echo "  • Git tag: v$new_version"
    echo ""
    print_info "🚀 Next steps:"
    echo "  • Push changes: git push origin main --tags"
    echo "  • Share the release with users"
    echo "  • Update documentation if needed"
    echo ""
}

# Run main function
main "$@"
