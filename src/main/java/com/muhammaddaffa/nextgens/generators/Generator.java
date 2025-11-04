package com.muhammaddaffa.nextgens.generators;

import com.muhammaddaffa.mdlib.utils.ItemBuilder;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.requirements.GensRequirement;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public record Generator(
        String id,
        String displayName,
        double interval,
        ItemStack item,
        List<Drop> drops,
        String nextTier,
        double cost,
        boolean corrupted,
        double fixCost,
        double corruptChance,
        Boolean onlineOnly,
        List<GensRequirement> placeRequirements,
        List<GensRequirement> upgradeRequirements
) {

    public List<String> checkRequirements(Player player, List<GensRequirement> requirements) {
        List<String> messages = new ArrayList<>();
        for (GensRequirement requirement : requirements) {
            if (!requirement.isSuccessful(player)) {
                messages.add(requirement.getMessage());
            }
        }
        return messages;
    }

    public Drop getRandomDrop() {
        if (drops == null || drops.isEmpty())
            return null;

        // Prepare a list where each Drop is added multiple times based on its chance
        List<Drop> weightedList = new ArrayList<>();

        for (Drop drop : drops) {
            int entries = (int) Math.round(drop.chance());
            for (int i = 0; i < entries; i++) {
                weightedList.add(drop);
            }
        }

        if (weightedList.isEmpty())
            return null;

        // Shuffle to add randomness
        Collections.shuffle(weightedList);

        // Pick one random entry
        return weightedList.get(ThreadLocalRandom.current().nextInt(weightedList.size()));
    }

    @Nullable
    public Drop getDrop(String id) {
        return this.drops.stream()
                .filter(drop -> drop.id().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    public ItemStack createItem(int amount) {
        ItemBuilder builder = new ItemBuilder(this.item().clone());
        // apply data to the item
        builder.pdc(NextGens.generator_id, this.id());
        // set the amount
        ItemStack stack = builder.build();
        stack.setAmount(Math.max(1, amount));

        return stack;
    }

    public void addDrop(Drop drop) {
        this.drops.add(drop);
    }

}
