# Systemic Salience

Systemic Salience is the deliberately thin Better Content integration layer for dynamic bodily state. Diet remains the owner of ordinary nutrient values; Brewin' and Chewin', Thirst Was Taken, Cold Sweat, Epic Fight, and vanilla remain the owners of their mechanics. This mod supplies the state and compatibility hooks needed for superlinear nutrition, sugar loading and debt, inverted-U alcohol, threshold interactions, and a unified Diet-screen readout.

Each ordinary food group owns one behavioral identity: Proteins are Impact, Grains are Work,
Fruits are Mobility, Fats are Endurance, Vegetables are Robustness, and Dairy is Renewal.
Sugar expresses Tempo by amplifying and spending the state already built; Alcohol expresses
Control through its useful midpoint and independently rising impairment.

Opening Diet now shows one integrated, responsive screen instead of a detached side panel. The
screen keeps Diet's ordinary group values and navigation while presenting Systemic Salience's
thresholds, readiness identities, sugar/debt, and alcohol load together. Values below Supported
are labeled **Undernourished**. Labels, icons, threshold ticks, and bar positions duplicate color
meaning, and the layout fits the pack default GUI scale without hiding an ordinary group.

The eight identities use short physical-foley gestures with a quiet one-second room decay.
Nutrition, RPG allocation, and Matter use related contextual renders rather than identical
files. Sugar crash deliberately receives a broken Tempo gesture. The reproducible renderer,
checked-in CC0 excerpts, exact hashes, and source provenance live under `audio/` and `tools/`.

World owners can tune curve constants, half-lives, every nutrient threshold, and `item_id=load` alcohol profiles in `systemic_salience-server.toml`. Defaults remain the authored Better Content balance contract, so ordinary iteration does not require a mod rebuild.

It is not a global aspect registry and the other Better Content mods do not depend on it.

## Build

```sh
./gradlew verifyFull
```
