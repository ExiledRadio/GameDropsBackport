package com.exiledradio.gamedropsbackport.selftest;

import com.exiledradio.gamedropsbackport.GameDropsBackport;

import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Assertions for the self-tests. Streams add their own helpers (scratch areas, fake players) when they need them. */
public final class Check {

    public final WorldServer world;

    private final List<String> failures = new ArrayList<String>();
    private int passed;
    private String section = "";

    Check(WorldServer world) {
        this.world = world;
    }

    void section(String name) {
        this.section = name;
        GameDropsBackport.LOGGER.info("SELFTEST --- {} ---", name);
    }

    public boolean ok(boolean condition, String what) {
        if (condition) {
            this.passed++;
        } else {
            fail(what);
        }
        return condition;
    }

    public boolean eq(Object expected, Object actual, String what) {
        return ok(Objects.equals(expected, actual), what + ": expected " + expected + ", got " + actual);
    }

    public void fail(String what) {
        String line = "[" + this.section + "] " + what;
        this.failures.add(line);
        GameDropsBackport.LOGGER.error("SELFTEST FAIL {}", line);
    }

    int passed() {
        return this.passed;
    }

    int failed() {
        return this.failures.size();
    }

    List<String> failures() {
        return this.failures;
    }
}
