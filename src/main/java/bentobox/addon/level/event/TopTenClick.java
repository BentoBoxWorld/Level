package bentobox.addon.level.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This event is fired when a player clicks on a top ten head.
 * 
 * @author tastybento
 */
public class TopTenClick extends Event implements Cancellable {
    
    private boolean cancelled;
    private static final HandlerList handlers = new HandlerList();
    private final String owner;
    

    public TopTenClick(String owner) {
        this.owner = owner;
    }

    /**
     * @return name of head owner that was clicked
     */
    public String getOwner() {
        return owner;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
        
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }


}
