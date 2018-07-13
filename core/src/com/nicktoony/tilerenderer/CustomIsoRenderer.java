/*******************************************************************************
 * Copyright 2013 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.nicktoony.tilerenderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteCache;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.renderers.BatchTiledMapRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;
import java.util.List;

import static com.badlogic.gdx.graphics.g2d.Batch.*;

public class CustomIsoRenderer extends BatchTiledMapRenderer {

    static private final int MAX_PER_BATCH = 8000;

    private Matrix4 isoTransform;
    private Matrix4 invIsotransform;
    private Vector3 screenPos = new Vector3();

    private Vector2 topRight = new Vector2();
    private Vector2 bottomLeft = new Vector2();
    private Vector2 topLeft = new Vector2();
    private Vector2 bottomRight = new Vector2();

    private List<Cache> caches;

    private boolean blending = true;

    protected final Rectangle cacheBounds = new Rectangle();
    protected boolean cached = false;
    protected float overCache = 0.50f;
    protected float maxTileWidth, maxTileHeight;
    protected boolean canCacheMoreN, canCacheMoreE, canCacheMoreW, canCacheMoreS;
    private float extraWidth;
    private float extraHeight;

    private Cache currentCache = null;
    private int cacheCount = 0;
    private int totalCacheCount = 0;
    private int cacheIndex = 0;

    class Cache {
        SpriteCache cache;
        boolean render;

        public Cache() {
            cache = new SpriteCache(MAX_PER_BATCH, true);
            render = false;
        }

        public void dispose() {
            cache.dispose();
        }
    }

    public CustomIsoRenderer(TiledMap map, int size) {
        super(map);
        init();

        // Rows/caches
        caches = new ArrayList<Cache>();
        int count = 0;
        while (count < size) {
            caches.add(new Cache());
            count += MAX_PER_BATCH;
        }
    }

    private void init () {
        // create the isometric transform
        isoTransform = new Matrix4();
        isoTransform.idt();

        // isoTransform.translate(0, 32, 0);
        isoTransform.scale((float)(Math.sqrt(2.0) / 2.0), (float)(Math.sqrt(2.0) / 4.0), 1.0f);
        isoTransform.rotate(0.0f, 0.0f, 1.0f, -45);

        // ... and the inverse matrix
        invIsotransform = new Matrix4(isoTransform);
        invIsotransform.inv();
    }

    private Vector3 translateScreenToIso (Vector2 vec) {
        screenPos.set(vec.x, vec.y, 0);
        screenPos.mul(invIsotransform);

        return screenPos;
    }

    @Override
    public void renderTileLayer (TiledMapTileLayer layer) {
        final Color batchColor = batch.getColor();
        final float color = Color.toFloatBits(batchColor.r, batchColor.g, batchColor.b, batchColor.a * layer.getOpacity());

        float tileWidth = layer.getTileWidth() * unitScale;
        float tileHeight = layer.getTileHeight() * unitScale;

        final float layerOffsetX = layer.getRenderOffsetX() * unitScale;
        // offset in tiled is y down, so we flip it
        final float layerOffsetY = -layer.getRenderOffsetY() * unitScale;

        float halfTileWidth = tileWidth * 0.5f;
        float halfTileHeight = tileHeight * 0.5f;

        // setting up the screen points
        // COL1
        topRight.set(viewBounds.x + viewBounds.width - layerOffsetX, viewBounds.y - layerOffsetY);
        // COL2
        bottomLeft.set(viewBounds.x - layerOffsetX, viewBounds.y + viewBounds.height - layerOffsetY);
        // ROW1
        topLeft.set(viewBounds.x - layerOffsetX, viewBounds.y - layerOffsetY);
        // ROW2
        bottomRight.set(viewBounds.x + viewBounds.width - layerOffsetX, viewBounds.y + viewBounds.height - layerOffsetY);

        // transforming screen coordinates to iso coordinates
        int row1 = (int)(translateScreenToIso(topLeft).y / tileWidth) - 2;
        int row2 = (int)(translateScreenToIso(bottomRight).y / tileWidth) + 2;

        int col1 = (int)(translateScreenToIso(bottomLeft).x / tileWidth) - 2;
        int col2 = (int)(translateScreenToIso(topRight).x / tileWidth) + 2;

//        canCacheMoreN = row2 < layer.getWidth()*0.5f;
//        canCacheMoreE = col2 < layer.getHeight()*0.5f;
//        canCacheMoreW = col1 > 0;
//        canCacheMoreS = row1 > 0;

        for (int row = row2; row >= row1; row--) {
            for (int col = col1; col <= col2; col++) {
                float x = (col * halfTileWidth) + (row * halfTileWidth);
                float y = (row * halfTileHeight) - (col * halfTileHeight);

                final TiledMapTileLayer.Cell cell = layer.getCell(col, row);
                if (cell == null) continue;
                final TiledMapTile tile = cell.getTile();

                if (tile != null) {
                    final boolean flipX = cell.getFlipHorizontally();
                    final boolean flipY = cell.getFlipVertically();
                    final int rotations = cell.getRotation();

                    TextureRegion region = tile.getTextureRegion();

                    float x1 = x + tile.getOffsetX() * unitScale + layerOffsetX;
                    float y1 = y + tile.getOffsetY() * unitScale + layerOffsetY;
                    float x2 = x1 + region.getRegionWidth() * unitScale;
                    float y2 = y1 + region.getRegionHeight() * unitScale;

                    float u1 = region.getU();
                    float v1 = region.getV2();
                    float u2 = region.getU2();
                    float v2 = region.getV();

                    vertices[X1] = x1;
                    vertices[Y1] = y1;
                    vertices[C1] = color;
                    vertices[U1] = u1;
                    vertices[V1] = v1;

                    vertices[X2] = x1;
                    vertices[Y2] = y2;
                    vertices[C2] = color;
                    vertices[U2] = u1;
                    vertices[V2] = v2;

                    vertices[X3] = x2;
                    vertices[Y3] = y2;
                    vertices[C3] = color;
                    vertices[U3] = u2;
                    vertices[V3] = v2;

                    vertices[X4] = x2;
                    vertices[Y4] = y1;
                    vertices[C4] = color;
                    vertices[U4] = u2;
                    vertices[V4] = v1;

                    if (flipX) {
                        float temp = vertices[U1];
                        vertices[U1] = vertices[U3];
                        vertices[U3] = temp;
                        temp = vertices[U2];
                        vertices[U2] = vertices[U4];
                        vertices[U4] = temp;
                    }
                    if (flipY) {
                        float temp = vertices[V1];
                        vertices[V1] = vertices[V3];
                        vertices[V3] = temp;
                        temp = vertices[V2];
                        vertices[V2] = vertices[V4];
                        vertices[V4] = temp;
                    }
                    if (rotations != 0) {
                        switch (rotations) {
                            case Cell.ROTATE_90: {
                                float tempV = vertices[V1];
                                vertices[V1] = vertices[V2];
                                vertices[V2] = vertices[V3];
                                vertices[V3] = vertices[V4];
                                vertices[V4] = tempV;

                                float tempU = vertices[U1];
                                vertices[U1] = vertices[U2];
                                vertices[U2] = vertices[U3];
                                vertices[U3] = vertices[U4];
                                vertices[U4] = tempU;
                                break;
                            }
                            case Cell.ROTATE_180: {
                                float tempU = vertices[U1];
                                vertices[U1] = vertices[U3];
                                vertices[U3] = tempU;
                                tempU = vertices[U2];
                                vertices[U2] = vertices[U4];
                                vertices[U4] = tempU;
                                float tempV = vertices[V1];
                                vertices[V1] = vertices[V3];
                                vertices[V3] = tempV;
                                tempV = vertices[V2];
                                vertices[V2] = vertices[V4];
                                vertices[V4] = tempV;
                                break;
                            }
                            case Cell.ROTATE_270: {
                                float tempV = vertices[V1];
                                vertices[V1] = vertices[V4];
                                vertices[V4] = vertices[V3];
                                vertices[V3] = vertices[V2];
                                vertices[V2] = tempV;

                                float tempU = vertices[U1];
                                vertices[U1] = vertices[U4];
                                vertices[U4] = vertices[U3];
                                vertices[U3] = vertices[U2];
                                vertices[U2] = tempU;
                                break;
                            }
                        }
                    }
                    currentCache.cache.add(region.getTexture(), vertices, 0, NUM_VERTICES);
                    currentCache.render = true;
                    cacheCount += 1;
                    totalCacheCount += 1;

                    if (cacheCount >= MAX_PER_BATCH - 1) {
                        currentCache.cache.endCache();
                        cacheIndex += 1;
                        cacheCount = 0;
                        currentCache = caches.get(cacheIndex);
                        currentCache.cache.beginCache();
                    }
                }
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        for (Cache cache : caches) {
            cache.dispose();
        }
    }

    @Override
    public void render () {
        if (!cached) {
            cached = true;
            for (int i = 0; i < caches.size(); i ++) {
                Cache cache = caches.get(i);
                cache.cache.clear();
                cache.render = false;
            }

            System.out.println("Re-Render");
            extraWidth = viewBounds.width*0.5f * overCache;
            extraHeight = viewBounds.height*0.5f * overCache;

            cacheBounds.x = viewBounds.x;// - extraWidth;
            cacheBounds.y = viewBounds.y;// - extraHeight;
            cacheBounds.width = viewBounds.width;// + extraWidth * 1.5f;
            cacheBounds.height = viewBounds.height;// + extraHeight * 1.5f;

            extraWidth *= 1;
            extraHeight *= 1;

            cacheIndex = 0;
            cacheCount = 0;
            totalCacheCount = 0;
            currentCache = caches.get(cacheIndex);
            currentCache.cache.beginCache();
            for (MapLayer layer : map.getLayers()) {
                renderTileLayer((TiledMapTileLayer)layer);
            }
            currentCache.cache.endCache();
        }

        if (blending) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }

        for (Cache cache : caches) {
            if (cache.render) {
                cache.cache.begin();
                MapLayers mapLayers = map.getLayers();
                for (int i = 0, j = mapLayers.getCount(); i < j; i++) {
                    MapLayer layer = mapLayers.get(i);
                    if (layer.isVisible()) {
                        cache.cache.draw(i);
                        renderObjects(layer);
                    }
                }
                cache.cache.end();
            }
        }


        if (blending) Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override
    public void setView (OrthographicCamera camera) {
        for (Cache cache : caches) {
            cache.cache.setProjectionMatrix(camera.combined);
        }
        float width = camera.viewportWidth * camera.zoom + maxTileWidth * 2 * unitScale;
        float height = camera.viewportHeight * camera.zoom + maxTileHeight * 2 * unitScale;
        viewBounds.set(camera.position.x - width / 2, camera.position.y - height / 2, width, height);

        float areaDiff = Math.abs(1 - (viewBounds.area() / cacheBounds.area()));
        float xDiff = Math.abs(viewBounds.x -  cacheBounds.x);
        float yDiff = Math.abs(viewBounds.y -  cacheBounds.y);
        if (areaDiff > 0.2f || xDiff >= extraWidth || yDiff >= extraHeight) {
            cached = false;
        }
    }

    @Override
    public void setView (Matrix4 projection, float x, float y, float width, float height) {
        for (Cache cache : caches) {
            cache.cache.setProjectionMatrix(projection);
        }
        x -= maxTileWidth * unitScale;
        y -= maxTileHeight * unitScale;
        width += maxTileWidth * 2 * unitScale;
        height += maxTileHeight * 2 * unitScale;
        viewBounds.set(x, y, width, height);

        float areaDiff = Math.abs(1 - (viewBounds.area() / cacheBounds.area()));
        float xDiff = Math.abs(viewBounds.x -  cacheBounds.x);
        float yDiff = Math.abs(viewBounds.y -  cacheBounds.y);
        if (areaDiff > 0.2f || xDiff >= extraWidth || yDiff >= extraHeight) {
            cached = false;
        }
    }

}
