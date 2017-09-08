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
import com.badlogic.gdx.utils.FloatArray;
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
    private boolean attacking;
    private boolean recovering;
    private int hits;
    private float attackMoveSpeed;
    private float recoverySpeed;
    private static final float RECOVERY_REST_TIME = .75f;
    private float restTime;
    private int points;
    private static final float MINIMUM_SPACING = 100.0f;
    
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
            recoverySpeed = 700.0f;
            hits = 1;
            points = 10;
        } else if (type == Type.HAT) {
            skeletonData = getCore().getAssetManager().get(Core.DATA_PATH + "/spine/hat.json", SkeletonData.class);
            recoverySpeed = 700.0f;
            hits = 2;
            points = 30;
        } else {
            skeletonData = getCore().getAssetManager().get(Core.DATA_PATH + "/spine/spike.json", SkeletonData.class);
            recoverySpeed = 2000.0f;
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
        setMode(Mode.NONE);
        attacking = true;
        
        animationState.addListener(new AnimationState.AnimationStateAdapter() {
            @Override
            public void event(AnimationState.TrackEntry entry, Event event) {
                if (event.getData().getName().equals("death")) {
                    ConfettiEntity confet = new ConfettiEntity(EnemyEntity.this.gameState);
                    PointAttachment confetPoint = (PointAttachment) skeleton.getAttachment("confetti", "confetti");
                    Vector2 location = confetPoint.computeWorldPosition(skeleton.findBone("body"), new Vector2());
                    
                    confet.setPosition(location.x, location.y);
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
        
        recovering = false;
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
        
        if (recovering) {
            float recoveryTargetX;
            
            if (skeleton.getFlipX()) {
                recoveryTargetX = gameState.getPlayer().getX() - 75.0f;
            } else {
                recoveryTargetX = gameState.getPlayer().getX() + 75.0f;
            }
            moveTowardsPoint(recoveryTargetX, getY(), recoverySpeed, delta);
            
            if (MathUtils.isEqual(getX(), recoveryTargetX)) {
                restTime -= delta;
                if (restTime <= 0) {
                    attacking = true;
                    recovering = false;
                    restTime = RECOVERY_REST_TIME;

                    if (skeleton.getFlipX()) {
                        setMotion(attackMoveSpeed, 0.0f);
                    } else {
                        setMotion(attackMoveSpeed, 180.0f);
                    }
                }
            }
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
        skeleton.setSkin((Skin)null);
        if (mode == Mode.RIGHT) {
            skeleton.setSkin("right");
        } else if (mode == Mode.LEFT) {
            skeleton.setSkin("left");
            skeleton.setFlipX(true);
        } else {
            skeleton.setSkin("none");
        }
        
        if (skeleton.getFlipX()) {
            setMotion(attackMoveSpeed, 0.0f);
        } else {
            setMotion(attackMoveSpeed, 180.0f);
        }
    }

    public Mode getMode() {
        return mode;
    }

    public Skeleton getSkeleton() {
        return skeleton;
    }

    public boolean isAttacking() {
        return attacking;
    }

    public void setAttacking(boolean attacking) {
        this.attacking = attacking;
    }
    
    public void hit() {
        if (hits > 0) {
            hits--;
            if (hits <= 0) {
                animationState.setAnimation(1, "die", false);
                attacking = false;
                recovering = false;
            } else {
                recovering = true;
                restTime = RECOVERY_REST_TIME;
                
                Vector2 temp = new Vector2();
                skeleton.getBounds(new Vector2(), temp, new FloatArray());
                if (type == Type.SPIKE_BALL) {
                    if (mode == Mode.LEFT) {
                        setMode(Mode.RIGHT);
                        skeleton.setFlipX(false);
                        addX(-temp.x);
                    } else if (mode == Mode.RIGHT) {
                        setMode(Mode.LEFT);
                        skeleton.setFlipX(true);
                        addX(temp.x);
                    }
                }
                setMotion(0.0f, 0.0f);
            }
        }
    }

    public Type getType() {
        return type;
    }

    public boolean isRecovering() {
        return recovering;
    }

    public AnimationState getAnimationState() {
        return animationState;
    }
}
