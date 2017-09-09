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
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.spine.Animation;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.AnimationState.TrackEntry;
import com.esotericsoftware.spine.AnimationStateData;
import com.esotericsoftware.spine.Event;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonBounds;
import com.esotericsoftware.spine.SkeletonData;
import com.ray3k.pinatapunch.Core;
import com.ray3k.pinatapunch.Entity;
import com.ray3k.pinatapunch.states.GameState;

public class PlayerEntity extends Entity {
    private Skeleton skeleton;
    private AnimationState animationState;
    private SkeletonBounds skeletonBounds;
    private GameState gameState;
    private static final float HURT_DISTANCE = 50.0f;
    public static final float ATTACK_DISTANCE = 150.0f;
    private Array<Animation> attackAnimations;
    private Array<MoveType> moveQueue;
    private boolean keyIsDown;
    private EnemyEntity targetEnemy;
    private float attackTargetX;
    
    public static enum MoveType {
        LEFT, RIGHT
    }
    
    public PlayerEntity(GameState gameState) {
        super(gameState.getEntityManager(), gameState.getCore());
        this.gameState = gameState;
        SkeletonData skeletonData = getCore().getAssetManager().get(Core.DATA_PATH + "/spine/player.json", SkeletonData.class);
        skeleton = new Skeleton(skeletonData);
        AnimationStateData animationStateData = new AnimationStateData(skeletonData);
        animationStateData.setDefaultMix(0.0f);
        
        animationState = new AnimationState(animationStateData);
        animationState.setAnimation(0, "stance", false);
        animationState.addAnimation(0, "stand", true, 0);
        
        attackAnimations = new Array<Animation>();
        attackAnimations.add(skeletonData.findAnimation("back-flip-kick"));
        attackAnimations.add(skeletonData.findAnimation("cartwheel"));
        attackAnimations.add(skeletonData.findAnimation("flip-kick"));
        attackAnimations.add(skeletonData.findAnimation("kick-left"));
        attackAnimations.add(skeletonData.findAnimation("kick-right"));
        attackAnimations.add(skeletonData.findAnimation("punch-left"));
        attackAnimations.add(skeletonData.findAnimation("punch-right"));
        attackAnimations.add(skeletonData.findAnimation("slide-kick"));
        attackAnimations.add(skeletonData.findAnimation("split-punch"));
        attackAnimations.add(skeletonData.findAnimation("tackle"));
        attackAnimations.add(skeletonData.findAnimation("uppercut"));
        attackAnimations.add(skeletonData.findAnimation("windmill"));
        
        skeletonBounds = new SkeletonBounds();
        skeletonBounds.update(skeleton, true);
        
        animationState.addListener(new AnimationState.AnimationStateAdapter() {
            @Override
            public void start(AnimationState.TrackEntry entry) {
                if (entry.getAnimation().getName().equals("stand")) {
                    
                }
            }

            @Override
            public void event(AnimationState.TrackEntry entry, Event event) {
                if (event.getData().getName().equals("attack")) {
                    PlayerEntity.this.gameState.playPunchSound();
                    if (targetEnemy != null) {
                        targetEnemy.hit();
                        targetEnemy = null;
                    }
                } else if (event.getData().getName().equals("sound")) {
                    if (event.getString().equals("swoosh")) {
                        PlayerEntity.this.gameState.playSwooshSound();
                    }
                }
            }

            @Override
            public void complete(AnimationState.TrackEntry entry) {
                if (entry.getAnimation().getName().equals("stance")) {
                    PlayerEntity.this.gameState.setSpawnEnemies(true);
                }
            }
        });
        
        keyIsDown = false;
        moveQueue = new Array<MoveType>();
    }

    @Override
    public void create() {
    }

