package models.chat;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import static java.util.concurrent.TimeUnit.SECONDS;
import models.factory.Factory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import play.libs.F;
import play.libs.Json;
import play.mvc.WebSocket;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import utils.gamebus.GameEventType;
import utils.gamebus.GameMessages.GameEvent;
import utils.gamebus.GameMessages.Join;

public class ChatFactory extends Factory {

    public static synchronized void createChat(final String username, final String room, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) throws Exception {
        final ActorRef finalRoom = create(room, Chat.class);
        Future<Object> future = Patterns.ask(finalRoom, new Join(username, out), 1000);
        // Send the Join message to the room
        String result = (String) Await.result(future, Duration.create(10, SECONDS));

        if ("OK".equals(result)) {

            //Define the actions to be performed on the websockets
            in.onMessage(new F.Callback<JsonNode>() {
                @Override
                public void invoke(JsonNode event) {

                    // Send a Talk message to the room.
                    finalRoom.tell(new GameEvent(event.get("text").asText(), username, GameEventType.talk), finalRoom);

                }
            });

            // When the socket is closed.
            in.onClose(new F.Callback0() {
                @Override
                public void invoke() {
                    // Send a Quit message to the room.
                    finalRoom.tell(new GameEvent(username, room, GameEventType.quit), finalRoom);
                }
            });

        } else {
            // Cannot connect, create a Json error.
            ObjectNode error = Json.newObject();
            error.put("error", result);
            // Send the error to the socket.
            out.write(error);
        }
    }
}
