# Implementation Plan - Mach Zen Documents Tab using IslamHouse API v3

Implement a dedicated Documents tab for scholar Mach Zen (`mach_zen`) in `MuslimVN` app, consuming the official IslamHouse API v3.

## Proposed Changes

### 1. Data Layer (`com.example.muslimvn.data`)
#### [NEW] [IslamHouseApiService.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/data/remote/IslamHouseApiService.kt)
- Define Retrofit interface for IslamHouse API v3:
  `GET v3/paV29H2gm56kVLPy/main/get-author-items/{authorId}/showall/{displayLang}/{interfaceLang}/{page}/{limit}/json`

#### [NEW] [IslamHouseModels.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/data/remote/model/IslamHouseModels.kt)
- Define data classes for `IslamHouseResponse`, `IslamHouseItem`, `Attachment`, and `PaginationLinks`.

#### [NEW] [IslamHouseRepository.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/data/repository/IslamHouseRepository.kt)
- Implement repository and data source to fetch author items with pagination.

### 2. Dependency Injection (`com.example.muslimvn.core.di`)
#### [MODIFY] [NetworkModule.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/core/di/NetworkModule.kt)
- Provide `IslamHouseApiService` Retrofit client pointing to `https://api3.islamhouse.com/`.

#### [MODIFY] [RepositoryModule.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/core/di/RepositoryModule.kt)
- Provide binding for `IslamHouseRepository`.

### 3. ViewModel (`com.example.muslimvn.presentation.screens.vietnamscholars`)
#### [MODIFY] [MachZenViewModel.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/presentation/screens/vietnamscholars/MachZenViewModel.kt)
- Integrate `IslamHouseRepository` into `MachZenViewModel`.
- Add states for documents (`documents`, `isLoadingDocuments`, `documentHasNextPage`, `documentCurrentPage`).
- Implement `fetchDocuments()` and `loadMoreDocuments()`.

### 4. UI Layer (`com.example.muslimvn.presentation.screens.vietnamscholars`)
#### [MODIFY] [MachZenScreen.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/presentation/screens/vietnamscholars/MachZenScreen.kt)
- Replace the placeholder in `selectedTab == 1` with a fully featured Material 3 document list (`MachZenDocumentItemCard`) with support for infinite scrolling, file type badges, and download/view actions.

## Verification Plan

### Automated Tests
- Build project successfully with `./gradlew assembleDebug`.

### Manual Verification
- Run app on emulator/device, navigate to Scholar Mách Zên, switch to "Tài liệu" tab, verify documents are fetched from IslamHouse API v3 and rendered smoothly, test pagination (load more) and clicking a document to view/download.
