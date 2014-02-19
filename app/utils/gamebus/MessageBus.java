package utils.gamebus;

import akka.actor.ActorRef;
import akka.event.japi.LookupEventBus;
import utils.gamebus.GameMessages.GameEvent;

public class MessageBus extends LookupEventBus<Object, Object, Object> {

    /**
     * Initial size of the index data structure used internally (i.e. the
     * expected number of different classifiers)
     */
    @Override
    public int mapSize() {
        return 5;
    }

    /**
     * Used to define a partial ordering of subscribers. The ordering is based
     * on Event.channel
     */
    @Override
    public int compareSubscribers(Object subscriberA, Object subscriberB) {
        return ((ActorRef) subscriberA).path().compareTo(((ActorRef) subscriberB).path());
    }

    /**
     * Extract the classification data from the event.
     *
     * @param event {@link Event} to classify
     * @return Channel string from the {@link Event}
     */
    @Override
    public Object classify(Object event) {
        return ((GameEvent) event).getChannel().getRoom();
    }

    /**
     * Publish an {@link Event}
     *
     * @param event {@link Event} to publish
     * @param subscriber {@link akka.actor.ActorRef} that is subscribed to the
     * {@link Event}
     */
    @Override
    public void publish(Object event, Object subscriber) {
        ((ActorRef) subscriber).tell(event, (ActorRef) subscriber);
    }
}