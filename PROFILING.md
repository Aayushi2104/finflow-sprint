# Profiling

This project uses Java Flight Recorder (JFR) for runtime profiling.

## Record a profile

1. Start the target service normally.
2. Run:

```powershell
cd D:\ideaprojects\FinFlow
.\start-jfr-recording.ps1 -ServiceName admin-service -Duration 2m
```

3. Exercise the API during the recording window.
4. After the duration ends, the `.jfr` file is written under:

```text
D:\ideaprojects\FinFlow\profiling
```

## Check active recording

```powershell
cd D:\ideaprojects\FinFlow
.\check-jfr-recording.ps1 -ServiceName admin-service
```

## Open the result

Open the generated `.jfr` file in:

- JDK Mission Control (JMC), or
- VisualVM with JFR support

## What to show

- CPU hotspots
- memory allocation
- thread activity
- method execution behavior under load
