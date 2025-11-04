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
        if (items.size() < max) {
            this.pageItems.put(this.totalPage, items);
            return;
        }

        List<T> pages = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            // If the total added skins is over the max item amount
            // Put the skins into the map and "create a new page"
            if(i % max == 0 && i != 0){
                this.pageItems.put(this.totalPage, pages);
                pages = new ArrayList<>();
                this.totalPage++;
            }
            // If the leftover skins doesn't exceed the max amount
            if(i % max != 0 && i == (items.size() - 1)){
                this.pageItems.put(this.totalPage, pages);
            }
            pages.add(items.get(i));
        }
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
