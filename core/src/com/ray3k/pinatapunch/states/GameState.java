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
package com.ray3k.pinatapunch.states;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.ray3k.pinatapunch.Core;
import com.ray3k.pinatapunch.EntityManager;
import com.ray3k.pinatapunch.InputManager;
import com.ray3k.pinatapunch.State;
import com.ray3k.pinatapunch.entities.BackgroundEntity;
import com.ray3k.pinatapunch.entities.EnemyEntity;
import com.ray3k.pinatapunch.entities.PlayerEntity;

public class GameState extends State {
    private int score;
    private static int highscore = 0;
    private OrthographicCamera gameCamera;
    private Viewport gameViewport;
    private OrthographicCamera uiCamera;
    private Viewport uiViewport;
    private InputManager inputManager;
    private Skin skin;
    private Stage stage;
    private Table table;
    private Label scoreLabel;
    private EntityManager entityManager;
    private PlayerEntity player;
    private float spawnTimer;
    private float spawnDelay;
    private float spawnDelayChange;
    private static final float SPAWN_DELAY_MINIMUM = .5f;
    private boolean spawnEnemies;
    private float hatTimer;
    private float spikeTimer;
    private int worldEdgeLeft;
    private int worldEdgeRight;
    
    public static enum Team {
        PLAYER, ENEMY;
    }
    
    public GameState(Core core) {
        super(core);
    }
    
    @Override
    public void start() {
        spawnDelay = 2.0f;
        spawnTimer = 0.0f;
        spawnDelayChange = .01f;
        
        hatTimer = 15.0f;
        
        spikeTimer = 30.0f;
        
        score = 0;
        
        inputManager = new InputManager(); 
        
        uiCamera = new OrthographicCamera();
        uiViewport = new ScreenViewport(uiCamera);
        uiViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        uiViewport.apply();
        
        uiCamera.position.set(uiCamera.viewportWidth / 2, uiCamera.viewportHeight / 2, 0);
        
        gameCamera = new OrthographicCamera();
        gameViewport = new ScreenViewport(gameCamera);
        gameViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        gameViewport.apply();
        
        gameCamera.position.set(gameCamera.viewportWidth / 2, gameCamera.viewportHeight / 2, 0);
        
        skin = getCore().getAssetManager().get(Core.DATA_PATH + "/ui/pinata-punch.json", Skin.class);
        stage = new Stage(new ScreenViewport());
        
        InputMultiplexer inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(inputManager);
        inputMultiplexer.addProcessor(stage);
        Gdx.input.setInputProcessor(inputMultiplexer);
        
        table = new Table();
        table.setFillParent(true);
        stage.addActor(table);
        
        entityManager = new EntityManager();
        
        createStageElements();
        
        player = new PlayerEntity(this);
        player.setPosition(Gdx.graphics.getWidth() / 2.0f, 250);
        
        worldEdgeLeft = 0;
        worldEdgeRight = MathUtils.ceil(Gdx.graphics.getWidth() / 76.0f) * 76;
        BackgroundEntity bg = new BackgroundEntity(this);
        bg.setPosition(0.0f, 0.0f);
        bg.setWidth(worldEdgeRight);
        bg.setHeight(329.0f);
        
        spawnEnemies = false;
    }
    
    public void generateEnemy() {
        Array<EnemyEntity.Type> enemyTypes = new Array<EnemyEntity.Type>();
        enemyTypes.add(EnemyEntity.Type.DONKEY);
        if (hatTimer <= 0) {
            enemyTypes.add(EnemyEntity.Type.HAT);
        }
        if (spikeTimer <= 0) {
            enemyTypes.add(EnemyEntity.Type.SPIKE_BALL);
        }
        
        EnemyEntity enemy = new EnemyEntity(this, enemyTypes.random(), 200.0f);
        if (MathUtils.randomBoolean()) {
            enemy.setPosition(gameCamera.position.x + Gdx.graphics.getWidth() / 2.0f, 250);
        } else {
            enemy.setPosition(gameCamera.position.x - Gdx.graphics.getWidth() / 2.0f, 250);
            enemy.getSkeleton().setFlipX(true);
        }
        enemy.setMode(EnemyEntity.Mode.NONE);
        spawnDelay -= spawnDelayChange;
        if (spawnDelay < SPAWN_DELAY_MINIMUM) {
            spawnDelay = SPAWN_DELAY_MINIMUM;
        }
    }
    
