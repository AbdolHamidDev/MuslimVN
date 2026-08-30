# Fix Reciter Change Logic and Download Mechanism

The goal is to fix the bug where changing the reciter doesn't update the currently playing audio and to ensure the download mechanism correctly handles multiple reciters independently.

## User Review Required

> [!IMPORTANT]
> The playback will be interrupted for a brief moment when switching reciters as the playlist needs to be reloaded with new URLs. The position will be maintained (starting from the beginning of the current ayah).

## Proposed Changes

### Data Layer

#### [MODIFY] [QuranDao.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/data/local/dao/QuranDao.kt)
- Add a reactive `Flow` method for counting downloaded ayahs to avoid manual polling in the repository.

#### [MODIFY] [QuranRepositoryImpl.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/data/repository/QuranRepositoryImpl.kt)
- Update `startSurahDownload` to use a unique work name that includes the `reciterId`. This allows simultaneous or sequential downloads for different reciters of the same Surah.
- Update `getDownloadedAyahsCount` to use the new reactive DAO method.

### Presentation Layer

#### [MODIFY] [SurahDetailViewModel.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/presentation/viewmodels/SurahDetailViewModel.kt)
- Modify `playAyah` to accept a `forceReload` parameter.
- Update `observeSettingsChanges` to call `playAyah` with `forceReload = true` when the reciter identifier changes, ensuring the new voice is loaded immediately.
- Update `observeDownloadStatus` to be more efficient by using the reactive count flow.

## Verification Plan

### Automated Tests
- N/A (Unit tests could be added for ViewModel logic, but manual verification is more effective for audio behavior).

### Manual Verification
1. Open a Surah and start playing.
2. Go to Settings and change the Reciter.
3. Verify that the audio player reloads and starts playing with the new reciter's voice from the current ayah.
4. Click Download for one reciter, verify progress.
5. Change to another reciter, verify that the download count/progress reflects the new reciter (should be 0 if not downloaded).
6. Click Download for the second reciter and verify it starts correctly without being blocked by the first download.
