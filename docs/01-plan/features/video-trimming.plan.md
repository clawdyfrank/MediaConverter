# Plan: Video Trimming (MediaConverter)

## 1. 개요 (Overview)
사용자가 안드로이드 앱(MediaConverter)에서 동영상을 오디오로 변환하기 전, 원하는 구간(시작점과 끝점)만 잘라낼 수 있는 기능(Video Trimming)을 제공합니다.

## 2. 유저 스토리 (User Stories)
- 사용자는 변환할 동영상을 선택한 후, 전체 영상 대신 특정 구간만 선택할 수 있어야 한다.
- 사용자는 UI에서 시작 시간(Start Time)과 종료 시간(End Time)을 직관적으로 설정할 수 있어야 한다.
- 사용자는 선택한 구간만 오디오(MP3, M4A 등)로 변환할 수 있어야 한다.

## 3. 기능 요구사항 (Acceptance Criteria)
1. **구간 설정 UI**: 
   - Jetpack Compose를 활용하여 영상의 시작/종료 시간을 조절할 수 있는 Range Slider(또는 텍스트 입력 UI)를 제공해야 한다.
2. **FFmpeg 연동**:
   - `ffmpeg-kit-lts-min-16kb`를 사용하여 선택된 시작(`-ss`) 및 종료(`-to` 또는 `-t`) 구간만 추출하여 변환해야 한다.
3. **재생 및 확인(선택적/기본 요건)**:
   - 선택된 구간의 총 길이를 화면에 표시하여 사용자가 알 수 있게 한다.
4. **결과물 저장**:
   - 변환된 오디오는 기존과 동일하게 디바이스의 `Music/Convert` 폴더에 정상적으로 저장되어야 한다.

## 4. 제외 대상 (Out-of-Scope)
- 영상 자체를 자르고 새로운 비디오 파일(MP4 등)로 저장하는 기능 (현재는 오디오 변환 시에만 구간 적용).
- 밀리초(ms) 단위의 초정밀 프레임 단위 편집 UI.
- 영상 썸네일 스트립(Thumbnail Strip)을 프레임별로 보여주는 복잡한 타임라인 UI (추후 고도화 시 고려, 초기 버전은 단순 슬라이더/시간 입력 기반).

## 5. 다음 단계 (Next Steps)
- 본 Plan이 확정되면 `/pdca design video-trimming` 명령을 통해 아키텍처 및 FFmpeg 명령어 설계를 진행합니다.