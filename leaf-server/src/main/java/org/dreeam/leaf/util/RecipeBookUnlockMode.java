package org.dreeam.leaf.util;

public enum RecipeBookUnlockMode {

    DEFAULT,
    NO_OP,
    UNLOCK_ALL;

    public static RecipeBookUnlockMode fromString(String string) {
        for (RecipeBookUnlockMode mode : values()) {
            if (mode.name().equalsIgnoreCase(string)) {
                return mode;
            }
        }

        return null;
    }
}
