package com.codingame.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Game {
    public List<Player> players;
    public Center center;
    public ArrayList<Ball> balls = new ArrayList<>();
    public ArrayList<Car> entities = new ArrayList<>();
    public ArrayList<Unit.UnitCollision> wallCollisions = new ArrayList<>();
    public ArrayList<Collision> allOccuringCollisions = new ArrayList<>();
    public ArrayList<Integer> scorings = new ArrayList<>();
    public double t;
    public int turn;

    public Game(int numCars, int maxBallCount, List<Player> players){
        this.players = players;
        this.center = new Center();
        int startY = Constants.Random.nextInt(1000)-2500;
        for(Player p : players){
            p.initialize(numCars, p.getIndex()==0?-1:1, startY);
            for(Car c : p.Cars){
                entities.add(c);
            }
        }

        int dy = -1;
        double spawnX = Constants.Random.nextDouble()*1000;
        double targetX = Constants.Random.nextDouble()*6000-3000;
        int sign = -1;
        for(int i = 0; i < maxBallCount; i++){
            Ball ball = new Ball(spawnX*sign*-1, dy*Constants.BALL_SPAWN_RADIUS);
            ball.thrust(new Point(targetX*sign, 0), Constants.BALL_SPAWN_SPEED);
            balls.add(ball);
            dy = 1;
            sign = 1;
            ball.adjust();
        }
    }

    public boolean isGameOver(){
        for(Player p : players){
            if(p.getScore() >= Constants.MAX_GOALS) return true;
        }

        return false;
    }

    public void gameLoop(){

        // View related
        turn++;
        beforeRound();

        // Game logic
        t = 0.0;
        while(t < 1.0){
            ArrayList<Collision> collisions = findNextCollision(1.0-t);
            if(collisions.size()==0){
                break;
            }

            Collections.sort(collisions, Comparator.comparingDouble(s -> s.time));

            step(collisions.get(0).time);
            for(Collision next : collisions){
                next.a.reactToCollision(next.b, this);

                // View
                if(next.b==null){
                    wallCollisions.add(new Unit.UnitCollision(next.a.clonePoint(), null, t, Unit.BallState.Unchanged, 0.0));
                }

                // Game manager tooltip - No need for this collision anymore, set time to T
                next.time = t;
                allOccuringCollisions.add(next);
            }

            if(isGameOver()){
                onRoundEnd();
                return;
            }
        }

        step(1.0-t);
        onRoundEnd();
    }

    private ArrayList<Collision> findNextCollision(double maxTime){
        for(Car c : entities){
            c.beforeColvx = c.vx;
            c.beforeColvy = c.vy;
        }
        for(Ball b: balls){
            b.beforeColvx = b.vx;
            b.beforeColvy = b.vy;
        }

        ArrayList<Collision> collisions = new ArrayList<>();
        Collision collision;
        for(int i = 0; i < entities.size();i++){
            Car a = entities.get(i);

            // Border
            collision = a.getCollision();
            if(collision.time >= 0){
                if(Math.abs(collision.time-maxTime) < Constants.EPSILON){
                    collisions.add(collision);
                }else if(collision.time < maxTime){
                    collisions.clear();
                    collisions.add(collision);
                    maxTime = collision.time;
                }
            }

            // Center
            collision = center.getCarCollision(a);
            if(collision.time >= 0){
                if(Math.abs(collision.time-maxTime) < Constants.EPSILON){
                    collisions.add(collision);
                }else if(collision.time < maxTime){
                    collisions.clear();
                    collisions.add(collision);
                    maxTime = collision.time;
                }
            }

            // Ball
            if(a.ball==null){
                for(Ball ball: balls){
                    if(ball.captured) continue;
                    collision = ball.getCollision(a, a.radius - 1);
                    if(collision.time >= 0){
                        if(Math.abs(collision.time-maxTime) < Constants.EPSILON){
                            collisions.add(collision);
                        }else if(collision.time < maxTime){
                            collisions.clear();
                            collisions.add(collision);
                            maxTime = collision.time;
                        }
                    }
                }
            }

            // Other cars
            for(int j = i+1; j < entities.size(); j++){
                Unit b = entities.get(j);

                collision = a.getCollision(b);
                if(collision.time >= 0){
                    if(Math.abs(collision.time-maxTime) < Constants.EPSILON){
                        collisions.add(collision);
                    }else if(collision.time < maxTime){
                        collisions.clear();
                        collisions.add(collision);
                        maxTime = collision.time;
                    }
                }
            }
        }

        // Balls with border
        for(Ball ball: balls){
            if(ball.captured) continue;
            collision = ball.getCollision();
            if(collision.time >= 0){
                if(Math.abs(collision.time-maxTime) < Constants.EPSILON){
                    collisions.add(collision);
                }else if(collision.time < maxTime){
                    collisions.clear();
                    collisions.add(collision);
                    maxTime = collision.time;
                }
            }
        }

        return collisions;
    }

    private void beforeRound(){
        wallCollisions.clear();
        scorings.clear();
        allOccuringCollisions.clear();

        for(Unit e : entities){
            e.beforeRound();
        }
        for(Ball b : balls){
            b.beforeRound();
        }
    }

    private void step(double time){
        t+=time;
        for(Unit e : entities){
            e.move(time);
        }

        for(Ball b : balls)
            b.move(time);

    }

    private void onRoundEnd(){
        for(Unit e : entities)
            e.adjust();

        for(Ball b : balls)
            b.adjust();


        for(int i = balls.size()-1; i >= 0; i--){
            if(balls.get(i).captured) balls.remove(i);
        }
    }

    public void addBall(Ball ball){
        balls.add(ball);
    }

    public void score(int index){
        players.get(index).score(turn+t*0.1);
        scorings.add(index);
    }
}