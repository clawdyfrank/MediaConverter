# Design: Video Trimming (MediaConverter)

## 1. 아키텍처 및 폴더 구조 (Architecture & Structure)
이 기능은 기존 MediaConverter 안드로이드 앱의 아키텍처(MVVM 패턴, Jetpack Compose UI)에 통합됩니다.

- **UI 계층**: `ConversionScreen.kt` (또는 변환 설정 다이얼로그) 내에 구간 설정 UI(Trimming UI) 컴포넌트 추가.
- **ViewModel 계층**: `ConversionViewModel.kt` 에 시작/종료 시간 상태(State)를 추가하고, 사용자의 입력 값을 검증/저장.
- **Repository/UseCase 계층**: `FFmpegRepository.kt` 의 변환 로직에 파라미터를 동적으로 추가.

## 2. 데이터 흐름 (Data Flow)
1. **사용자 액션**: 변환할 영상 선택 -> 구간 설정 슬라이더(또는 시간 입력란) 조작.
2. **ViewModel 상태 업데이트**: `startTime`과 `endTime` StateFlow 업데이트.
3. **변환 요청**: 사용자가 '변환(Convert)' 버튼 클릭.
4. **FFmpeg 명령어 생성**: ViewModel에서 설정된 시간값을 바탕으로 동적으로 FFmpeg 명령어를 구성하여 FFmpegKit 호출.
5. **UI 피드백**: 기존과 동일하게 변환 진행률(Progress Bar) 표시.

## 3. UI 컴포넌트 설계 (UI Component Design)
Jetpack Compose를 사용한 `TrimmingSliderSection` 컴포넌트 구상:
- **RangeSlider**: 전체 영상 길이(`totalDuration`)를 기반으로 0부터 `totalDuration`까지 범위를 갖는 양방향 슬라이더.
- **TimeText**: 현재 선택된 시작 시간(`startTime`)과 종료 시간(`endTime`)을 `MM:SS` 또는 `HH:MM:SS` 포맷으로 양옆에 표시.
- **DurationText**: `endTime - startTime` 계산 결과를 "총 선택된 길이: MM:SS" 형태로 하단에 표시.

## 4. FFmpeg 명령어 구조 (FFmpeg Command)
기존 오디오 변환 명령어에 구간 설정 옵션을 추가합니다.

**기본 포맷 (Fast Seek 방식 권장):**
```bash
-ss {startTime} -i {input_file} -to {endTime} -vn -c:a {audio_codec} -b:a {bitrate} {output_file}
```
* `-ss`: 입력 파일 전에 위치하여 빠르게 해당 시간으로 Seek.
* `-to`: 시작 시간(`-ss`) 기준으로 언제까지 읽을 것인지 지정 (또는 지속 시간 `-t` 사용 가능).
* `-vn`: 비디오 스트림 제거.

**예시 (10초부터 30초까지 자르기):**
```bash
-ss 00:00:10.000 -i input.mp4 -to 00:00:30.000 -vn -c:a libmp3lame -b:a 128k output.mp3
```

## 5. 엣지 케이스 및 예외 처리 (Edge Cases)
- `startTime`이 `endTime`보다 클 수 없도록 UI 락킹 또는 검증.
- `endTime`이 영상의 전체 길이(Total Duration)를 초과하지 않도록 검증.
- 영상 길이가 너무 짧은 경우(예: 1초 미만) 변환을 막거나 예외 처리.

## 6. 다음 단계 (Next Steps)
- 본 Design 문서를 기반으로 실제 안드로이드 코드 구현을 위해 `/pdca do video-trimming` 명령을 실행합니다.