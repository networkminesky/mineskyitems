package net.minesky.events;

import org.bukkit.event.Cancellable;

public class DummyEvent implements Cancellable {
    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public void setCancelled(boolean b) {

    }
}
