# Requests for Backport Core

What Game Drops Backport needs from Backport Core (`backportcore` 1.4.0) or from another family mod through Core.
Nothing here is built in Core's repo by this mod; each request says what we do meanwhile.

## Stream 1 (babies)

1. **Baby models of other family mods' mobs.** 26.1 redrew every baby but the nautilus and the ghastling, each with
   a new model, not only a texture, so none can go into Core's modern pack: each owner needs its own baby renderer
   (26.3's `Baby*Model` classes and `*_baby.png` textures). Owners: Village & Pillage (fox, cat, panda, its
   villagers' biome overlays), Buzzy Bees (bee), Update Aquatic (dolphin, turtle), Nether Update (strider, hoglin,
   zoglin, piglin, zombified piglin), Caves & Cliffs (goat, axolotl, glow squid), Trails & Tales (camel, sniffer),
   Tricky Trials (armadillo). Meanwhile: this mod draws only vanilla 1.12.2's babies.
2. **Age lock for babies that are not `EntityAgeable`.** The golden dandelion works on every `EntityAgeable` baby,
   any mod's. 26.1 also locks tadpoles (Wild Update) and gives snifflets a 48000-tick start age (Trails & Tales).
   Proposal: Core hosts a small interface (`isBaby`, `getAge`/`setAge`, `startAge`) that owners implement, and this
   mod's `babies.AgeLock` uses it. Meanwhile: tadpoles are not lockable; a locked snifflet restarts at -24000.
3. **Piglins and the golden dandelion** (Nether Update): 26.3 lists it in `piglin_loved`; piglins inspect it and take
   it. Needs a way for another mod to add to Nether Update's piglin-loved list. Meanwhile: piglins ignore it.
4. **Name tags out of the master librarian's trades** (Village & Pillage) and **out of ancient city chests** (Wild
   Update): 26.1 removed both. Owners' tables. Meanwhile: unchanged. (The woodland mansion, 1.12.2's own, is done here.)
