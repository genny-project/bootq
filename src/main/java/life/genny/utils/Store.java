package life.genny.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import life.genny.qwandaq.entity.BaseEntity;

public class Store {
    
    public static final Map<String, Map<String, BaseEntity>> defs = new ConcurrentHashMap<>();
}
