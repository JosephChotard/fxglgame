package team.underwurlde;

import static com.almasb.fxgl.dsl.FXGL.entityBuilder;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.almasb.fxgl.physics.box2d.dynamics.BodyType;
import com.almasb.fxgl.physics.box2d.dynamics.FixtureDef;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import team.underwurlde.component.EnemyComponent;
import team.underwurlde.component.MoveComponent;
import team.underwurlde.component.PlayerComponent;
import team.underwurlde.component.SlimeComponent;


public class GameAppFactory implements EntityFactory {

    @Spawns("player")
    public Entity newPlayer(SpawnData data) {
        // physics
        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.DYNAMIC);
        physics.setFixtureDef(new FixtureDef().friction(0.0f)); // 0 friction
        return FXGL.entityBuilder()
            .type(EntityType.PLAYER)
            .from(data)
            .viewWithBBox(new Rectangle(GameConfig.getPlayerSize(), GameConfig.getPlayerSize()))
            .with(physics)
            .with(new PlayerComponent())
            .build();
            //.with(new CollidableComponent(true))
    }

    @Spawns("collide")
	public Entity newCollide(SpawnData data) {
        return FXGL.entityBuilder()
            .type(EntityType.WALL)
            .bbox(new HitBox(BoundingShape.box(data.<Integer>get("width"), data.<Integer>get("height"))))
            .with(new PhysicsComponent())
			.from(data)
            .build();
            // .with(new CollidableComponent(true))
    }


    @Spawns("slime")
    public Entity slime(SpawnData data) {
        int movementSpeed = 0;

        // add physics to slime
        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.DYNAMIC);
        physics.setFixtureDef(new FixtureDef().friction(0.0f)); // 0 friction

        return FXGL.entityBuilder()
            .type(EntityType.ENEMY)
            .from(data)
            .viewWithBBox(new Rectangle(GameConfig.getEnemySize()/2, GameConfig.getEnemySize()/2, Color.BLUE))
            .with(new SlimeComponent())
            .with(physics)
            .with(new MoveComponent(movementSpeed, -1, MoveComponent.MoveType.LINEAR))
            .build();
    }


    @Spawns("enemy")
	public Entity newEnemy(SpawnData data) {
        // add physics to enemy
        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.DYNAMIC);
        physics.setFixtureDef(new FixtureDef().friction(0.0f)); // 0 friction
        return entityBuilder()
            .type(EntityType.ENEMY)
            .from(data)
            .viewWithBBox(new Rectangle(GameConfig.getEnemySize(), GameConfig.getEnemySize(), Color.ORANGE))
            .with(physics)
            .with(new EnemyComponent())
            .with(new MoveComponent(GameConfig.getEnemySpeed(), 200, MoveComponent.MoveType.LINEAR))
            .build();
    }

    @Spawns("")
    public Entity newEmpty(SpawnData data) {
        System.out.println("An object has no Type: must be fixed");
        return null;
    }
}