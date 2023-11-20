package world.bentobox.level.objects;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.World;

import com.google.gson.annotations.Expose;

/**
 * This class stores the top ten.
 * 
 * @author tastybento
 *
 */
public class TopTenData {

    // UniqueId is the world name
    @Expose
    private String uniqueId = "";
    @Expose
    private Map<String, Long> topTen = new LinkedHashMap<>();

    public TopTenData(World k) {
	uniqueId = k.getName().toLowerCase(Locale.ENGLISH);
    }

    /**
     * @return the topTen
     */
    public Map<String, Long> getTopTen() {
	return topTen;
    }

    /**
     * @param topTen the topTen to set
     */
    public void setTopTen(Map<String, Long> topTen) {
	this.topTen = topTen;
    }

}
