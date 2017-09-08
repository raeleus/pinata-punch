/*
 * The MIT License
 *
 * Copyright 2017 Raymond Buckley.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ray3k.pinatapunch.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.ray3k.pinatapunch.Core;
import com.ray3k.pinatapunch.Entity;
import com.ray3k.pinatapunch.states.GameState;

public class BackgroundEntity extends Entity {
    private TiledDrawable tiledDrawable;
    private float width;
    private float height;
    private GameState gameState;
    
    public BackgroundEntity(GameState gameState) {
        super(gameState.getEntityManager(), gameState.getCore());
        this.gameState = gameState;
        setDepth(100);
    }
    
    @Override
    public void create() {
        TextureAtlas atlas = getCore().getAssetManager().get(Core.DATA_PATH + "/spine/pinata-punch.atlas", TextureAtlas.class);
        tiledDrawable = new TiledDrawable(atlas.findRegion("platform"));
    }

    @Override
    public void act(float delta) {
        if (getY() + height < gameState.getGameCamera().position.y - Gdx.graphics.getHeight() / 2.0f) {
            dispose();
        }
    }

    @Override
    public void act_end(float delta) {
    }

    @Override
    public void draw(SpriteBatch spriteBatch, float delta) {
        tiledDrawable.draw(spriteBatch, getX(), getY(), width, height);
    }

    @Override
    public void destroy() {
    }

    @Override
    public void collision(Entity other) {
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }
    
}
