# Material Design 3 Cards Implementation

This document outlines the modern Material Design 3 card system implemented in the Android Nomad Gateway app, following the latest [Material Design 3 guidelines](https://m3.material.io/components/cards).

## Card Types

### 1. Elevated Cards (Primary Choice)
**Usage**: Main content cards, list items, primary information containers
```xml
<com.google.android.material.card.MaterialCardView
    style="@style/Widget.App.CardView.Elevated"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">
```

**Properties**:
- Elevation: 2dp
- Corner Radius: 20dp
- Background: `?attr/colorSurface`
- No stroke/border

### 2. Filled Cards
**Usage**: Secondary content, supporting information, grouped content
```xml
<com.google.android.material.card.MaterialCardView
    style="@style/Widget.App.CardView.Filled"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">
```

**Properties**:
- Elevation: 0dp
- Corner Radius: 16dp
- Background: `?attr/colorSurfaceVariant`
- No stroke/border

### 3. Outlined Cards (Legacy Support)
**Usage**: Only when borders are specifically needed for accessibility or design requirements
```xml
<com.google.android.material.card.MaterialCardView
    style="@style/Widget.App.CardView.Outlined"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">
```

**Properties**:
- Elevation: 0dp
- Corner Radius: 16dp
- Background: `?attr/colorSurface`
- Stroke: 1dp `?attr/colorOutline`

## Design Principles

### Modern Material Design 3 Approach
1. **No Borders by Default**: Modern cards use elevation and surface tinting instead of borders
2. **Generous Corner Radius**: 16dp-24dp for a softer, more contemporary look
3. **Subtle Elevation**: 1dp-3dp for depth without overwhelming shadows
4. **Surface Tinting**: Uses Material You color system for dynamic theming

### Elevation Hierarchy
- **1dp**: Subtle cards, secondary content
- **2dp**: Standard cards, list items
- **3dp**: Important cards, headers
- **6dp**: Floating elements, dialogs

### Corner Radius Scale
- **12dp**: Small components, chips
- **16dp**: Standard cards
- **20dp**: Large cards, primary content
- **24dp**: Hero cards, headers

## Implementation Examples

### List Item Cards
```xml
<!-- Modern list item with elevated card -->
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginHorizontal="8dp"
    android:layout_marginVertical="8dp"
    app:cardBackgroundColor="?attr/colorSurface"
    app:cardCornerRadius="20dp"
    app:cardElevation="2dp"
    app:strokeWidth="0dp">
```

### Header Cards
```xml
<!-- Hero header card with maximum visual impact -->
<com.google.android.material.card.MaterialCardView
    app:cardBackgroundColor="?attr/colorPrimaryContainer"
    app:cardCornerRadius="24dp"
    app:cardElevation="3dp"
    app:strokeWidth="0dp">
```

### Content Cards
```xml
<!-- Filled cards for secondary content -->
<com.google.android.material.card.MaterialCardView
    app:cardBackgroundColor="?attr/colorSurfaceVariant"
    app:cardCornerRadius="16dp"
    app:cardElevation="0dp"
    app:strokeWidth="0dp">
```

## Color System Integration

The card system integrates seamlessly with the app's ocean-inspired Material Design 3 color palette:

- **Primary Cards**: `colorSurface` with elevation shadows
- **Secondary Cards**: `colorSurfaceVariant` for subtle differentiation
- **Accent Cards**: `colorPrimaryContainer` for headers and important content

## Accessibility Considerations

1. **Sufficient Contrast**: All card backgrounds maintain proper contrast ratios
2. **Touch Targets**: Minimum 48dp touch targets for interactive cards
3. **Focus Indicators**: Material ripple effects for interactive feedback
4. **Screen Reader Support**: Proper content descriptions and semantic markup

## Migration from Bordered Cards

The previous implementation used bordered cards (`strokeWidth="1dp"`). The new system:

1. **Removes all borders** for a cleaner, modern appearance
2. **Adds subtle elevation** for depth and hierarchy
3. **Increases corner radius** for a softer, more contemporary feel
4. **Uses surface tinting** for better integration with Material You theming

## Best Practices

1. **Use elevated cards** for primary content and interactive elements
2. **Use filled cards** for secondary content and grouping
3. **Maintain consistent spacing** (8dp margins for cards)
4. **Follow elevation hierarchy** to establish visual importance
5. **Avoid mixing card types** within the same content group
6. **Test in both light and dark themes** for optimal appearance

## Resources

- [Material Design 3 Cards](https://m3.material.io/components/cards)
- [Material Design 3 Elevation](https://m3.material.io/styles/elevation/overview)
- [Material You Color System](https://m3.material.io/styles/color/overview) 