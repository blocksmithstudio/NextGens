import javax.tools.ToolProvider;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HologramProviderCheck {

    public static void main(String[] args) throws Exception {
        Path output = Files.createTempDirectory(Files.createDirectories(Path.of("target")), "hologram-check-");
        List<String> sources = new ArrayList<>();
        Map<String, String> substitutes = Map.of(
                "org.bukkit.Bukkit", """
                        package org.bukkit;
                        public class Bukkit {
                            public static final org.bukkit.plugin.PluginManager manager = new org.bukkit.plugin.PluginManager();
                            public static org.bukkit.plugin.PluginManager getPluginManager() { return manager; }
                        }
                        """,
                "org.bukkit.plugin.PluginManager", """
                        package org.bukkit.plugin;
                        public class PluginManager {
                            public final java.util.Set<String> enabled = new java.util.HashSet<>();
                            public boolean isPluginEnabled(String name) { return enabled.contains(name); }
                            public void disablePlugins() { throw new AssertionError("Must not disable other plugins"); }
                        }
                        """,
                "org.bukkit.Location", "package org.bukkit; public class Location {}",
                "org.bukkit.configuration.file.FileConfiguration", """
                        package org.bukkit.configuration.file;
                        public class FileConfiguration {
                            public String type;
                            public String getString(String key) { return type; }
                        }
                        """,
                "com.muhammaddaffa.mdlib.utils.Config", """
                        package com.muhammaddaffa.mdlib.utils;
                        public class Config {
                            private final org.bukkit.configuration.file.FileConfiguration config = new org.bukkit.configuration.file.FileConfiguration();
                            public org.bukkit.configuration.file.FileConfiguration getConfig() { return config; }
                        }
                        """,
                "com.muhammaddaffa.mdlib.utils.Logger", """
                        package com.muhammaddaffa.mdlib.utils;
                        public class Logger {
                            public static final java.util.List<String> warnings = new java.util.ArrayList<>();
                            public static void info(String message) {}
                            public static void warning(String message) { warnings.add(message); }
                        }
                        """,
                "com.muhammaddaffa.nextgens.NextGens", """
                        package com.muhammaddaffa.nextgens;
                        public class NextGens {
                            public static final com.muhammaddaffa.mdlib.utils.Config DEFAULT_CONFIG = new com.muhammaddaffa.mdlib.utils.Config();
                        }
                        """,
                "Check", """
                        import com.muhammaddaffa.nextgens.NextGens;
                        import com.muhammaddaffa.nextgens.hologram.HologramManager;
                        import com.muhammaddaffa.nextgens.hologram.HologramProvider;
                        import com.muhammaddaffa.mdlib.utils.Logger;
                        import org.bukkit.Bukkit;
                        public class Check {
                            public static final java.util.List<String> attempted = new java.util.ArrayList<>();
                            public static String broken;
                            public static void main(String[] args) {
                                NextGens.DEFAULT_CONFIG.getConfig().type = args[0].equals("null") ? null : args[0];
                                Bukkit.manager.enabled.addAll(java.util.List.of(args[1].split(",")));
                                broken = args[2];
                                if (args[0].equals("Custom")) {
                                    HologramManager.register(custom("Custom", "Custom", false));
                                }
                                if (args.length > 6) {
                                    switch (args[6]) {
                                        case "invalid" -> {
                                            HologramManager.register(null);
                                            HologramManager.register(custom(null, "Custom", false));
                                            HologramManager.register(custom("", "Custom", false));
                                            HologramManager.register(custom("Custom", "Other", false));
                                        }
                                        case "order" -> {
                                            HologramManager.register(custom("ZProvider", "ZProvider", false));
                                            HologramManager.register(custom("AProvider", "AProvider", false));
                                        }
                                        case "reverse" -> {
                                            HologramManager.register(custom("AProvider", "AProvider", false));
                                            HologramManager.register(custom("ZProvider", "ZProvider", false));
                                        }
                                        case "broken" -> HologramManager.register(custom("Broken", "Broken", true));
                                    }
                                }
                                HologramManager manager = new HologramManager();
                                manager.load();
                                String actual = manager.isEnabled() ? manager.getProvider().id() : "none";
                                if (!actual.equals(args[3])) {
                                    throw new AssertionError("Expected " + args[3] + ", got " + actual);
                                }
                                if (actual.equals("Custom") && !manager.getProvider().pluginName().equals("Custom")) {
                                    throw new AssertionError("Duplicate registration replaced the original provider");
                                }
                                String attempts = String.join(",", attempted);
                                if (!attempts.equals(args[4])) {
                                    throw new AssertionError("Unexpected provider loading: " + attempts);
                                }
                                boolean skipped = Logger.warnings.stream().anyMatch(line -> line.contains("skipping it"));
                                if (skipped != Boolean.parseBoolean(args[5])) {
                                    throw new AssertionError("Unexpected compatibility warnings: " + Logger.warnings);
                                }
                                if (actual.equals("none") && Logger.warnings.stream().noneMatch(line -> line.contains("No supported hologram"))) {
                                    throw new AssertionError("No-provider warning missing");
                                }
                            }
                            private static HologramProvider custom(String id, String plugin, boolean broken) {
                                return new HologramProvider() {
                                    public String id() { return id; }
                                    public String pluginName() { return plugin; }
                                    public boolean isAvailable() {
                                        if (broken) {
                                            throw new NoClassDefFoundError("missing/custom/API");
                                        }
                                        return HologramProvider.super.isAvailable();
                                    }
                                    public void spawn(String name, org.bukkit.Location location, java.util.List<String> lines) {}
                                    public void destroy(String name, org.bukkit.Location location) {}
                                };
                            }
                        }
                        """
        );
        for (var entry : substitutes.entrySet()) {
            sources.add(write(output, entry.getKey(), entry.getValue()));
        }
        for (String[] provider : List.of(
                new String[]{"DecentHologramsProvider", "DecentHolograms", "DecentHolograms"},
                new String[]{"FancyHologramProvider", "FancyHologram", "FancyHolograms"},
                new String[]{"HolographicDisplaysProvider", "HolographicDisplays", "HolographicDisplays"})) {
            String source = """
                    package com.muhammaddaffa.nextgens.hologram.imp;
                    import com.muhammaddaffa.nextgens.hologram.HologramProvider;
                    public class %s implements HologramProvider {
                        public %s() {
                            try {
                                Class<?> check = Class.forName("Check");
                                ((java.util.List<String>) check.getField("attempted").get(null)).add("%s");
                                if ("%s".equals(check.getField("broken").get(null))) {
                                    throw new NoClassDefFoundError("missing/provider/API");
                                }
                            } catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
                        }
                        public String id() { return "%s"; }
                        public String pluginName() { return "%s"; }
                        public void spawn(String name, org.bukkit.Location location, java.util.List<String> lines) {}
                        public void destroy(String name, org.bukkit.Location location) {}
                    }
                    """.formatted(provider[0], provider[0], provider[2], provider[2], provider[1], provider[2]);
            sources.add(write(output, "com.muhammaddaffa.nextgens.hologram.imp." + provider[0], source));
        }
        sources.add("src/main/java/com/muhammaddaffa/nextgens/hologram/HologramManager.java");
        sources.add("src/main/java/com/muhammaddaffa/nextgens/hologram/HologramProvider.java");
        List<String> compilerArgs = new ArrayList<>(List.of("-d", output.toString()));
        compilerArgs.addAll(sources);
        int result = ToolProvider.getSystemJavaCompiler().run(null, null, null, compilerArgs.toArray(String[]::new));
        if (result != 0) {
            throw new AssertionError("Regression check compilation failed: " + result);
        }
        String[][] cases = {
                {"DecentHolograms", "DecentHolograms,FancyHolograms", "FancyHolograms", "DecentHolograms", "DecentHolograms", "false"},
                {"DecentHolograms", "DecentHolograms", "", "DecentHolograms", "DecentHolograms", "false"},
                {"FancyHologram", "FancyHolograms,DecentHolograms", "", "FancyHologram", "FancyHolograms", "false"},
                {"fancyholograms", "FancyHolograms", "", "FancyHologram", "FancyHolograms", "false"},
                {"FancyHolograms", "FancyHolograms,DecentHolograms", "FancyHolograms", "DecentHolograms", "FancyHolograms,DecentHolograms", "true"},
                {"FancyHologram", "DecentHolograms", "", "DecentHolograms", "DecentHolograms", "false"},
                {"null", "DecentHolograms", "", "DecentHolograms", "DecentHolograms", "false"},
                {"", "HolographicDisplays", "", "HolographicDisplays", "HolographicDisplays", "false"},
                {"DecentHolograms", "", "", "none", "", "false"},
                {"FancyHologram", "FancyHolograms", "FancyHolograms", "none", "FancyHolograms", "true"},
                {"Custom", "Custom,DecentHolograms,FancyHolograms", "FancyHolograms", "Custom", "", "false"},
                {"Unknown", "DecentHolograms", "", "DecentHolograms", "DecentHolograms", "false"},
                {"", "DecentHolograms,FancyHolograms,HolographicDisplays", "FancyHolograms", "DecentHolograms", "DecentHolograms", "false"},
                {"Unknown", "FancyHolograms,HolographicDisplays", "", "FancyHologram", "FancyHolograms", "false"},
                {"Unknown", "DecentHolograms,FancyHolograms,HolographicDisplays", "DecentHolograms", "FancyHologram", "DecentHolograms,FancyHolograms", "true"},
                {"null", "", "", "none", "", "false"},
                {"HolographicDisplays", "DecentHolograms,FancyHolograms", "", "DecentHolograms", "DecentHolograms", "false"},
                {"Custom", "Custom,Other,DecentHolograms", "", "Custom", "", "false", "invalid"},
                {"Unknown", "ZProvider,AProvider,DecentHolograms", "", "ZProvider", "", "false", "order"},
                {"Unknown", "ZProvider,AProvider,DecentHolograms", "", "AProvider", "", "false", "reverse"},
                {"Broken", "Broken,DecentHolograms", "", "DecentHolograms", "DecentHolograms", "true", "broken"}
        };
        for (String[] scenario : cases) {
            try (URLClassLoader loader = new URLClassLoader(new URL[]{output.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
                loader.loadClass("Check").getMethod("main", String[].class).invoke(null, (Object) scenario);
            }
        }
        System.out.println("Passed " + cases.length + " hologram provider selection checks.");
    }

    private static String write(Path output, String name, String source) throws Exception {
        Path file = output.resolve(name.replace('.', '/') + ".java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, source);
        return file.toString();
    }
}
