package com.reqlens.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BrowserHistoryRepository {
    private static final int MAX = 1500;
    private static final ArrayList<BrowserRequest> ITEMS = new ArrayList<>();
    private BrowserHistoryRepository() { }

    public static synchronized void add(BrowserRequest item) {
        ITEMS.add(item);
        while (ITEMS.size() > MAX) ITEMS.remove(0);
        ProxyHistoryRepository.addHttp(item.toRequestRecord(), "browser");
    }

    public static synchronized List<BrowserRequest> snapshotNewestFirst() {
        ArrayList<BrowserRequest> out = new ArrayList<>(ITEMS);
        Collections.reverse(out);
        return out;
    }

    public static synchronized int size() { return ITEMS.size(); }
    public static synchronized void clear() { ITEMS.clear(); }
}
