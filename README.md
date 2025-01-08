## Just Like Rays

A mod that overhaul minecraft block light engine to behave more like real light (so for example, walls will cast shadows)

This mod is in early alpha. Core content is there but there be dragons! (and edge cases)


##### Info:

- Only compatible with worlds generated with this mod active (for save format reasons)


##### Todo/Roadmap:

- make lava "opaque" (will improve worldgen lag)
- implement face based GBV algorithm (will fix light leaks, non-full blocks handling and +)
- settings (radius, intensity, etc)

##### Maybe/Far future:

- par face light level data instead of per block (make 1 block large corridors look much better)
- tweaking the renderer to make use of the additionnal light level data
- RGB light
- paralelization of the GBV algorithm
