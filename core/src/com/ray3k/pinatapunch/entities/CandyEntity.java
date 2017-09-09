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

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.ray3k.pinatapunch.Core;
import com.ray3k.pinatapunch.Entity;
import com.ray3k.pinatapunch.states.GameState;

public class CandyEntity extends Entity {
    private GameState gameState;
    private float rotationSpeed;
    
    public CandyEntity(GameState gameState) {
        super(gameState.getEntityManager(), gameState.getCore());
        this.gameState = gameState;
        
        Array<String> names = getCore().getImagePacks().get(Core.DATA_PATH + "/candy");
        setTextureRegion(getCore().getAtlas().findRegion(names.random()));
        
        setOffsetX(getTextureRegion().getRegionWidth() / 2.0f);
        setOffsetY(getTextureRegion().getRegionHeight() / 2.0f);
        
        setGravity(700.0f, 270.0f);
        rotationSpeed = MathUtils.random(-300.0f, 300.0f);
    }

    @Override
    public void create() {
    }

    @Override
    public void act(float delta) {
        addRotation(rotationSpeed * delta);
        
        if (getY() < -200.0f) {
            dispose();
        }
    }

    @Override
    public void act_end(float delta) {
    }

    @Override
    public void draw(SpriteBatch spriteBatch, float delta) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void collision(Entity other) {
    }

}
