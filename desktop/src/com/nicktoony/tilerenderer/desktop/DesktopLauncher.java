package com.nicktoony.tilerenderer.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.nicktoony.tilerenderer.MyGame;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.vSyncEnabled = false;
		config.foregroundFPS = 9999;
		config.title = "LibGDX Tile Renderer";
		config.width = 1280;
		config.height = 800;
		new LwjglApplication(new MyGame(), config);
	}
}
