package org.thingml.java;

import org.thingml.java.ext.Event;
import org.thingml.java.ext.NullEventType;

import java.net.CookieHandler;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by bmori on 29.04.2014.
 */
public abstract class Component {

    protected CompositeState behavior;
    private Connector[][] bindings;
    private String name;

    //private final SignedEvent ne;
    private final Event ne = new NullEventType().instantiate(null);

    private Receiver receiver;
    private Thread receiverT;

    protected BlockingQueue<Event> queue = new ArrayBlockingQueue<Event>(64);

    public Component(int ports) {
        //ne = new SignedEvent(new NullEventType().instantiate(), null);
        bindings = new Connector[ports][];
    }

    public Component(String name, int ports) {
        this(ports);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    abstract public Component buildBehavior();

    boolean canSend(Event event, Port port) {
        return port.out.contains(event.getType());
    }

    boolean canReceive(Event event, Port port) {
        return port.in.contains(event.getType());
    }

    public void send(Event event, Port port) {
        Connector[] connectors = bindings[port.ID];
        if (connectors != null) {
            for(Connector c : connectors) {
                //c.onEvent(event, this);
                if (c.client == this)
                    c.server.receive(event, c.provided);
                else
                    c.client.receive(event, c.required);
            }
        }
    }

    public void receive(Event event, final Port p) {
        event.setPort(p);
        queue.offer(event);
    }

    public void start() {
        //receive(net.instantiate(), null);//it might be an auto-transition to be triggered right away
        behavior.onEntry();
        if (receiver == null) {
            receiver = new Receiver();
        } else {
            receiver.active = true;
        }
        receiverT = new Thread(receiver);
        receiverT.start();
    }

    public void stop() {
        if (receiver != null) {
            receiver.active = false;
            try {
                receiverT.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (behavior != null) {
            behavior.onExit();
        }
    }

    public void connect(Port p, Connector... c) {
        bindings[p.ID] = c;
    }

    private class Receiver implements Runnable {

        boolean active = true;

        @Override
        public void run() {
            queue.offer(ne);
            while (active) {
                try {
                    final Event e = queue.take();//should block if queue is empty, waiting for a message
                    behavior.dispatch(e, e.getPort());
                    while (behavior.dispatch(ne, null)) {
                        //receiverT.sleep(0,1);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
