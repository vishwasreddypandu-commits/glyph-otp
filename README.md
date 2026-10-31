# Glyph OTP
Shows one-time codes from incoming notifications on the Nothing Glyph Matrix (Phone (4a) Pro / Phone (3)).

* Built with GitHub Actions: push to `main`, then download `app-debug.apk` from the repo's **Releases > Latest build** on your phone and install it.
* Needs: system version 20250801+ (app matrix API), notification access, and for the always-on toy: Settings > Glyph Interface > Flip to Glyph > Always-on Glyph Toy.
* Privacy: reads notification text locally only to find a 4-6 digit code next to words like "code/OTP/verification". No network permission, no logging.
* Uses the Nothing GlyphMatrix SDK (`app/libs`, see SDK-LICENSE.md).
