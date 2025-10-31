package com.zerwhit.core.module;

public class Module {
    String name;
    boolean enabled;
    String category;

    Module(String name, boolean enabled, String category) {
        this.name = name;
        this.enabled = enabled;
        this.category = category;
    }
}