# Requests for Backport Core

What Game Drops Backport needs from Backport Core (`backportcore` 1.4.0) or from another family mod through Core.
Nothing here is built in Core's repo by this mod; each request says what we do meanwhile.

## Su-1. Jukebox comparator signal for discs added after 1.12.2 (stream 2)

1.12.2's `BlockJukebox.getComparatorInputOverride` counts the signal from the disc's item id, right for vanilla's
twelve discs only; Bounce gives a number far above 15. Tricky Trials (Decor-1) and Caves & Cliffs (Mu-1) ask for the
same. **Want:** Core's jukebox patch answering with the disc's own signal when the record has one, read by name:
`int comparatorSignal()` on the `ItemRecord` subclass (`sulfur.ItemSulfurDisc` returns 26.3's 8).
**Meanwhile:** the disc plays, names and stacks as vanilla's; only the comparator signal is 1.12.2's.
