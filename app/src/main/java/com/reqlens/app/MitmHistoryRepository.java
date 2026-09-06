package com.reqlens.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MitmHistoryRepository {
    private static final int MAX = 1000;
    private static final ArrayList<RequestRecord> ITEMS = new ArrayList<>();
    private MitmHistoryRepository() {}
    public static synchronized void add(RequestRecord r) {
        ITEMS.add(r);
        while (ITEMS.size() > MAX) ITEMS.remove(0);
        ProxyHistoryRepository.addHttp(r, "mitm");
    }
    public static synchronized List<RequestRecord> snapshotNewestFirst() {
        ArrayList<RequestRecord> out = new ArrayList<>(ITEMS);
        Collections.reverse(out);
        return out;
    }
    public static synchronized void clear() { ITEMS.clear(); }
    public static synchronized int size() { return ITEMS.size(); }
}
