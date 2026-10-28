package com.muhammaddaffa.nextgens.gui.helpers;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Pagination<T> {

    private final Map<Integer, List<T>> pageItems = new HashMap<>();

    public int totalPage = 1;
    public int currentPage = 1;

    public Pagination(List<T> items, List<Integer> slots) {
        int max = slots.size();
        if (max == 0 || items.isEmpty()) {
            this.pageItems.put(1, new ArrayList<>());
            return;
        }
        for (int start = 0; start < items.size(); start += max) {
            this.pageItems.put(start / max + 1,
                    new ArrayList<>(items.subList(start, Math.min(start + max, items.size()))));
        }
        this.totalPage = this.pageItems.size();
    }

    public boolean hasNextPage() {
        return this.pageItems.get(this.currentPage + 1) != null;
    }

    public boolean hasPreviousPage() {
        return this.pageItems.get(this.currentPage - 1) != null;
    }

    @Nullable
    public List<T> getItems() {
        return this.pageItems.get(currentPage);
    }

    @Nullable
    public List<T> getItems(int page) {
        return this.pageItems.get(page);
    }

}
