# WordStore App - Text Translation Feature

This Android application is a vocabulary notebook with text translation functionality using the Hugging Face Inference API.

## Features

- Text translation using the Hugging Face Inference API with the `google-t5/t5-base` model
- Both traditional View-based UI and Jetpack Compose UI implementations
- MVVM architecture with LiveData and Coroutines
- Error handling and loading state management

## Setup

1. Clone this repository
2. Open the project in Android Studio
3. Create or update your `local.properties` file to include your Hugging Face API key:

```
HUGGING_FACE_API_KEY=your_hugging_face_api_key_here
```

4. Sync the project and run it on an emulator or physical device

## Architecture

The app follows the MVVM (Model-View-ViewModel) architecture:

- **Model**: The data layer with `TranslationRequest` and `TranslationResponse` data classes
- **View**: UI implementation in both traditional XML (MainActivity) and Jetpack Compose (ComposeMainActivity)
- **ViewModel**: `TranslationViewModel` handling the business logic and API calls

## Network Configuration

- Retrofit for API requests
- OkHttp for HTTP client with logging interceptor
- Moshi for JSON serialization/deserialization

## Usage

1. Enter text in the input field
2. Click the "Translate" button
3. The translated text will appear below
4. If there is an error, an error message will be displayed

## Switching Between UI Implementations

The app includes two UI implementations:

1. Traditional XML-based UI: `MainActivity`
2. Jetpack Compose UI: `ComposeMainActivity`

To switch between them, modify the `AndroidManifest.xml` to change which activity has the launcher intent filter.

## Dependencies

- Retrofit and OkHttp for networking
- Moshi for JSON parsing
- Kotlin Coroutines for async operations
- AndroidX ViewModel and LiveData for state management
- Material Design components
- Jetpack Compose for modern UI (optional) 