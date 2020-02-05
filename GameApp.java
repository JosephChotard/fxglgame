package team.underwurlde;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.input.UserAction;

import javafx.scene.input.KeyCode;
import com.almasb.sslogger.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import team.underwurlde.component.MoveComponent;
import team.underwurlde.component.PlayerComponent;
import team.underwurlde.net.Client;
import team.underwurlde.net.DataMessage;
import team.underwurlde.net.RequestMessage;
import team.underwurlde.net.Server;
import static com.almasb.fxgl.dsl.FXGL.*;
import static com.sun.javafx.application.PlatformImpl.exit;

public class GameApp extends GameApplication
{
    private Entity player1;
    private Entity player2;
    private Room curRoom;
    final public static int WIDTH = 1280;
    final public static int HEIGHT = 720;
    public Menus menu;
	public boolean gameActive = false;
	public boolean isMulti = false;

    private Entity slime;
    private Entity enemy;

    //Networking
    private static final Logger log = Logger.get(GameApp.class);

    private Server server = new Server();
    private Client client = new Client("localhost");

    private boolean isHost = true;
    private boolean isConnected = false;

    private Map<KeyCode, Boolean> keys = new HashMap<>();

    private Queue<DataMessage> updateQueue = new ConcurrentLinkedQueue<>();
    private Queue<RequestMessage> requestQueue = new ConcurrentLinkedQueue<>();
    //End of Networking
    
    public GameApp()
    {
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void initSettings(GameSettings settings)
    {
        settings.setWidth(WIDTH);
        settings.setHeight(HEIGHT);
        settings.setTitle("Underwurlde");
        settings.setVersion("0.1");
        settings.setIntroEnabled(false);
        settings.setMenuKey(KeyCode.UNDEFINED);
        settings.setConfigClass(GameConfig.class);
		settings.isFullScreenFromStart();
    }

    protected void startGame()
    {
		if (isMulti) {
			initNetworking();
		}

        gameActive = true;
        FXGL.getGameWorld().addEntityFactory(new GameAppFactory());
        FXGL.setLevelFromMap("level1.tmx");

        player1 = getGameWorld().spawn("player", FXGL.getAppWidth() / 2 - 15, FXGL.getAppHeight() / 2 - 15);

        // Creat viewport and scale arcordingly
        var zoom = GameConfig.getZoom();
        getGameScene().getViewport().setBounds(0, 0, 800 + (FXGL.getAppWidth() / zoom * (zoom - 1)),
                800 + (FXGL.getAppHeight() / zoom * (zoom - 1)));
        getGameScene().getViewport().setZoom(GameConfig.getZoom());
        getGameScene().getViewport().bindToEntity(player1, FXGL.getAppWidth() / (2 * zoom),
                FXGL.getAppHeight() / (2 * zoom));

        // spawn enemies
        /*enemy = getGameWorld().spawn("enemy", 100, 100);
        slime = getGameWorld().spawn("slime", 100, 100);*/

    }

    @Override
    protected void initPhysics()
    {
        super.initPhysics();
        getPhysicsWorld().setGravity(0, 0);
    }

    @Override
    public void onUpdate(double tpf)
    {
        if (isHost)
        {
            if (isMulti && !isConnected)
                return;

            // Your Code goes here

           	var enemies = FXGL.getGameWorld().getEntitiesByComponent(MoveComponent.class);
			for (var enemy : enemies) {
				enemy.getComponent(MoveComponent.class).move();
			}
            // entity.setScaleX(player.getX() < entity.getX() ? -1 : 1);

            // No code below here

            RequestMessage data = requestQueue.poll();

            if (data != null)
            {
                for (KeyCode key : data.keys)
                {
                    switch (key)
                    {
                        case A:
                            player2.getComponent(PlayerComponent.class).left();
                            break;
                        case D:
                            player2.getComponent(PlayerComponent.class).right();
                            break;
                        case W:
                            player2.getComponent(PlayerComponent.class).up();
                            break;
                        case S:
                            player2.getComponent(PlayerComponent.class).down();
                            break;
                    }
                }

                try
                {
                    server.send(new DataMessage(player1.getX(), player1.getY(), player2.getX(), player2.getY()));
                }
                catch (Exception e)
                {
                    log.warning("Failed to send message: " + e.getMessage());
                    exit();
                }
            }
        }
        else
        {
            DataMessage data = updateQueue.poll();
            if (data != null)
            {
                player1.setPosition(data.pos1x, data.pos1y);
                player2.setPosition(data.pos2x, data.pos2y);
            }

            KeyCode[] codes = keys.keySet()
                    .stream()
                    .filter(k -> keys.get(k))
                    .collect(Collectors.toList())
                    .toArray(new KeyCode[0]);

            try
            {
                client.send(new RequestMessage(codes));

            }
            catch (Exception e)
            {
                log.warning("Failed to send message: " + e.getMessage());
                exit();
            }

            keys.forEach((key, value) -> keys.put(key, false));

        }

    }

    @Override
    protected void initInput()
    {
        if (isHost)
        {
            //Your code goes here

            super.initInput();
            getInput().addAction(new UserAction("Left")
            {
                @Override
                protected void onAction()
                {
                    player1.getComponent(PlayerComponent.class).left();
                }
            }, KeyCode.A);

            getInput().addAction(new UserAction("Right")
            {
                @Override
                protected void onAction()
                {
                    player1.getComponent(PlayerComponent.class).right();
                }
            }, KeyCode.D);

            getInput().addAction(new UserAction("Up")
            {
                @Override
                protected void onAction()
                {
                    player1.getComponent(PlayerComponent.class).up();
                }
            }, KeyCode.W);

            getInput().addAction(new UserAction("Down")
            {
                @Override
                protected void onAction()
                {
                    player1.getComponent(PlayerComponent.class).down();
                }
			}, KeyCode.S);
			
			getInput().addAction(new UserAction("Dash") {
				@Override
				protected void onAction() {
					player1.getComponent(PlayerComponent.class).dash();
				}
			}, KeyCode.SPACE);

			FXGL.onKey(KeyCode.ESCAPE, () -> menu.showPause());


            //No code below here
        }
        else
        {
            initKeys(KeyCode.A, KeyCode.D, KeyCode.W, KeyCode.S, KeyCode.ESCAPE);
        }
    }

    private void initKeys(KeyCode... codes)
    {
        for (KeyCode k : codes)
        {
            keys.put(k, false);
            FXGL.onKey(k, () -> keys.put(k, true));
        }
    }

    @Override
    protected void initUI()
    {
        FXGL.getGameScene().appendCSS(FXGL.getAssetLoader().loadCSS("style.css"));
        menu = new Menus(this);
    }

    public static void main(String[] args)
    {
        launch(args);
    }

    private void initNetworking()
    {
        if (isHost)
        {
            server.addParser(RequestMessage.class, data -> requestQueue.offer(data));
            server.addParser(String.class, data -> isConnected = true);
            server.start();
        }
        else
        {
            client.addParser(DataMessage.class, data -> updateQueue.offer(data));

            try
            {
                client.connect();
                client.send("Hi");
            }
            catch (Exception e)
            {
                e.printStackTrace();
                exit();
            }
        }
    }
}