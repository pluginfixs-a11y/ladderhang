# LadderHang

Server-side Fabric mod for Minecraft **26.2**. Ladders hang like vines: one anchor on a wall, everything
below it hangs in the air. Right-click a ladder with a ladder in hand and the chain grows one block
downwards — a rope you lower into a ravine. Vanilla clients need nothing installed.

## Rules

| situation | vanilla 26.2 | with LadderHang |
|---|---|---|
| solid block behind the ladder | stays | stays |
| no block behind, ladder above with the same facing | breaks | **stays** |
| no block behind, nothing above | breaks | breaks |
| anchor removed (wall or top ladder) | that ladder breaks | **the whole hanging chain falls** |

The support check mirrors `VineBlock`: `wall behind || same ladder above`. A ladder above must face the
same way, otherwise the column would look broken.

Breaking the wall behind any ladder in the middle of a chain drops that ladder and everything hanging
under it; ladders that still have their own wall stay.

## Lowering the rope

Right-click any ladder of the chain with a ladder in hand:

* the mod walks to the bottom of the chain and places the new ladder one block below it;
* the ladder is taken from the hand (creative does not consume it);
* if the spot below is not replaceable (stone, ore, a chest) nothing happens and vanilla placement
  gets the click instead;
* water at the target makes the new ladder waterlogged.

No length limit: a chain can go from the surface to bedrock, one ladder per click.

## What it does not touch

* climbing — `#minecraft:climbable`, independent of support;
* shape and collision — ladders keep the vanilla plate shape, so hanging ones render and collide normally;
* placement against a wall, waterlogging, rotation and mirroring.

## Client side

The mod is `environment: "server"`. A vanilla client does not know the new support rule, so it will not
*predict* a hanging ladder when you place one — the block arrives with the server update a moment later.
No desync, no client mod.

## Self-test

`-Dladderhang.selftest=1` builds ladders 2000 blocks from spawn on the first server tick and logs:

```
SELFTEST wall-anchor:      canSurvive=true   after wall removal air=true
SELFTEST floating-alone:   canSurvive=false
SELFTEST hang-from-above:  lower=true upper=false
SELFTEST cascade:          5/5 -> 0
SELFTEST wall-keeps:       lower ladder stays=true
SELFTEST rope:             +1 ladder, hand 3->2, 2->1, empty hand = PASS, chain=6
```

The rope test goes through the real `UseBlockCallback` chain with a `FakePlayer`.

## Install

Fabric Loader ≥ 0.20.1 + Fabric API on the server, jar into `mods/`.

## Build

```
./gradlew build
```

## License

MIT.
