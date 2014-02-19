package models.factory;

import play.libs.Akka;
import utils.gamebus.GameBus;
import utils.gamebus.GameMessages.Room;
import akka.actor.ActorRef;
import akka.actor.EmptyLocalActorRef;
import akka.actor.Props;

public abstract class Factory {

	/**
	 * Join the evolutions.default room.
	 */
	@SuppressWarnings("rawtypes")
	protected static ActorRef create(final String room, final int maxMembers,
			final Class module) throws Exception {
		ActorRef newRoom;
		final String roomID = module.getSimpleName() + "_" + room.replaceAll("[^\\w\\s]","");
			
		// Try to see if we have already registered this actor in the system, if
		// it is the case return the reference
		newRoom = Akka.system().actorFor("akka://application/user/" + roomID);
		if (newRoom instanceof EmptyLocalActorRef) {
			@SuppressWarnings("unchecked")
			final Props properties = new Props(module);
			// Otherwise create a new actor to the room and subscribe it to the
			// message channel
			newRoom = Akka.system().actorOf(properties, roomID);
			newRoom.tell(new Room(room, maxMembers), newRoom);
			GameBus.getInstance().subscribe(newRoom, room);
		}
		return newRoom;
	}

	protected static ActorRef create(final String room, final Class module)
			throws Exception {
		ActorRef newRoom;
		final String roomID = module.getSimpleName() + "_" + room.replaceAll("[^\\w\\s]","");
		// Try to see if we have already registered this actor in the system, if
		// it is the case return the reference
		newRoom = Akka.system().actorFor("akka://application/user/" + roomID);
		if (newRoom instanceof EmptyLocalActorRef) {
			@SuppressWarnings("unchecked")
			final Props properties = new Props(module);
			// Otherwise create a new actor to the room and subscribe it to the
			// message channel
			newRoom = Akka.system().actorOf(properties, roomID);
			newRoom.tell(new Room(room), newRoom);
			GameBus.getInstance().subscribe(newRoom, room);
		}
		return newRoom;
	}
}
