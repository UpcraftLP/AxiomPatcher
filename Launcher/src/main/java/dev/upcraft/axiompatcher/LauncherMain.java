package dev.upcraft.axiompatcher;

import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class LauncherMain {

    public static void main(String[] args) {
        String javaVersion = System.getProperty("java.version");
        int javaVersionNumber;
        String[] split = javaVersion.split("\\.", 3);
        if (javaVersion.startsWith("1.")) {
            javaVersionNumber = Integer.parseInt(split[1]);
        } else {
            javaVersionNumber = Integer.parseInt(split[0]);
        }

        if (javaVersionNumber < 17) {
            System.out.println("ERROR: You need to have Java 17 or higher installed to run this application.");
            JOptionPane.showMessageDialog(null, "You need to have Java 17 or higher installed to run this application.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        } else {
            try {
                Class.forName("dev.upcraft.axiompatcher.Main").getDeclaredMethod("main", String[].class).invoke(null, (Object) args);
            } catch (Exception e) {
                throw new RuntimeException("Unable to launch Axiom Patcher UI", e);
            }
        }
    }

    @SuppressWarnings("unused")
    public static void notAMinecraftMod() {
        LoggerFactory.getLogger(LauncherMain.class).error("AxiomPatcher is not a Minecraft mod, it must be run as standalone application.");
        if(!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(null, "AxiomPatcher is not a Minecraft mod, it must be run as standalone application.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        System.exit(1);
    }
}
