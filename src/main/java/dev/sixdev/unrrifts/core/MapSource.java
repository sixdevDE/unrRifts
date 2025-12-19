package dev.sixdev.unrrifts.core;

/** Where a map lives. */
public enum MapSource {
    /** A region (bbox/boundary) inside a shared world. */
    SHARED_REGION,
    /** A whole dedicated world used as the map. */
    WORLD,
    /** Generated at runtime (kept for future/fallback). */
    GENERATED
}
