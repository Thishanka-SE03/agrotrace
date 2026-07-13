# AgroTrace UI/UX Upgrade

This project keeps the existing Kotlin/XML architecture and the current ML Kit → Gemini workflow.

## Implemented

- Material 3 agriculture-focused visual system with light/dark resources
- Improved bottom navigation and focused nested-screen navigation
- Dashboard with real Room statistics and recent saved documents
- Clear selection UI for all seven supported document types
- ML Kit/Gemini extraction progress pipeline with scan preview
- Generic editable review UI for all seven Gemini JSON schemas
- Missing-value highlighting and manual correction support
- Saving corrected JSON into the existing Room history database
- Search, category chips, and date filtering in History
- Dedicated document detail, share, and delete screen
- Future Supabase sync status messaging
- Improved officer profile and scan guidance

## Existing integrations preserved

- Google ML Kit Document Scanner
- Gemini API client and per-document prompts
- Room local database
- Supabase configuration placeholders

## Build note

The project was statically validated in this environment (XML parsing, navigation/action checks, Kotlin parser checks). A full Gradle build could not be run because the isolated environment could not download the configured Gradle distribution and Maven dependencies.
