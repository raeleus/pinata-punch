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
import com.badlogic.gdx.math.Vector2;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.AnimationStateData;
import com.esotericsoftware.spine.Event;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonBounds;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.Skin;
import com.esotericsoftware.spine.attachments.PointAttachment;
import com.ray3k.pinatapunch.Core;
import com.ray3k.pinatapunch.Entity;
import com.ray3k.pinatapunch.states.GameState;

public class EnemyEntity extends Entity {
    private Skeleton skeleton;
    private AnimationState animationState;
    private SkeletonBounds skeletonBounds;
    private GameState gameState;
    private Mode mode;
    private Type type;
    private static final float RECOVERY_REST_TIME = .75f;
    private static final float MINIMUM_SPACING = 100.0f;
    private int points;
    private float attackMoveSpeed;
    private int hits;
    private float recoveryTimer;
    private float recoveryMoveSpeed;
    private float recoveryTargetX;
    
    public static enum Mode {
        RIGHT, LEFT, NONE
    }
    
    public static enum Type {
        DONKEY, SPIKE_BALL, HAT
    }
    
    public EnemyEntity(GameState gameState, Type type, float attackMoveSpeed) {
        super(gameState.getEntityManager(), gameState.getCore());
        this.gameState = gameState;
        this.attackMoveSpeed = attackMoveSpeed;
        
        SkeletonData skeletonData;
        this.type = type;
        if (type == Type.DONKEY) {
            skeletonData = getCore().getAssetManager().get(Core.DATA_PATH + "/spine/donkey.json", SkeletonData.class);
            recoveryMoveSpeed = 700.0f;
            hits = 1;
            points = 10;
        } else if (type == Type.HAT) {
            skeletonData = getCore().getAssetManager().get(Core.DATA_PATH + "/spine/hat.json", SkeletonData.class);
            recoveryMoveSpeed = 700.0f;
            hits = 2;
            points = 30;
        } else {
            skeletonData = getCore().getAssetManager().get(Core.DATA_PATH + "/spine/spike.json", SkeletonData.class);
            recoveryMoveSpeed = 2000.0f;
            hits = 3;
            points = 60;
        }
        
        skeleton = new Skeleton(skeletonData);
        AnimationStateData animationStateData = new AnimationStateData(skeletonData);
        animationStateData.setDefaultMix(.25f);
        
        animationState = new AnimationState(animationStateData);
        animationState.setAnimation(0, "walk", true);
        
        skeletonBounds = new SkeletonBounds();
        skeletonBounds.update(skeleton, true);
        
        animationState.addListener(new AnimationState.AnimationStateAdapter() {
            @Override
            public void event(AnimationState.TrackEntry entry, Event event) {
                if (event.getData().getName().equals("death")) {
                    ConfettiEntity confet = new ConfettiEntity(EnemyEntity.this.gameState);
                    PointAttachment confetPoint = (PointAttachment) skeleton.getAttachment("confetti", "confetti");
                    Vector2 location = confetPoint.computeWorldPosition(skeleton.findBone("body"), new Vector2());
                    confet.setPosition(location.x, location.y);
                    
                    for (int i = 0; i < 5; i++) {
                        CandyEntity candy = new CandyEntity(EnemyEntity.this.gameState);
                        candy.setPosition(location.x, location.y);
                        candy.setMotion(MathUtils.random(700.0f), MathUtils.random(45.0f, 135.0f));
                    }
                    
                    EnemyEntity.this.gameState.addScore(points);
                }
            }

            @Override
            public void complete(AnimationState.TrackEntry entry) {
                if (entry.getAnimation().getName().equals("die")) {
                    EnemyEntity.this.dispose();
                }
            }
        });
        
        recoveryTimer = -1;
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
        
        for (Entity entity : gameState.getEntityManager().getEntities()) {
            if (entity instanceof EnemyEntity && !entity.equals(this)) {
                EnemyEntity enemy = (EnemyEntity) entity;
                if (enemy.getAnimationState().getCurrent(1) == null || !enemy.getAnimationState().getCurrent(1).getAnimation().equals("die")) {
                    if (skeleton.getFlipX()) {
                        if (getX() < enemy.getX() && getX() > enemy.getX() - MINIMUM_SPACING) {
                            setX(enemy.getX() - MINIMUM_SPACING);
                        }
                    } else {
                        if (getX() > enemy.getX() && getX() < enemy.getX() + MINIMUM_SPACING) {
                            setX(enemy.getX() + MINIMUM_SPACING);
                        }
                    }
                }
            }
        }
        
        skeleton.setFlipX(getX() < gameState.getPlayer().getX());
        
        if (recoveryTimer > 0) {
            recoveryTimer -= delta;
            if (recoveryTimer <= 0) {
                recoveryTimer = -1;
            }
            
            moveTowardsPoint(recoveryTargetX, getY(), recoveryMoveSpeed, delta);
        } else {
            if (getX() < gameState.getPlayer().getX()) {
                moveTowardsPoint(gameState.getPlayer().getX() - 10.0f, getY(), attackMoveSpeed, delta);
            } else {
                moveTowardsPoint(gameState.getPlayer().getX() + 10.0f, getY(), attackMoveSpeed, delta);
            }
        }
        
        float distance = Math.abs(getX() - gameState.getPlayer().getX());
        
        if (!gameState.getPlayer().getAnimationState().getCurrent(0).getAnimation().getName().equals("hit") && animationState.getCurrent(1) == null && distance < PlayerEntity.ATTACK_DISTANCE) {
            if (getX() < gameState.getPlayer().getX()) {
                skeleton.setSkin((Skin) null);
                skeleton.setSkin("left");
            } else {
                skeleton.setSkin((Skin) null);
                skeleton.setSkin("right");
            }
        } else {
            skeleton.setSkin((Skin) null);
            skeleton.setSkin("none");
        }
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

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Mode getMode() {
        return mode;
    }

    public Skeleton getSkeleton() {
        return skeleton;
    }
    
    public void hit() {
        if (hits > 0) {
            hits--;
            if (hits <= 0) {
                animationState.setAnimation(1, "die", false);
            } else {
                recoveryTimer = RECOVERY_REST_TIME;
                if (type == Type.HAT) {
                    if (getX() < gameState.getPlayer().getX()) {
                        recoveryTargetX = gameState.getPlayer().getX() - PlayerEntity.ATTACK_DISTANCE + .1f;
                    } else {
                        recoveryTargetX = gameState.getPlayer().getX() + PlayerEntity.ATTACK_DISTANCE - .1f;
                    }
                } else if (type == Type.SPIKE_BALL) {
                    if (getX() < gameState.getPlayer().getX()) {
                        recoveryTargetX = gameState.getPlayer().getX() + PlayerEntity.ATTACK_DISTANCE - .1f;
                    } else {
                        recoveryTargetX = gameState.getPlayer().getX() - PlayerEntity.ATTACK_DISTANCE + .1f;
                    }
                }
            }
        }
    }

    public Type getType() {
        return type;
    }

    public AnimationState getAnimationState() {
        return animationState;
    }
}
