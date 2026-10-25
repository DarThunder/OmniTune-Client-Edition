# OmniTune for Android

Native app (Kotlin + Jetpack Compose) for your OmniTune server, organized like Spotify
(Home / Search / Library, mini-player, and a "Now Playing" screen) and featuring your color palette.

## Setup and Run

1. Android Studio (Ladybug or newer) → **Open** → the `omnitune-android` folder.
2. Let Gradle sync (it will download wrapper 8.9). If prompted to update AGP/Kotlin/libraries,
   accept; the versions are defined in `gradle/libs.versions.toml`.
3. Run on a device with Tailscale active.
4. On the login screen: server = `https://<your-machine>.<your-tailnet>.ts.net`, your OSIRIS username and password.

## Architecture

```
data/        Models, Prefs (tokens encrypted with Keystore), SessionManager, Repository
data/net/    Retrofit (AuthApi, OmniApi) + interceptors (dynamic host, Bearer, refresh)
player/      PlaybackService (Media3) + PlayerController (UI ↔ service bridge)
ui/          Root MainViewModel, navigation, screens, theme
```

### Session & Refresh (The Tricky Part)

OSIRIS rotates the refresh token and **revokes the entire session if it detects reuse**. Therefore:

- Only one `/refresh` is in flight at a time: `SessionManager.refresh()` acquires a lock; callers
  waiting on it reuse the token already refreshed by the first caller.
- Proactive refresh happens when the access token has <30s left, so ExoPlayer, Coil, and
  Retrofit almost never encounter a 401.
- Only a 400–403 on `/refresh` closes the session; a 429, 5xx, or network failure does not.
- The Bearer token is only sent to your server's host, never to external hosts (YouTube thumbnails).

**Known limitation:** If a successful `/refresh` response is lost due to a network error, the old token
is already consumed, and the next attempt will be seen as reuse → the user will have to log in again.
