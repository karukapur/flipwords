---
name: FlipWords Mobile Design System
colors:
  surface: '#f7faf7'
  surface-dim: '#d7dbd8'
  surface-bright: '#f7faf7'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f1f4f1'
  surface-container: '#ebefec'
  surface-container-high: '#e5e9e6'
  surface-container-highest: '#e0e3e0'
  on-surface: '#181c1b'
  on-surface-variant: '#3e4945'
  inverse-surface: '#2d3130'
  inverse-on-surface: '#eef2ee'
  outline: '#6e7975'
  outline-variant: '#bec9c4'
  surface-tint: '#006b59'
  primary: '#005344'
  on-primary: '#ffffff'
  primary-container: '#006d5b'
  on-primary-container: '#96ebd5'
  inverse-primary: '#81d6c0'
  secondary: '#4f616b'
  on-secondary: '#ffffff'
  secondary-container: '#cfe3ee'
  on-secondary-container: '#53656f'
  tertiary: '#624200'
  on-tertiary: '#ffffff'
  tertiary-container: '#805800'
  on-tertiary-container: '#ffd593'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#9df3dc'
  primary-fixed-dim: '#81d6c0'
  on-primary-fixed: '#00201a'
  on-primary-fixed-variant: '#005143'
  secondary-fixed: '#d2e5f1'
  secondary-fixed-dim: '#b6c9d5'
  on-secondary-fixed: '#0b1e26'
  on-secondary-fixed-variant: '#374953'
  tertiary-fixed: '#ffdeac'
  tertiary-fixed-dim: '#ffba38'
  on-tertiary-fixed: '#281900'
  on-tertiary-fixed-variant: '#604100'
  background: '#f7faf7'
  on-background: '#181c1b'
  surface-variant: '#e0e3e0'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 57px
    fontWeight: '700'
    lineHeight: 64px
    letterSpacing: -0.25px
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
  hanzi-display:
    fontFamily: Noto Serif TC
    fontSize: 48px
    fontWeight: '500'
    lineHeight: 60px
  hanzi-card:
    fontFamily: Noto Serif TC
    fontSize: 32px
    fontWeight: '500'
    lineHeight: 44px
  title-lg:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '500'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0.5px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0.25px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.1px
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.5px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 4px
  xs: 8px
  sm: 12px
  md: 16px
  lg: 24px
  xl: 32px
  2xl: 48px
  margin-mobile: 20px
  gutter-mobile: 16px
---

## Brand & Style
The design system focuses on a premium, tactile, and highly legible language-learning experience specifically optimized for high-end foldable mobile devices. The personality is "The Sophisticated Mentor": professional and deep, yet warm and encouraging. 

The aesthetic is a refined evolution of **Material 3 (M3)**, blending **Minimalism** with **Modern Corporate** sensibilities. It prioritizes clarity for language acquisition, utilizing expansive whitespace and high-contrast typography to reduce cognitive load. The UI relies on soft, pill-shaped elements and organic, oversized containers to create a "digital paper" feel that is both contemporary and approachable.

## Colors
The palette is grounded in a warm, ivory background to reduce eye strain during long study sessions. 
- **Primary (Deep Jade):** Used for main actions, active states, and branding elements. It conveys growth and stability.
- **Secondary (Muted Ink Blue):** Used for supportive UI elements, icons, and non-primary interactive components.
- **Accent (Soft Amber):** Reserved for achievements, streak indicators, and "aha!" moments. Use sparingly to maintain a premium feel.
- **Neutral/Text:** High-contrast charcoal is used for primary content, while the cool gray handles metadata and labels to create clear information hierarchy.

## Typography
The system uses **Inter** for all Latin characters and UI controls to ensure maximum legibility and a modern, neutral tone. For Chinese characters (Hanzi), use a refined Traditional Chinese Serif (like Noto Serif TC) to provide an elegant, literary contrast that emphasizes the beauty of the calligraphy.

**Hierarchy Rules:**
- **Hanzi Display:** Specifically for flashcards and main lesson headers. These should always be 25-50% larger than accompanying English text.
- **Body Text:** Use standard `body-lg` for lesson content and `body-md` for descriptions.
- **Labels:** Use `label-lg` for buttons and navigation items.

## Layout & Spacing
The layout follows a **fluid grid** model optimized for the narrow but tall aspect ratio of the Z Flip. 

- **Margins:** A consistent 20px side margin ensures content does not feel cramped by the device bezel.
- **Vertical Spacing:** Use an 8px base grid. Group related items with 8px or 12px; separate distinct sections with 32px or 48px to create "breathing room."
- **Safe Areas:** Strictly observe the Android system bars. Top-heavy layouts should be avoided to keep primary interactive elements (like the "Flip" button) within the lower two-thirds of the screen for one-handed use.

## Elevation & Depth
Depth is communicated through **Tonal Layering** and soft, ambient shadows rather than harsh borders. 

- **Level 0 (Background):** #FEFAF6.
- **Level 1 (Cards):** Pure White (#FFFFFF) with a 2px-8px blur shadow, 4% opacity (Primary Color tint). Use this for flashcards and lesson modules.
- **Level 2 (Floating/Active):** Slightly higher elevation (12px blur) for active drag-and-drop elements or bottom sheets.
- **Overlays:** 20% Black scrim for modals to focus the user's attention entirely on the learning task.

## Shapes
The shape language is characterized by "Super-ellipses" and heavy rounding to evoke friendliness and safety.

- **Primary Cards:** 28px - 32px corner radius. This mimics the physical rounded corners of the mobile device.
- **Buttons & Chips:** Always use fully rounded (pill-shaped) corners for buttons.
- **Input Fields:** 16px corner radius to distinguish them from the larger structural cards.

## Components
- **Primary Button:** Large, pill-shaped, #006D5B background with White text. Minimum height: 56px for easy thumb tapping.
- **Learning Cards:** Oversized containers with 32px rounding. Content should be centered vertically and horizontally. 
- **Segmented Controls:** Used for switching between "Traditional," "Simplified," and "Pinyin" views. Uses a tonal background (#EEF2F1) with a white pill indicator for the active state.
- **Bottom Navigation:** Material 3 style with an active indicator (pill shape around the icon). No labels for a cleaner look, or small `label-sm` text.
- **Progress Sliders:** Thick, 8px tracks with a large, circular #FFB300 thumb to indicate lesson progress.
- **Status Chips:** Small, 12px font, 16px vertical padding, used for "New," "Review," or "Mastered" tags.
- **Translation Bottom Sheet:** Used for quick-lookups; should slide up from the bottom with a prominent drag handle and 32px top corner rounding.