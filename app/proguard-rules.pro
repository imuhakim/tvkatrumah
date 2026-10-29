# Components are kept through the manifest; nothing is reached by reflection. Shrink and optimise,
# but keep class and method names: readable stack traces matter more than the few KB renaming saves
# in a sideloaded app.
-dontobfuscate
