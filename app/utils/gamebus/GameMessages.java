package utils.gamebus;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import play.mvc.WebSocket;

public class GameMessages {

    public static class GameEvent {

        protected Room channel;
        protected GameEventType type;
        protected String message;
        protected ObjectNode object;
        protected String username;

        private GameEvent(String message, Room channel) {
            this.channel = channel;
            this.type = GameEventType.unknown;
            this.message = message;
        }

        public GameEvent(Room channel, GameEventType type) {
            this.channel = channel;
            this.type = type;
        }

        public GameEvent(GameEventType type) {
            this.type = type;
        }

        public GameEvent(String message, Room channel, GameEventType type) {
            this.channel = channel;
            this.type = type;
            this.message = message;
        }

        public GameEvent(String message, String username, GameEventType type) {
            this.message = message;
            this.username = username;
            this.type = type;
        }

        public GameEvent(String message, String username, Room channel, GameEventType type) {
            this(message, channel);
            this.username = username;
            this.type = type;
        }

        public GameEventType getType() {
            return type;
        }

        public void setObject(ObjectNode object) {
            this.object = object;
        }

        public ObjectNode getObject() {
            return object;
        }

        public Room getChannel() {
            return channel;
        }

        public String getMessage() {
            return message;
        }

        public String getUsername() {
            return username;
        }
    }

    public static class SystemMessage extends GameEvent {

        public SystemMessage(String message, Room channel) {
            super(message, channel);
        }
    }

    public static class Join {

        final String username;
        final WebSocket.Out<JsonNode> channel;

        public Join(String username, WebSocket.Out<JsonNode> channel) {
            this.username = username;
            this.channel = channel;
        }

        public WebSocket.Out<JsonNode> getChannel() {
            return channel;
        }

        public String getUsername() {
            return username;
        }
    }

    public static class Room {

        final String room;
        final int requiredPlayers;

        public Room(String room, int requiredPlayers) {
            this.room = room;
            this.requiredPlayers = requiredPlayers;
        }

        public Room(String room) {
            this.room = room;
            requiredPlayers = -1;
        }

        public String getRoom() {
            return room;
        }

        public int getRequiredPlayers() {
            return requiredPlayers;
        }
    }

    public static class GameInfo {

        String roomName;
        Integer nPlayers;
        Integer maxPlayers;

        public GameInfo(String roomName, Integer nPlayers, Integer maxPlayers) {
            this.roomName = roomName;
            this.nPlayers = nPlayers;
            this.maxPlayers = maxPlayers;
        }

        public Integer getMaxPlayers() {
            return maxPlayers;
        }

        public String getRoomName() {
            return roomName;
        }

        public Integer getnPlayers() {
            return nPlayers;
        }
    }
}
