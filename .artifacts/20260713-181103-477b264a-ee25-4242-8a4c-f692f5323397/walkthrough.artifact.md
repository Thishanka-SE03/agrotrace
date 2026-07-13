# UI Design Walkthrough - AgroTrace

The UI of the AgroTrace application has been redesigned to match the provided reference image. This includes a comprehensive overhaul of the Home screen, Scanner, Extraction progress, Results form, History, and Navigation components.

## Key Changes

### 1. Visual Identity and Styling
- **Color Palette**: Updated `colors.xml` with the primary green theme and specific colors for different document types.
- **Icons**: Created several vector drawables for menu, notifications, home, person, and document actions (rotate, crop, enhance).
- **Typography**: Applied Material 3 typography guidelines with bold headers and clear subtitles.

### 2. Core Screens Redesign
- **Home Screen**: Implemented a modern dashboard with a logo, tagline, "Scan New Document" call-to-action, horizontal document type selector, and overview statistics.
- **Select Document Type**: A dedicated screen with a vertical list of document types, each with its own color-coded icon.
- **Camera/Scanner**: A full-screen camera interface with a document framing overlay, flash/settings controls, and specialized capture/auto-enhance buttons.
- **Extraction Progress**: A clean progress screen with a large circular indicator and a step-by-step checklist of the AI processing status.
- **Extracted Data**: A functional form for reviewing and editing AI-extracted data before saving, including success banners and field-level validation icons.
- **History & Filters**: A refined history list with search/filter capabilities and status indicators for each scanned document.

### 3. Navigation
- **Bottom Navigation**: Integrated a persistent bottom bar for quick access to Home, Scan, History, and Profile.
- **Navigation Drawer**: Added a sidebar for secondary actions like Settings, Backup, and Support, featuring a profile header.

## Layout Files Summary
- [activity_main.xml](file:///C:/Users/Shadow/AndroidStudioProjects/agrotrace/app/src/main/res/layout/activity_main.xml): Home Screen with Drawer and Bottom Nav.
- [activity_select_doc_type.xml](file:///C:/Users/Shadow/AndroidStudioProjects/agrotrace/app/src/main/res/layout/activity_select_doc_type.xml): Document selection list.
- [activity_scanner.xml](file:///C:/Users/Shadow/AndroidStudioProjects/agrotrace/app/src/main/res/layout/activity_scanner.xml): Camera interface.
- [activity_extraction.xml](file:///C:/Users/Shadow/AndroidStudioProjects/agrotrace/app/src/main/res/layout/activity_extraction.xml): Progress screen.
- [activity_extracted_data.xml](file:///C:/Users/Shadow/AndroidStudioProjects/agrotrace/app/src/main/res/layout/activity_extracted_data.xml): Results form.
- [activity_history.xml](file:///C:/Users/Shadow/AndroidStudioProjects/agrotrace/app/src/main/res/layout/activity_history.xml): History list.
- [activity_doc_details.xml](file:///C:/Users/Shadow/AndroidStudioProjects/agrotrace/app/src/main/res/layout/activity_doc_details.xml): Detailed document view with image tools.
- [activity_filters.xml](file:///C:/Users/Shadow/AndroidStudioProjects/agrotrace/app/src/main/res/layout/activity_filters.xml): Search and filter options.

## Verification Results
- All layouts follow the `ConstraintLayout` best practices for responsiveness.
- Colors and icons are consistent with the provided design.
- Basic navigation (Home -> Scan, Home -> History, Menu -> Drawer) is functional.