    private void createStageElements() {
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);
        
        scoreLabel = new Label("0", skin, "score");
        root.add(scoreLabel).expandY().padTop(25.0f).top();
    }
    
    @Override
    public void draw(SpriteBatch spriteBatch, float delta) {
        Gdx.gl.glClearColor(28 / 256f, 32 / 256f, 86 /256f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        gameCamera.update();
        spriteBatch.setProjectionMatrix(gameCamera.combined);
        spriteBatch.begin();
        entityManager.draw(spriteBatch, delta);
        spriteBatch.end();
        
        stage.draw();
    }

    @Override
    public void act(float delta) {
        entityManager.act(delta);
        
        stage.act(delta);
        
        hatTimer -= delta;
        if (hatTimer <= 0) {
            hatTimer = 0;
        }
        
        spikeTimer -= delta;
        if (spikeTimer <= 0) {
            spikeTimer = 0;
        }
        
        if (spawnEnemies) {
            spawnTimer -= delta;
            if (spawnTimer <= 0) {
                generateEnemy();
                spawnTimer = spawnDelay;
            }
        }
        
        if (gameCamera.position.x + Gdx.graphics.getWidth() / 2.0f > worldEdgeRight) {
            BackgroundEntity bg = new BackgroundEntity(this);
            bg.setPosition(worldEdgeRight, 0.0f);
            bg.setWidth(MathUtils.ceil(Gdx.graphics.getWidth() / 76.0f) * 76);
            bg.setHeight(329.0f);
            worldEdgeRight += bg.getWidth();
        }
        
        if (gameCamera.position.x - Gdx.graphics.getWidth() / 2.0f < worldEdgeLeft) {
            BackgroundEntity bg = new BackgroundEntity(this);
            bg.setWidth(MathUtils.ceil(Gdx.graphics.getWidth() / 76.0f) * 76);
            bg.setPosition(worldEdgeLeft - bg.getWidth(), 0.0f);
            bg.setHeight(329.0f);
            worldEdgeLeft -= bg.getWidth();
        }
    }

    @Override
    public void dispose() {
    }

    @Override
    public void stop() {
        stage.dispose();
    }
    
    @Override
    public void resize(int width, int height) {
        gameViewport.update(width, height);
        gameCamera.position.set(width / 2, height / 2.0f, 0.0f);
        
        uiViewport.update(width, height);
        uiCamera.position.set(uiCamera.viewportWidth / 2, uiCamera.viewportHeight / 2, 0);
        stage.getViewport().update(width, height, true);
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public InputManager getInputManager() {
        return inputManager;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
        scoreLabel.setText(Integer.toString(score));
        if (score > highscore) {
            highscore = score;
        }
    }
    
    public void addScore(int score) {
        this.score += score;
        scoreLabel.setText(Integer.toString(this.score));
        if (this.score > highscore) {
            highscore = this.score;
        }
    }
    
    public void playHitSound() {
        getCore().getAssetManager().get(Core.DATA_PATH + "/sfx/hit.wav", Sound.class).play(.5f);
    }
    
    public void playPunchSound() {
        getCore().getAssetManager().get(Core.DATA_PATH + "/sfx/punch.wav", Sound.class).play(.5f);
    }
    
    public void playSwooshSound() {
        getCore().getAssetManager().get(Core.DATA_PATH + "/sfx/swoosh.wav", Sound.class).play(.5f);
    }

    public OrthographicCamera getGameCamera() {
        return gameCamera;
    }

    public void setGameCamera(OrthographicCamera gameCamera) {
        this.gameCamera = gameCamera;
    }
    
    public Skin getSkin() {
        return skin;
    }

    public Stage getStage() {
        return stage;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public boolean isSpawnEnemies() {
        return spawnEnemies;
    }

    public void setSpawnEnemies(boolean spawnEnemies) {
        this.spawnEnemies = spawnEnemies;
    }
}