package com.mc2.block.Shader;

import com.mc2.Utils.Tile;

public interface Shader {
    void onSurfaceChanged(int width, int height);
    void prepareDraw();
    void draw(int tileSchemeIdx, Tile tile, int frameIdx);
    void endDraw(int fIdx);
    void recycle(Tile tile);
}
