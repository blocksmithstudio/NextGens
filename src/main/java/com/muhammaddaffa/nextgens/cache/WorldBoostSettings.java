package com.muhammaddaffa.nextgens.cache;

import java.util.List;

public class WorldBoostSettings {

    private final Double sellMultiplier;
    private final Double speedMultiplierPercentage;
    private final Integer dropMultiplier;
    private final List<String> whitelistGeneratorIds;

    public WorldBoostSettings(
            Double sellMultiplier,
            Double speedMultiplierPercentage,
            Integer dropMultiplier,
            List<String> whitelistGeneratorIds
    ) {
        this.sellMultiplier = sellMultiplier;
        this.speedMultiplierPercentage = speedMultiplierPercentage;
        this.dropMultiplier = dropMultiplier;
        this.whitelistGeneratorIds = List.copyOf(whitelistGeneratorIds);
    }

    public Double getSellMultiplier() {
        return sellMultiplier;
    }

    public Double getSpeedMultiplierPercentage() {
        return speedMultiplierPercentage;
    }

    public Integer getDropMultiplier() {
        return dropMultiplier;
    }

    public List<String> getWhitelistGeneratorIds() {
        return whitelistGeneratorIds;
    }

    public boolean isGeneratorWhitelisted(String generatorId) {
        return whitelistGeneratorIds.isEmpty() || whitelistGeneratorIds.contains(generatorId);
    }
}
