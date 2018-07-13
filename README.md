# LibGDX Tile Renderer

This is an experiment to push the limits of LibGDX's tile rendering capabilities. The goal is to exploit the GPU as much as possible to render extremely large tiled worlds. We do this by storing tile vertices/geometry on the GPU itself, reducing costly access times. The result allows us to render an entire 512*512 world without a significant performance drop (and a solid 60 FPS in browsers using WebGL!).

![Demo](https://github.com/NickToony/libgdx-tile-renderer/raw/master/demo.gif)

Should you use this? *Probably not* - it's really just an experiment and not polished. LibGDX default renderers can handle up to ~80\*80 ortho tiles pretty well (using OrthoCachedTiledMapRenderer), though you'll struggle to achieve that with the isometric renderer. There's really no value in rendering an entire map like this in most games.

## Classes
There are 3 implementations included, and they're each based on an existing tile renderer:

 1. **CustomOrthoRenderer** - Improves OrthoCachedTiledMapRenderer by removing the vertices limit (using multiple GPU caches)
 2. **CustomIsoRenderer** - Adds SpriteCache capabilities to IsometricTiledMapRenderer, also removing any vertices limit
 3. **CustomIsoRendererYSort** - Same as CustomIsoRenderer, except each row is rendered individually to allow depth sorting (this one is slowest)

## Comparison
For comparison, similar tile setups in Godot and GameMaker yielded the following results:

iMac 2015 (OpenGL) - 128*128 isometric tiles:
- Godot: 50 FPS
- GameMaker: 100 FPS

iMac 2015 (OpenGL) 512*512 isometric tiles:
- LibGDX implementation: 470-500 FPS
