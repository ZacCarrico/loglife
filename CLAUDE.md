Before proceeding, read and follow AGENTS.md

## Design & Style Guidelines

### Material Design 3 (Material You)

Follow the Material Design 3 style guide from https://m3.material.io/ and Android mobile UI design guidelines from https://developer.android.com/design/ui/mobile.

**Key Material Design 3 Principles:**

1. **Dynamic Color System**: Use Material You's dynamic color system which generates complete tonal palettes from a seed color, supporting both light and dark modes with proper accessibility.

2. **Color Roles**:
   - Primary: Main components like prominent buttons and active states
   - Secondary: Less prominent components like filter chips
   - Use appropriate "on" colors (onPrimary, onSecondary, etc.) for proper contrast

3. **Modern Aesthetics**: Embrace rounder elements, lighter shadows, generous white space, and new active states that work with dynamic colors.

4. **Typography & Spacing**: Follow Material 3 type scale and spacing guidelines for consistency.

**Dark/Light Mode Best Practices:**

1. **Dark Mode (Default)**:
   - Use smoky grays (not pure black) to reduce eye strain and pixel bleed
   - Desaturate colors while maintaining emotional resonance
   - Made for low-light environments with careful contrast

2. **Light Mode**:
   - Ensure proper contrast ratios for accessibility
   - Harmonize brand colors with user preferences

3. **Color Adaptation**: Colors should automatically adapt between light and dark themes using Material 3's color role system.

**Android Mobile UI Best Practices:**

1. **Responsive Design**: Account for different screen sizes (phones, tablets, foldables) using flexible layouts, not fixed ones.

2. **Navigation Patterns**:
   - Bottom navigation bars for 3-5 core destinations on phones
   - Navigation rails on the left for tablets
   - FAB for the most important screen action

3. **Accessibility**:
   - Proper color contrast (WCAG standards)
   - Screen reader labels on all interactive elements
   - Large tap targets (48dp minimum)
   - Readable typography

4. **User-First Design**: Remove anything that doesn't help the user. Use:
   - Calmer screens with focused layouts
   - Clear visual hierarchy
   - Intentional whitespace
   - Restrained, intelligent, empathetic design

5. **Platform Consistency**: Follow Android's Material Design conventions for familiar, intuitive experiences.

**Implementation Notes:**
- The app uses `Theme.Material3.DayNight.NoActionBar` as the base theme
- Implement dark mode as the default theme with toggle in settings
- Use Material 3 components from `com.google.android.material.*`
- Leverage dynamic theming where appropriate
