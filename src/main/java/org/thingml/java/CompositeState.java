package org.thingml.java;

import org.thingml.java.ext.Event;
import org.thingml.java.ext.IStateAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Composite are containers for regions
 *
 * @author bmori
 */
public abstract class CompositeState extends AtomicState {

    protected final List<Region> regions;

    protected final Logger log = Logger.getLogger(AtomicState.class.getName());

    public CompositeState(final String name, final List<IState> states, final IState initial, final List<Handler> transitions, final IStateAction action) {
        this(name, states, initial, transitions, action, Collections.EMPTY_LIST, false);
    }

    public CompositeState(final String name, final List<IState> states, final IState initial, final List<Handler> transitions, final IStateAction action, final List<Region> regions, final boolean keepHistory) {
        super(name);
        Region r = new Region("default", states, initial, transitions, keepHistory);
        List<Region> reg = new ArrayList<>(regions);
        reg.add(0, r);//we add the default region first
        this.regions = Collections.unmodifiableList(reg);
    }

    public synchronized boolean dispatch(final Event e, final Port port) {
        final DispatchStatus status = new DispatchStatus();
        getRegions().forEach(r -> {
            status.update(r.handle(e, port));
        });
        return status.status;
    }

    public synchronized IState dispatch(final Event e, Port port, final HandlerHelper helper) {
        if (!dispatch(e, port)) {//none of the region consumed the event, then this composite can try to consume it
            final IHandler handler = helper.getActiveHandler(this, e, port);
            return handler.execute(e);
        } else {
            return null;
        }
    }

    public String toString() {
        return "Composite state " + name;
    }

    //return a parallel or sequential stream, depending on sub-class (MT or ST).
    public abstract Stream<Region> getRegions();

    public void onEntry() {
        log.finest(name + " on entry at " + System.currentTimeMillis());
        super.onEntry();
        getRegions().forEach(r -> r.onEntry());
    }

    public void onExit() {
        log.finest(name + " on exit at " + System.currentTimeMillis());
        getRegions().forEach(r -> r.onExit());
        super.onExit();
    }

}