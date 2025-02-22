package world.bentobox.level.calculators;

import org.bukkit.Location;
import org.bukkit.Material;

import com.craftaro.ultimatestacker.api.UltimateStackerApi;
import com.craftaro.ultimatestacker.api.utils.Stackable;

/**
 * Isolates UltimateStacker imports so that they are only loaded if the plugin exists
 */
public class UltimateStackerCalc {
    public static void addStackers(Material material, Location location, Results results, boolean belowSeaLevel,
            int value) {
        Stackable stack = UltimateStackerApi.getBlockStackManager().getBlock(location);
        if (stack != null) {
            if (belowSeaLevel) {
                results.underWaterBlockCount.addAndGet((long) stack.getAmount() * value);
                results.uwCount.add(material);
            } else {
                results.rawBlockCount.addAndGet((long) stack.getAmount() * value);
                results.mdCount.add(material);
            }
        }
    }
}