    @Override
    public void act(float delta) {
        skeleton.setPosition(getX(), getY());
        animationState.update(delta);
        skeleton.updateWorldTransform();
        animationState.apply(skeleton);
        skeletonBounds.update(skeleton, true);
        
        if (Gdx.input.isKeyPressed(Keys.ESCAPE)) {
            new GameOverTimerEntity(gameState, .5f);
        }
        
        if (!keyIsDown && !animationState.getCurrent(0).getAnimation().getName().equals("hit")) {
            if (Gdx.input.isKeyPressed(Keys.LEFT)) {
                moveQueue.add(MoveType.LEFT);
                keyIsDown = true;
            } else if (Gdx.input.isKeyPressed(Keys.RIGHT)) {
                moveQueue.add(MoveType.RIGHT);
                keyIsDown = true;
            }
        } else {
            if (!Gdx.input.isKeyPressed(Keys.LEFT) && !Gdx.input.isKeyPressed(Keys.RIGHT)) {
                keyIsDown = false;
            }
        }
        
        if (animationState.getCurrent(0).getAnimation().getName().equals("stand")) {
            EnemyEntity closestEnemy = null;
            float closestDistance = ATTACK_DISTANCE;

            for (Entity entity : gameState.getEntityManager().getEntities()) {
                if (entity instanceof EnemyEntity) {
                    EnemyEntity enemy = (EnemyEntity) entity;

                    float distance = Math.abs(getX() - enemy.getX());

                    if (distance < HURT_DISTANCE) {
                        TrackEntry trackEntry = enemy.getAnimationState().getCurrent(1);
                        if (trackEntry == null || !trackEntry.getAnimation().getName().equals("die")) {
                            hurt(enemy);
                            closestEnemy = null;
                            break;
                        }
                    } else if (distance < closestDistance && moveQueue.size > 0) {
                        if (enemy.getAnimationState().getCurrent(1) == null || !enemy.getAnimationState().getCurrent(1).getAnimation().getName().equals("die")) {
                            if (moveQueue.first() == MoveType.LEFT && enemy.getX() < getX()) {
                                closestEnemy = enemy;
                            } else if (moveQueue.first() == MoveType.RIGHT && enemy.getX() > getX()) {
                                closestEnemy = enemy;
                            }
                        }
                    }
                }
            }

            if (closestEnemy != null) {
                attack(closestEnemy);
            } else if (moveQueue.size > 0) {
                miss();
            }
        }
        
        if (targetEnemy != null) {
            if (targetEnemy.getX() < getX()) {
                moveTowardsPoint(targetEnemy.getX() + 50.0f, getY(), 300.0f, delta);
            } else {
                moveTowardsPoint(targetEnemy.getX() - 50.0f, getY(), 300.0f, delta);
            }
        }
        
        gameState.getGameCamera().position.x = getX();
    }
    
    @Override
    public void act_end(float delta) {
        
    }

    @Override
    public void draw(SpriteBatch spriteBatch, float delta) {
        getCore().getSkeletonRenderer().draw(spriteBatch, skeleton);
    }

    @Override
    public void destroy() {
        
    }

    @Override
    public void collision(Entity other) {
    }
    
    public SkeletonBounds getSkeletonBounds() {
        return skeletonBounds;
    }
    
    private void hurt(EnemyEntity enemy) {
        moveQueue.clear();
        
        skeleton.setFlipX(enemy.getX() < getX());
        
        animationState.setAnimation(0, "hit", false);
        gameState.playHitSound();
        new GameOverTimerEntity(gameState, 5.0f);
    }
    
    private void attack(EnemyEntity enemy) {
        skeleton.setFlipX(enemy.getX() < getX());
        
        animationState.setAnimation(0, attackAnimations.random(), false);
        animationState.addAnimation(0, "stand", false, 0.0f);
        moveQueue.removeIndex(0);
        targetEnemy = enemy;
    }
    
    private void miss() {
        gameState.playSwooshSound();
        skeleton.setFlipX(moveQueue.first() == MoveType.LEFT);
        if (skeleton.getFlipX()) {
            skeleton.findBone("sign-miss").setScaleX(-1);
        } else {
            skeleton.findBone("sign-miss").setScaleX(1);
        }
        
        moveQueue.clear();
        animationState.setAnimation(0, "miss", false);
        animationState.addAnimation(0, "stand", true, 0.0f);
    }

    public AnimationState getAnimationState() {
        return animationState;
    }
}
