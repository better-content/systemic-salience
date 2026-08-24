# Systemic Salience

Systemic Salience is the deliberately thin Better Content integration layer for dynamic bodily state. Diet remains the owner of ordinary nutrient values; Brewin' and Chewin', Thirst Was Taken, Cold Sweat, Epic Fight, and vanilla remain the owners of their mechanics. This mod supplies the state and compatibility hooks needed for superlinear nutrition, sugar loading and debt, inverted-U alcohol, threshold interactions, and a unified Diet-screen readout.

Ordinary food groups display overlapping behavioral profiles rather than one-to-one categories: Fruits support Renewal and Endurance, Grains support Work and Endurance, Proteins support Impact and Robustness, and Vegetables support Robustness and Renewal. Sugar amplifies and spends the state already built; Alcohol shows its midpoint optimum and its independently rising impairment directly in the UI.

World owners can tune curve constants, half-lives, every nutrient threshold, and `item_id=load` alcohol profiles in `systemic_salience-server.toml`. Defaults remain the authored Better Content balance contract, so ordinary iteration does not require a mod rebuild.

It is not a global aspect registry and the other Better Content mods do not depend on it.

## Build

```sh
./gradlew verifyFull
```
