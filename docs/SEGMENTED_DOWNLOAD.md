# Segmented download (download only part of a video)

This guide explains how MintPlay downloads just a slice of a YouTube video, for example
**02:10:20 to 03:10:20**, instead of the whole thing. It also covers the other ways to do it,
and what to try when something goes wrong.

> Status: the in-app feature was written without being run on a device. If something fails,
> the red error text on the Download screen shows the last line of the FFmpeg log.

---

## 1. Using it in the app

1. Paste the link and tap **Check available qualities**.
2. Pick a quality.
3. Turn on **Download only a segment**.
4. Type a start and an end time. Accepted formats: `H:MM:SS`, `MM:SS` or plain seconds
   (`2:10:20`, `130:20`, `7820`).
5. Tap **Download segment**.

The file is saved as `Video title (02-10-20 to 03-10-20).mkv`.

Rules the app enforces:

- End must be after start.
- Start must be inside the video. An end time past the video length is clamped to the length.

---

## 2. How it works

```
yt-dlp --dump-json            (once: slow, solves YouTube's JS challenge)
        |
        v
info.json  ->  pick best video stream + best audio stream (direct https URLs)
        |
        v
FFmpegKit:  -ss START -i VIDEO_URL  -ss START -i AUDIO_URL  -t LENGTH  -c copy  out.mkv
```

Key points:

- **Only the slice is downloaded.** With `-ss` placed *before* `-i`, FFmpeg seeks inside the
  remote file using HTTP range requests, so it never fetches the part before the start time.
- **No re-encoding.** `-c copy` copies the streams as they are, which is fast and lossless.
- **No second extraction.** The stream URLs come from the info that was already fetched when
  you checked the qualities, so there is no extra 30 second wait.
- **Format choice.** Video: the highest height up to the chosen quality, preferring H.264
  (plays everywhere), then VP9, then AV1. Audio: the highest bitrate, original language first.
- **Why not yt-dlp's own option?** `--download-sections` makes yt-dlp call a separate
  `ffmpeg` *binary*. FFmpegKit is a library inside the app, not a binary yt-dlp can run, so
  the app calls FFmpegKit directly with the same idea.

---

## 3. The same thing on a computer

With plain yt-dlp (needs ffmpeg installed):

```bash
yt-dlp --download-sections "*2:10:20-3:10:20" \
       -f "bv*[height<=1080]+ba" --merge-output-format mkv \
       -o "%(title)s [%(section_start)s-%(section_end)s].%(ext)s" \
       "https://www.youtube.com/watch?v=VIDEO_ID"
```

- The `*` before the times means "this is a time range", not a chapter name.
- Add `--force-keyframes-at-cuts` for frame-accurate cuts (slower, because it re-encodes
  around the cut points).
- Several ranges: repeat `--download-sections`.

With FFmpeg only, using direct URLs from `yt-dlp -g`:

```bash
ffmpeg -ss 02:10:20 -i "VIDEO_URL" -ss 02:10:20 -i "AUDIO_URL" \
       -t 01:00:00 -map 0:v:0 -map 1:a:0 -c copy -avoid_negative_ts make_zero out.mkv
```

---

## 4. Accuracy: keyframes

Because the app copies the streams instead of re-encoding them, a cut can only start on a
**keyframe**. YouTube keyframes are typically 2 to 5 seconds apart, so:

- The clip may begin a few seconds **before** the time you typed.
- The end is cut at the requested length, so it is accurate to within a frame or so.

If you need an exact start, the only way is to re-encode:

```bash
ffmpeg -ss START -i VIDEO_URL -ss START -i AUDIO_URL -t LENGTH \
       -map 0:v:0 -map 1:a:0 -c:v libvpx-vp9 -c:a copy out.mkv
```

The `ffmpeg-kit-full` build is LGPL, so it has no `libx264`. Use `libvpx-vp9` or the Android
hardware encoder (`h264_mediacodec`), and check what is available with `-encoders`. Re-encoding
is much slower and uses a lot of battery.

---

## 5. Troubleshooting

| Symptom | Likely cause | What to try |
| --- | --- | --- |
| Error mentions `403 Forbidden` | YouTube rejected the stream URL | The app retries once with another YouTube client automatically. If it still fails, restart the app (yt-dlp updates on start) and try again |
| Very slow, even for a short clip | YouTube throttles plain, unchunked requests | Try a lower quality, or download the whole video (the normal path uses yt-dlp's chunked downloader) |
| Clip starts a few seconds early | Keyframe cut | Expected; see section 4 |
| Video plays but audio is out of sync | Different keyframe alignment in the two streams | Try another quality (it may pick a different codec) |
| "No directly downloadable ... stream" | The video only offers streams without a direct URL | Use the normal full download |
| Start is past the end of the video | Wrong time format (e.g. `1:30` is 1 min 30 s, not 1 h 30 min) | Write it as `1:30:00` |

---

## 6. Alternative: let yt-dlp do the cutting

If you would rather use `--download-sections` inside the app:

1. Add the library's FFmpeg module next to the existing one:
   `io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1`
2. Call `FFmpeg.getInstance().init(context)` right after `YoutubeDL.getInstance().init(context)`.
3. Add `--download-sections "*START-END"` to the yt-dlp request (and, if wanted,
   `--force-keyframes-at-cuts`).

Trade-off: it adds a second FFmpeg to the app (the project already ships FFmpegKit), which can
cause duplicate native library errors at build time, and it makes the APK larger. It was not
done here because the direct FFmpegKit route needs no new dependencies.

---

## 7. Why checking a video takes long, and how the app speeds it up

- Most of the wait is yt-dlp running YouTube's JavaScript challenge on the phone. That cost
  is paid **once per video** now: the result is saved as `info.json` and reused by the video
  download, the audio download and the segment download (`--load-info-json`).
- yt-dlp now keeps a cache folder (`--cache-dir`), so work that can be reused between runs is
  not thrown away.
- yt-dlp is started and updated in the background as soon as the Download screen opens.
- Stream URLs in the saved info expire after a few hours, so the app fetches fresh info when
  the saved copy is older than 4 hours.

### Is the download parallel?

Not really, for ordinary YouTube downloads:

- `yt-dlp -N 4` (`--concurrent-fragments`) only helps formats delivered as many small
  fragments (HLS or DASH manifests). YouTube's normal video and audio files are single files
  fetched in 10 MB chunks one after another, so `-N` does nothing there.
- Video and audio are downloaded one after the other. Audio is a small fraction of the size,
  so running them together would save little.
- The real multi-connection option is **aria2c** (`--downloader aria2c`), available through
  `io.github.junkfood02.youtubedl-android:aria2c`. It can open several connections to one file,
  but YouTube sometimes answers many parallel connections with 403, and it adds another native
  library to the app.
