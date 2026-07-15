# AgroTrace

AgroTrace is a modern, AI-powered Android application designed to digitize and manage agricultural documents. It specializes in extracting structured data from various Sri Lankan agricultural forms (Sinhala and English) using Google ML Kit and Gemini AI.

## 🚀 Features

-   **AI-Powered Extraction**: Uses Google Gemini AI to accurately extract complex fields from scanned images.
-   **Intelligent Document Scanning**: Integrated with Google ML Kit Document Scanner for high-quality image capture and perspective correction.
-   **Dynamic Form Review**: Automatically generates editable forms from AI-extracted JSON, allowing users to verify and correct data easily.
-   **Local Journal (History)**: Stores all scanned documents locally using Room Database for offline access and record-keeping.
-   **Dashboard Analytics**: Provides a quick overview of scanning activity, including total documents and daily stats.
-   **Single Activity Architecture**: Uses Jetpack Navigation Component for a smooth, modern fragment-based user experience.
-   **Material 3 Design**: Clean, professional UI following the latest Android design guidelines.

## 🛠️ Technology Stack

-   **Language**: Kotlin
-   **Architecture**: MVVM (Model-View-ViewModel)
-   **UI Framework**: Fragments with ViewBinding & Material 3
-   **Navigation**: Jetpack Navigation Component
-   **AI/ML**: 
    -   Google ML Kit (Document Scanner)
    -   Google Gemini AI (Generative AI for data extraction)
-   **Database**: Room
-   **Networking**: Ktor Client
-   **Dependency Management**: Gradle Version Catalog (libs.versions.toml)

## 📋 Supported Document Types

1.  Land Approval Form
2.  Crop Registration Form
3.  Field/Lot Inspection Report
4.  Final Field Inspection Report
5.  Seed Test Request Form
6.  Seed Test Report
7.  Labeling Document

## ⚙️ Setup

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/Thishanka-SE03/agrotrace.git
    ```
2.  **Add Gemini API Key**:
    Add your API key to `local.properties`:
    ```properties
    GEMINI_API_KEY=your_api_key_here
    ```
3.  **Build and Run**: Open the project in Android Studio (Ladybug or newer) and run it on a device or emulator with Google Play Services.

## 📸 Screenshots

*(Add screenshots here)*

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.