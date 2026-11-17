# LandPoint help

Everything in LandPoint runs on this phone, and this page runs on it too — it is bundled inside the app, so it reads exactly the same with no signal. Each section below matches one part of the app.

## Capture

**Save land** starts a record. Give the parcel a name, then place it in one of three ways: type the latitude and longitude straight off a survey letter or certificate, tap **Use my location** while you stand on the land, or leave both empty for now and add the boundary later.

While the GPS reads, LandPoint averages several fixes into one and shows the accuracy in metres. Go outdoors — roofs and dense canopy blur the fix — and let the accuracy settle before you accept the reading. Cell-tower and Wi-Fi positions are labelled as such, because they can be off by more than a city block.

**Find address** fills the address fields from the coordinates. It uses the phone's geocoder and needs a connection; everything else works offline.

Photos of the land, and photos of individual corners, come from the camera or the gallery. Each photo can carry a caption — for example, "boundary marker on the north side". Shared copies of a photo can have their GPS tags removed; the setting is under Privacy and security.

## Measure

**Mark on the map** opens the corner picker, where a boundary is drawn corner by corner. There is more than one way to place a corner, because no single way fits every piece of ground:

- Stand on the corner and take a GPS fix where you are.
- Place a mark at the crosshair on the map, then drag or nudge it before you accept it.
- Type the coordinates as decimal degrees, exactly as a survey letter prints them.
- Measure a corner from another one by distance and bearing, the way deed descriptions give their sides.
- Copy the corners two parcels share, so the shared boundary matches exactly.

Three or more corners close into a shape and show its area and perimeter. If two sides cross, the app warns you — a crossed outline is not one enclosed shape, and its area is not the ground it covers. Usually one corner is out of sequence and needs moving up or down.

**Walk the boundary** records the outline continuously while you walk it, for edges too long or too overgrown to stand on every corner. Corner-by-corner stays the more accurate method; the walk needs a clear view of the sky and a closed loop — finish where you started.

Every measured corner keeps its accuracy, so you can always tell a solid fix from a shaky one and know which corners are worth measuring again.

## Map

The **Map** tab shows every saved parcel. Tap a pin or a drawn outline to open its record. The style picker floats at the top of the map: street, satellite, terrain, or a map file you added yourself.

Satellite imagery shows the ground as a camera saw it on one day. Use it to recognise the place — never as evidence of where a boundary runs.

With location permission granted, the readout chip opens your position: latitude, longitude, accuracy, altitude, speed, heading, which satellites the fix used and how strong they are. The scale bar answers "how big is what I am looking at", in metres or feet.

Without a signal the background tiles stay blank. Corners, area, your own position and everything you saved keep working.

## Offline

Two ways to work with no signal:

- **Download an area.** In Settings, Offline maps: frame the ground you want, check the tile and size estimate, and download it. A download can pause and resume, and the app stops you before a frame would exceed the limits it keeps for one area.
- **Add a map file (.pmtiles).** A single file holds a whole region and is drawn entirely on the phone — it uses no data at all. Choose it from the map style picker.

Esri satellite imagery and OpenTopoMap terrain cannot be stored for offline use; their terms do not allow bulk downloading. An imported map file you licensed yourself is the way to have photographs without a signal.

## Privacy

Everything stays on this phone. There is no account, no tracking and no upload. Records are encrypted with a key held in this phone's hardware and locked to it — which also means the only way to move records to a new phone is a backup archive, because the key cannot travel.

- **Lock the app** asks for a fingerprint or the screen lock each time LandPoint opens.
- **Block screenshots** keeps the app out of the recent-apps preview and stops screenshots and screen recording.
- **Remove photo location when sharing** strips GPS tags from shared copies; the coordinates stamped onto the picture stay readable, and the stored photo is untouched.
- **Recently deleted** keeps deleted parcels for 30 days, then erases them for good.

Location permission is used only to read GPS fixes while you are measuring or looking at the map. Nothing is sent anywhere.

## Export

From Settings, Backup and export:

- **Full backup (with photos)** — one archive holding every location, boundary and photo. Keep a copy somewhere else: another phone, a memory card, your own cloud drive. A password makes it unreadable to anyone who finds the file — and there is no way to recover a forgotten one.
- **JSON and CSV** — data copies; the CSV opens in a spreadsheet.
- **PDF report** — one tidy page per parcel: map sketch, corners, area, owner and photos.
- **GPX and KML** — for GPS apps and Google Earth.

Restore and import bring records back, and ask what to do when an imported location already exists: skip it, replace it, or keep both as separate locations.
