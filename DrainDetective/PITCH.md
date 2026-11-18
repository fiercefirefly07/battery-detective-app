# 🎙️ Drain Detective — 2-Minute Demo Script & Judge Q&A

---

## ⏱️ The 2-Minute Pitch Script

### [0:00 - 0:25] The Hook
*"Judges, every battery manager in existence tells you the exact same thing: a flat, boring list of which apps you opened today. But that doesn't answer the real mystery of battery life.*

*Think about this: Why did your phone lose 14% while you were sitting in a movie theater? Why did it drop 8% while in your backpack?*

*Here is the core premise of our app:*  
**'Other apps tell you which app used your battery. Drain Detective tells you what situation your phone was in.'**"

---

### [0:25 - 0:55] Feature 1: Context Fingerprint
*(Holding device or emulator)*
*"Drain Detective introduces the **Context Fingerprint**. Using low-power episodic sampling of the proximity sensor, ambient light, and accelerometer motion variance, we automatically classify your phone's physical situation into 6 forensic states: **On Desk**, **In Pocket/Bag**, **Moving in Transit**, **In Active Use**, **Charging**, or **Standby**.*

*Right here on our main screen, you can see our **Context Drain Bar Chart**:*
- *In Use: 11.8%/hr*
- *In Pocket: 7.2%/hr*
- *Moving: 6.4%/hr*
- *On Desk: 2.1%/hr*

*And if your phone gets trapped in a pocket or bag with rising temperature and high drain, our **'Pocket Cooking'** alert warns you before thermal throttling damages your hardware!"*

---

### [0:55 - 1:25] Feature 2: Ghost Race
*(Swipes to the Ghost tab)*
*"Next up: the **Ghost Race**.*

*Instead of comparing today against an arbitrary factory number, Drain Detective finds your personal **Ghost Day** — your all-time most battery-efficient day.*

*On this custom Compose Canvas, we overlay today's live battery curve directly over your ghost curve, aligned by time of day. Right now, our banner says: **'You're 6% ahead of your ghost! 🏎️'**.*

*And if you're falling behind, Drain Detective doesn't just give you a number — it tells you the exact context culprit: **'Primary reason: High active screen-on time (55% of snapshots)'** or **'Elevated cellular handovers during transit'**."*

---

### [1:25 - 1:45] Feature 3: Black Box (10-Minute Flight Recorder)
*(Swipes to the Incidents tab)*
*"When a sudden acute battery collapse occurs (>= 3% in a single interval), Drain Detective triggers the **Black Box** — a 10-minute high-resolution flight recorder taking samples every 20 seconds.*

*Judges, watch this replay scrubber: we can drag through all 30 points of the incident and watch the battery level, temperature, and physical context unfold step-by-step to understand the exact anatomy of the failure."*

---

### [1:45 - 2:00] Summary & Ethics
*"Best of all, Drain Detective requires zero API keys, runs 100% offline on-device, and includes an honest **'Detective on Duty'** mode that respects Android's background sensor rules.*

*You can tap **'Seed Full Demo Data'** in Settings right now to see all three features live with 3 days of realistic data. Thank you!"*

---

## ❓ Likely Judge Questions & Answers

### Q1: How does the Context Fingerprint classify situations without draining battery itself?
**Answer:**
> *"We use low-power episodic sampling. Rather than running sensors continuously in the background, `ContextSensor` registers listeners with `SENSOR_DELAY_NORMAL`, records for ~4 seconds during each reading, calculates accelerometer variance, and immediately unregisters all listeners. In a 24-hour cycle, the sensors are active for less than 6 minutes total, consuming less than 0.02% battery."*

### Q2: How do you handle Android's restrictions on background sensors?
**Answer:**
> *"Android prevents standard background workers from reading proximity and light sensors. We handle this with complete engineering honesty: when the app runs in the standard background via WorkManager, readings are classified as `UNKNOWN` rather than fabricating fake data. To enable continuous situational awareness, we offer an opt-in **'Detective on Duty'** foreground service with a persistent notification that grants OS permission to sample every 15 minutes."*

### Q3: How is the Ghost Day chosen, and what if the user just started using the app?
**Answer:**
> *"The Ghost Day is selected by scanning past calendar days and finding the day with at least 8 readings that had the lowest average discharge drop rate. If there are fewer than 2 days of data, the app displays a friendly 'Ghost Benchmark Calibrating' state. Judges can immediately see the Ghost Race in action by tapping 'Seed Full Demo Data' in Settings."*

### Q4: What triggers the Black Box flight recorder?
**Answer:**
> *"The Black Box monitors consecutive readings for acute drain spikes: either a drop >= 3% within a single 15-minute interval or an annualized drain rate >= 10%/hr. When triggered, it initiates a 10-minute capture loop sampling every 20 seconds, persisting an `Incident` entity to Room SQLite with a verdict and full telemetry for interactive scrubbing."*

### Q5: How does the 'Pocket Cooking' alert avoid spamming notifications?
**Answer:**
> *"The `PocketCookingNotifier` enforces three strict guards: (1) the context must be `IN_POCKET_OR_BAG`, (2) the battery temperature must exceed 37.5°C, and (3) a 30-minute cooldown timer is enforced in SharedPreferences. It also checks for the `POST_NOTIFICATIONS` runtime permission on Android 13+."*
