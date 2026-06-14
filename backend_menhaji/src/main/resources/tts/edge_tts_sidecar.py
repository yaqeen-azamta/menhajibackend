#!/usr/bin/env python3
"""
Edge TTS sidecar — used by TtsService.java to synthesize speech with
Microsoft Edge's free neural voices.

Why a Python sidecar at all
---------------------------
Microsoft Edge's "Read aloud" feature talks to a public neural-TTS endpoint.
The `edge-tts` Python library (https://pypi.org/project/edge-tts/) wraps
that endpoint. Same voice quality you'd pay for on Azure Cognitive
Services — no API key, no signup, no credit card. There is no equivalent
maintained Java client, so we shell out to Python once per synthesis.

Voices in use
-------------
    Arabic   ar-JO-SanaNeural   — Jordanian female, Levantine accent
                                  closest to Palestinian Arabic.
    English  en-US-AriaNeural   — Microsoft's flagship US female voice,
                                  designed for "teacher" tone.

Process protocol (TtsService.synthesizeViaEdge spawns us)
---------------------------------------------------------
    argv:   python edge_tts_sidecar.py <voice> <output_path>
    stdin:  raw UTF-8 bytes of the text to speak
    stdout: silent on success (we write nothing on the happy path)
    stderr: error message on failure, prefixed with the Python exception type
    exit:   0 ok
            2 bad argv
            3 empty / whitespace-only text
            4 synthesis failure (network, voice not found, etc.)

Encoding gotcha — *read this before touching stdin handling*
------------------------------------------------------------
On Windows, `sys.stdin` is a *text* stream whose encoding is derived from
the system locale (often cp1252) and whose decode-error policy defaults
to ``surrogateescape``. If the Java caller writes valid UTF-8 bytes —
which it does, intentionally, to dodge command-line argv encoding issues
with Arabic — Python decodes those bytes one by one as cp1252 and any
byte that has no cp1252 mapping (e.g. ``0x81``, common as the second byte
of an Arabic UTF-8 sequence such as ``ف`` = ``0xd9 0x81``) gets mapped to
a lone surrogate in the U+DC80–U+DCFF range. The string then looks
half-Latin-1-half-garbage, and the moment `edge-tts` tries to re-encode
it as UTF-8 for the network call, Python raises::

    UnicodeEncodeError: 'utf-8' codec can't encode character '\\udc81' …
                        surrogates not allowed

The fix is to skip the text stream entirely: read ``sys.stdin.buffer``
(the underlying binary stream) and decode the bytes ourselves with the
exact encoding the Java caller used. We also pass ``errors="replace"``
as a last-resort guard against any genuine bad bytes in the curriculum
(unlikely — the JSON is strict UTF-8 — but harmless to defend), and
strip surviving lone surrogates before calling ``edge-tts``.

Cache strategy is owned by the Java caller — once an mp3 is produced for
a given (questionId, language) it's persisted to Question.audioUrl and
this script is never invoked again for the same input. So a per-process
~1.5 s Python startup is paid once per question per lifetime.
"""
import asyncio
import sys

import edge_tts


def _read_text_from_stdin() -> str:
    """Read raw UTF-8 bytes from stdin and return a clean ``str``.

    Bypasses ``sys.stdin``'s locale-driven text decoding (see module
    docstring), then drops any U+DC80–U+DCFF lone surrogates that might
    have slipped through, so the result is guaranteed encodable as UTF-8
    by ``edge-tts``.
    """
    raw = sys.stdin.buffer.read()
    text = raw.decode("utf-8", errors="replace")
    if any(0xD800 <= ord(ch) <= 0xDFFF for ch in text):
        text = "".join(
            ch for ch in text if not (0xD800 <= ord(ch) <= 0xDFFF)
        )
    return text


async def synthesize(voice: str, text: str, output_path: str) -> None:
    """One synthesize call. ``edge_tts.Communicate`` streams chunks; ``.save``
    accumulates them into the given mp3 path."""
    communicate = edge_tts.Communicate(text=text, voice=voice)
    await communicate.save(output_path)


def main() -> int:
    if len(sys.argv) != 3:
        print(
            "Usage: edge_tts_sidecar.py <voice> <output_path>",
            file=sys.stderr,
        )
        return 2

    voice = sys.argv[1]
    output_path = sys.argv[2]

    text = _read_text_from_stdin()
    if not text.strip():
        print("Empty text on stdin", file=sys.stderr)
        return 3

    try:
        asyncio.run(synthesize(voice, text, output_path))
        return 0
    except Exception as exc:  # noqa: BLE001 — surface any failure verbatim
        print(f"edge-tts failed: {type(exc).__name__}: {exc}", file=sys.stderr)
        return 4


if __name__ == "__main__":
    sys.exit(main())
