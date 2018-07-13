package com.nicktoony.tilerenderer;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;

public class MyGame extends ApplicationAdapter {
	private OrthographicCamera camera;
	private Texture tiles;
	private BitmapFont font;
	private SpriteBatch batch;
	private TiledMap map;
	private TiledMapRenderer renderer;

	private static final int MAP_WIDTH = 512;
	private static final int MAP_HEIGHT = 512;

	@Override
	public void create () {
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();

		camera = new OrthographicCamera();
		camera.setToOrtho(false, (w / h) * Gdx.graphics.getHeight(), Gdx.graphics.getHeight());
		camera.update();

		font = new BitmapFont();
		batch = new SpriteBatch();

		tiles = new Texture(Gdx.files.internal("block.png"));
		TextureRegion[][] splitTiles = TextureRegion.split(tiles, tiles.getWidth(), tiles.getHeight());
		map = new TiledMap();
		MapLayers layers = map.getLayers();
		int count = 0;
//		for (int l = 0; l < 20; l++) {
			TiledMapTileLayer layer = new TiledMapTileLayer(MAP_WIDTH, MAP_HEIGHT, 180, 125);
			for (int x = 0; x < MAP_WIDTH; x++) {
				for (int y = 0; y < MAP_HEIGHT; y++) {
					int ty = (int)(Math.random() * splitTiles.length);
					int tx = (int)(Math.random() * splitTiles[ty].length);
					TiledMapTileLayer.Cell cell = new TiledMapTileLayer.Cell();
					cell.setTile(new StaticTiledMapTile(splitTiles[ty][tx]));
					layer.setCell(x, y, cell);
					count ++;
				}
			}
			layers.add(layer);
			System.out.println("Count: " + count);
//		}

//		renderer = new CustomOrthoRenderer(map, 1, MAP_WIDTH * MAP_HEIGHT);
//		renderer = new CustomIsoRendererYSort(map);
		renderer = new CustomIsoRenderer(map, MAP_WIDTH * MAP_HEIGHT);
//		renderer = new OrthogonalTiledMapRenderer(map);
	}

	private void updateCamera() {
		boolean left = Gdx.input.isKeyPressed(Input.Keys.A);
		boolean right = Gdx.input.isKeyPressed(Input.Keys.D);
		boolean up = Gdx.input.isKeyPressed(Input.Keys.W);
		boolean down = Gdx.input.isKeyPressed(Input.Keys.S);
		boolean zoomIn = Gdx.input.isKeyPressed(Input.Keys.Q);
		boolean zoomOut = Gdx.input.isKeyPressed(Input.Keys.E);

		camera.position.x += -(left ? 10 : 0) + (right ? 10 : 0);
		camera.position.y += (up ? 10 : 0) - (down ? 10 : 0);
		camera.zoom = Math.max(0.7f, camera.zoom -(zoomOut ? 0.1f : 0) + (zoomIn ? 0.1f : 0));
		camera.update();
	}

	@Override
	public void render () {
		Gdx.gl.glClearColor(100f / 255f, 100f / 255f, 250f / 255f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		updateCamera();
		renderer.setView(camera);
		renderer.render();
		batch.begin();
		font.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 10, 20);
		batch.end();
	}
	
	@Override
	public void dispose () {
		batch.dispose();
		tiles.dispose();
	}
}
