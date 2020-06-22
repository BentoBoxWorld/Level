package world.bentobox.level.objects;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.bukkit.World;

import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.database.objects.Table;

/**
 * This class stores the top ten.
 * @author tastybento
 *
 */
@Table(name = "TopTenData")
public class TopTenData implements DataObject {

    // UniqueId is the world name
    @Expose
    private String uniqueId = "";
    @Expose
    private Map<UUID, Long> topTen = new LinkedHashMap<>();

    public TopTenData(World k) {
        uniqueId = k.getName().toLowerCase(Locale.ENGLISH);
    }

    @Override
    public String getUniqueId() {
        // This is the world name
        return uniqueId;
    }

    @Override
    public void setUniqueId(String uniqueId) {
        // This is the world name - make it always lowercase
        this.uniqueId = uniqueId.toLowerCase(Locale.ENGLISH);
    }
    /**
     * @return the topTen
     */
    public Map<UUID, Long> getTopTen() {
        return topTen;
    }
    /**
     * @param topTen the topTen to set
     */
    public void setTopTen(Map<UUID, Long> topTen) {
        this.topTen = topTen;
    }


}
