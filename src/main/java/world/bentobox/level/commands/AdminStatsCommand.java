package world.bentobox.level.commands;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.bukkit.World;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.level.Level;
import world.bentobox.level.objects.TopTenData;

public class AdminStatsCommand extends CompositeCommand {

    private final Level level;

    public AdminStatsCommand(Level addon, CompositeCommand parent) {
	super(parent, "stats");
	this.level = addon;
	new AdminTopRemoveCommand(addon, this);
    }

    @Override
    public void setup() {
	this.setPermission("admin.stats");
	this.setOnlyPlayer(false);
	this.setDescription("admin.stats.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
	user.sendMessage("admin.stats.title");
	Map<World, TopTenData> topTenLists = level.getManager().getTopTenLists();
	if (topTenLists.isEmpty()) {
	    user.sendMessage("admin.stats.no-data");
	    return false;
	}
	for (Entry<World, TopTenData> en : topTenLists.entrySet()) {
	    user.sendMessage("admin.stats.world", TextVariables.NAME,
		    level.getPlugin().getIWM().getWorldName(en.getKey()));
	    Map<String, Long> topTen = en.getValue().getTopTen();
	    if (topTen.isEmpty()) {
		user.sendMessage("admin.stats.no-data");
		return false;
	    }

	    // Calculating basic statistics
	    long sum = 0, max = Long.MIN_VALUE, min = Long.MAX_VALUE;
	    Map<Long, Integer> levelFrequency = new HashMap<>();

	    for (Long level : topTen.values()) {
		sum += level;
		max = Math.max(max, level);
		min = Math.min(min, level);
		levelFrequency.merge(level, 1, Integer::sum);
	    }

	    double average = sum / (double) topTen.size();
	    List<Long> sortedLevels = topTen.values().stream().sorted().collect(Collectors.toList());
	    long median = sortedLevels.get(sortedLevels.size() / 2);
	    Long mode = Collections.max(levelFrequency.entrySet(), Map.Entry.comparingByValue()).getKey();

	    // Logging basic statistics
	    user.sendMessage("admin.stats.average-level", TextVariables.NUMBER, String.valueOf(average));
	    user.sendMessage("admin.stats.median-level", TextVariables.NUMBER, String.valueOf(median));
	    user.sendMessage("admin.stats.mode-level", TextVariables.NUMBER, String.valueOf(mode));
	    user.sendMessage("admin.stats.highest-level", TextVariables.NUMBER, String.valueOf(max));
	    user.sendMessage("admin.stats.lowest-level", TextVariables.NUMBER, String.valueOf(min));

	    // Grouping data for distribution analysis
	    Map<String, Integer> rangeMap = new TreeMap<>();
	    for (Long level : topTen.values()) {
		String range = getRange(level);
		rangeMap.merge(range, 1, Integer::sum);
	    }

	    // Logging distribution
	    user.sendMessage("admin.stats.distribution");
	    for (Map.Entry<String, Integer> entry : rangeMap.entrySet()) {
		user.sendMessage(
			entry.getKey() + ": " + entry.getValue() + " " + user.getTranslation("admin.stats.islands"));
	    }
	}
	return true;
    }

    private static String getRange(long level) {
	long rangeStart = level / 100 * 100;
	long rangeEnd = rangeStart + 99;
	return rangeStart + "-" + rangeEnd;
    }
}
