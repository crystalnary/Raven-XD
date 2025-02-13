package keystrokesmod.utility;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.realmsclient.gui.ChatFormatting;
import keystrokesmod.Client;
import keystrokesmod.clickgui.ClickGui;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.impl.exploit.ClientSpoofer;
import keystrokesmod.module.impl.minigames.DuelsStats;
import keystrokesmod.module.impl.other.FakeChat;
import keystrokesmod.module.impl.other.KillMessage;
import keystrokesmod.module.impl.other.NameHider;
import keystrokesmod.module.impl.render.Watermark;
import keystrokesmod.utility.font.IFont;
import keystrokesmod.utility.profile.Profile;
import keystrokesmod.utility.profile.ProfileManager;
import keystrokesmod.utility.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.network.play.client.C01PacketChatMessage;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class Commands {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static boolean firstRun = true;
    private static final List<Integer> colors = Arrays.asList(
            (new Color(170, 107, 148, 50)).getRGB(),
            (new Color(122, 158, 134, 50)).getRGB(),
            (new Color(16, 16, 16, 50)).getRGB(),
            (new Color(64, 114, 148, 50)).getRGB()
    );
    private static int currentColorIndex = 0;
    private static int lastColorIndex = -1;
    public static List<String> recentMessages = new ArrayList<>();
    private static final String INVALID_SYNTAX_BASE = "&cInvalid syntax. Use: &e";
    private static final String INVALID_COMMAND = "&cInvalid command.";

    private static final Map<String, CommandHandler> commandHandlers = new HashMap<>();

    static {
        registerCommands();
    }

    private static void registerCommands() {
        commandHandlers.put("setkey", Commands::handleSetKey);
        commandHandlers.put("nick", Commands::handleNick);
        commandHandlers.put("cname", Commands::handleCName);
        commandHandlers.put(FakeChat.command, Commands::handleFakeChat);
        commandHandlers.put("duels", Commands::handleDuels);
        commandHandlers.put("ping", (args) -> Ping.checkPing());
        commandHandlers.put("clear", (args) -> recentMessages.clear());
        commandHandlers.put("hide", Commands::handleHide);
        commandHandlers.put("show", Commands::handleShow);
        commandHandlers.put("panic", Commands::handlePanic);
        commandHandlers.put("rename", Commands::handleRename);
        commandHandlers.put("resetgui", (args) -> {
            ClickGui.resetPosition();
            print(ChatFormatting.GREEN + "Reset ClickGUI position!", 1);
        });
        commandHandlers.put("folder", (args) -> {
            File folder = new File(Client.mc.mcDataDir, "keystrokes");
            try {
                Desktop.getDesktop().open(folder);
            } catch (IOException ex) {
                boolean created = folder.mkdir();
                Utils.sendMessage(created ? "Folder created." : "Failed to create folder.");
            }
        });
        commandHandlers.put("update", (args) -> Client.getExecutor().execute(AutoUpdate::update));
        commandHandlers.put("say", Commands::handleSay);
        commandHandlers.put("clientname", Commands::handleClientName);
        commandHandlers.put("killmessage", Commands::handleKillMessage);
        commandHandlers.put("clientspoofer", Commands::handleClientSpoofer);
        commandHandlers.put("binds", Commands::handleBinds);
        commandHandlers.put("bind", Commands::handleBind);
        commandHandlers.put("import", Commands::handleImport);
        commandHandlers.put("export", Commands::handleExport);
        commandHandlers.put("friend", Commands::handleFriend);
        commandHandlers.put("f", Commands::handleFriend);
        commandHandlers.put("enemy", Commands::handleEnemy);
        commandHandlers.put("e", Commands::handleEnemy);
        commandHandlers.put("debug", (args) -> {
            Client.debugger = !Client.debugger;
            print("Debug " + (Client.debugger ? "enabled" : "disabled") + ".", 1);
        });
        commandHandlers.put("profiles", Commands::handleProfiles);
        commandHandlers.put("p", Commands::handleProfiles);
        commandHandlers.put("chat", Commands::handleChat);
        commandHandlers.put("help", Commands::handleHelp);
        commandHandlers.put("?", Commands::handleHelp);
        commandHandlers.put("shoutout", (args) -> {
            print("&eCelebrities:", 1);
            print("- hevex", 0);
            print("- jc", 0);
            print("- mood", 0);
            print("- charlotte", 0);
        });
    }


    public static void rCMD(@NotNull String command) {
        if (command.isEmpty()) {
            return;
        }

        List<String> args = Arrays.asList(command.split(" "));
        String commandName = args.get(0).toLowerCase();

        CommandHandler handler = commandHandlers.get(commandName);
        if (handler != null) {
            handler.execute(args);
        } else {
            String closestCommand = findClosestCommand(commandName);
            if (closestCommand != null) {
                print(INVALID_COMMAND + " Did you mean: &e" + closestCommand + "&c?", 1);
            } else {
                print(INVALID_COMMAND + " (" + (commandName.length() > 5 ? commandName.substring(0, 5) + "..." : commandName) + ")", 1);
            }
        }
    }

    private static String findClosestCommand(String input) {
        String closest = null;
        int minDistance = Integer.MAX_VALUE;

        for (String command : commandHandlers.keySet()) {
            int distance = levenshteinDistance(input, command);
            if (distance < minDistance) {
                minDistance = distance;
                closest = command;
            }
        }

        if (minDistance <= 2) {
            return closest;
        } else {
            return null;
        }
    }

    private static int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }

        return dp[s1.length()][s2.length()];
    }

    private static void handleSetKey(List<String> args) {
        if (args.size() != 2) {
            print(INVALID_SYNTAX_BASE + "setkey [key]", 1);
            return;
        }

        print("Setting...", 1);
        String key = args.get(1);
        Client.getExecutor().execute(() -> {
            if (URLUtils.isHypixelKeyValid(key)) {
                URLUtils.k = key;
                print("&a" + "success!", 0);
            } else {
                print("&c" + "Invalid key.", 0);
            }
        });
    }

    private static void handleNick(List<String> args) {
        if (args.size() != 2) {
            print(INVALID_SYNTAX_BASE + "nick [name/reset]", 1);
            return;
        }

        if (args.get(1).equals("reset")) {
            DuelsStats.nick = null;
            print("&aNick reset.", 1);
            return;
        }

        DuelsStats.nick = args.get(1);
        print("&aNick has been set to:", 1);
        print("\"" + DuelsStats.nick + "\"", 0);
    }

    private static void handleCName(List<String> args) {
        if (args.size() < 2) {
            print(INVALID_SYNTAX_BASE + "cname [name]", 1);
            return;
        }

        StringBuilder nameBuilder = new StringBuilder(args.get(1).replace("&", "§"));
        for (int i = 2; i < args.size(); i++) {
            nameBuilder.append(" ").append(args.get(i).replace("&", "§"));
        }
        NameHider.n = nameBuilder.toString();

        print("&a" + Utils.uf("name") + "Nick has been set to:".substring(4), 1);
        print("\"" + NameHider.n + "\"", 0);
    }

    private static void handleFakeChat(List<String> args) {
        if (args.size() < 2) {
            print(INVALID_SYNTAX_BASE + FakeChat.command + " [msg]", 1);
            return;
        }

        String message = String.join(" ", args.subList(1, args.size()));
        if (message.isEmpty() || message.equals("\\n")) {
            print(FakeChat.c4, 1);
            return;
        }

        FakeChat.msg = message;
        print("&aMessage set!", 1);
    }

    private static void handleDuels(List<String> args) {
        if (args.size() != 2) {
            print(INVALID_SYNTAX_BASE + "duels [player]", 1);
            return;
        }

        if (URLUtils.k.isEmpty()) {
            print("&cAPI Key is empty!", 1);
            print("Use \"setkey [api_key)\".", 0);
            return;
        }

        String playerName = args.get(1);
        print("Retrieving data...", 1);
        Client.getExecutor().execute(() -> {
            int[] stats = ProfileUtils.getHypixelStats(playerName, ProfileUtils.DM.OVERALL);
            if (stats != null) {
                if (stats[0] == -1) {
                    print("&c" + (playerName.length() > 16 ? playerName.substring(0, 16) + "..." : playerName) + " does not exist!", 0);
                } else {
                    double wlr = stats[1] != 0 ? Utils.rnd((double) stats[0] / (double) stats[1], 2) : (double) stats[0];
                    print("&e" + playerName + " stats:", 1);
                    print("Wins: " + stats[0], 0);
                    print("Losses: " + stats[1], 0);
                    print("WLR: " + wlr, 0);
                    print("Winstreak: " + stats[2], 0);
                    print("Threat: " + DuelsStats.gtl(stats[0], stats[1], wlr, stats[2]).substring(2), 0);
                }
            } else {
                print("&cThere was an error.", 0);
            }
        });
    }

    private static void handleHide(List<String> args) {
        handleModuleVisibility(args, true);
    }

    private static void handleShow(List<String> args) {
        handleModuleVisibility(args, false);
    }

    private static void handleModuleVisibility(List<String> args, boolean hide) {
        if (args.size() != 2) {
            print(INVALID_SYNTAX_BASE + (hide ? "hide" : "show") + " [module]", 1);
            return;
        }

        String moduleName = args.get(1).toLowerCase();
        Optional<Module> targetModule = Client.getModuleManager().getModules().stream()
                .filter(module -> module.getName().toLowerCase().replace(" ", "").equals(moduleName))
                .findFirst();

        if (targetModule.isPresent()) {
            Module module = targetModule.get();
            module.setHidden(hide);
            print("&a" + module.getName() + " is now " + (hide ? "hidden" : "visible") + " in HUD", 1);
        } else {
            print("&cModule not found.", 1);
        }
    }

    private static void handlePanic(List<String> args) {
        List<Module> modulesToDisable = new ArrayList<>();
        for (Module m : Client.getModuleManager().getModules()) {
            if (m.isEnabled()) {
                modulesToDisable.add(m);
            }
        }
        for (Module m : modulesToDisable) {
            m.disable();
        }
    }

    private static void handleRename(List<String> args) {
        if (args.size() != 3 && args.size() != 4) {
            print(INVALID_SYNTAX_BASE + "rename [module] [name] <info>", 1);
            return;
        }
        String moduleName = args.get(1).toLowerCase();
        Optional<Module> targetModule = Client.getModuleManager().getModules().stream()
                .filter(module -> module.getName().toLowerCase().replace(" ", "").equals(moduleName))
                .findFirst();

        if (targetModule.isPresent()) {
            Module module = targetModule.get();
            module.setPrettyName(args.get(2));
            if (args.size() == 4) {
                module.setPrettyInfo(args.get(3));
                print("&a'" + module.getName() + " " + module.getInfo() + "' is now called '" + module.getRawPrettyName() + " " + module.getRawPrettyInfo() + "'", 1);
            } else {
                print("&a" + module.getName() + " is now called " + module.getRawPrettyName(), 1);
            }
        } else {
            print("&cModule not found.", 1);
        }
    }

    private static void handleSay(List<String> args) {
        if (args.size() < 2) {
            print(INVALID_SYNTAX_BASE + "say [message]", 1);
            return;
        }

        String message = String.join(" ", args.subList(1, args.size()));
        PacketUtils.sendPacketNoEvent(new C01PacketChatMessage(message));
    }

    private static void handleClientName(List<String> args) {
        if (args.size() < 2) {
            print(INVALID_SYNTAX_BASE + "clientname [name]", 1);
            return;
        }

        Watermark.customName = String.join(" ", args.subList(1, args.size())).replace('&', '§');
        print("&aSet client name to " + Watermark.customName, 1);
    }

    private static void handleKillMessage(List<String> args) {
        if (args.size() < 2) {
            print(INVALID_SYNTAX_BASE + "killmessage [message]", 1);
            return;
        }

        KillMessage.killMessage = String.join(" ", args.subList(1, args.size()));
        print("&aSet killmessage to " + KillMessage.killMessage, 1);
    }

    private static void handleClientSpoofer(List<String> args) {
        if (args.size() < 2) {
            print(INVALID_SYNTAX_BASE + "clientspoofer [brand]", 1);
            return;
        }

        ClientSpoofer.customBrand = String.join(" ", args.subList(1, args.size()));
        print("&aSet clientspoofer custom brand to " + ClientSpoofer.customBrand, 1);
    }

    private static void handleBinds(List<String> args) {
        if (args.size() != 1) {
            print(INVALID_SYNTAX_BASE + "binds", 1);
            return;
        }
        for (Module module : Client.getModuleManager().getModules()) {
            if (module.getKeycode() != 0) {
                print(ChatFormatting.AQUA + module.getPrettyName() + ": " + Utils.getKeyName(module.getKeycode()), 1);
            }
        }
    }

    private static void handleBind(List<String> args) {
        if (args.size() != 3) {
            print(INVALID_SYNTAX_BASE + "bind [module] [key]", 1);
            return;
        }

        Module targetModule = Client.getModuleManager().getModules().stream()
                .filter(module -> Objects.equals(module.getName(), args.get(1)))
                .findFirst()
                .orElse(null);

        if (targetModule == null) {
            print(ChatFormatting.RED + "Module '" + ChatFormatting.RESET + args.get(1) + ChatFormatting.RED + "' is not found.", 1);
            return;
        }

        int keyCode = Utils.getKeyCode(args.get(2));
        if (keyCode == Keyboard.KEY_NONE) {
            print(ChatFormatting.RED + "Key '" + ChatFormatting.RESET + args.get(2) + ChatFormatting.RED + "' is invalid.", 1);
            return;
        }

        targetModule.setBind(keyCode);
        print(ChatFormatting.GREEN + "Bind '" + ChatFormatting.RESET + args.get(2) + ChatFormatting.GREEN + "' to " + targetModule.getPrettyName() + ".", 1);
    }

    private static void handleImport(List<String> args) {
        if (args.size() != 2) {
            print(INVALID_SYNTAX_BASE + "import [module]", 1);
            return;
        }

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        if (contents == null || !contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            print("Clipboard does not contain valid data.", 1);
            return;
        }

        Module targetModule = Client.getModuleManager().getModules().stream()
                .filter(module -> Objects.equals(module.getPrettyName(), args.get(1)))
                .findFirst().orElse(null);

        if (targetModule == null) {
            print(ChatFormatting.RED + "Module '" + ChatFormatting.RESET + args.get(1) + ChatFormatting.RED + "' is not found.", 1);
            return;
        }

        try {
            JsonObject jsonObject = new Gson().fromJson(((String) contents.getTransferData(DataFlavor.stringFlavor)), JsonObject.class);
            ProfileManager.loadFromJsonObject(jsonObject, targetModule);
            print("Loaded " + jsonObject.entrySet().size() + " properties from clipboard.", 1);
        } catch (Exception e) {
            print("Fail to import module settings.", 1);
        }
    }

    private static void handleExport(List<String> args) {
        if (args.size() != 2) {
            print(INVALID_SYNTAX_BASE + "export [module]", 1);
            return;
        }

        Module targetModule = Client.getModuleManager().getModules().stream()
                .filter(module -> Objects.equals(module.getPrettyName(), args.get(1)))
                .findFirst().orElse(null);

        if (targetModule == null) {
            print(ChatFormatting.RED + "Module '" + ChatFormatting.RESET + args.get(1) + ChatFormatting.RED + "' is not found.", 1);
            return;
        }

        try {
            JsonObject jsonObject = ProfileManager.getJsonObject(targetModule);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(jsonObject.toString()), null);
            print("Copied " + jsonObject.entrySet().size() + " properties to clipboard.", 1);
        } catch (Exception e) {
            print("Fail to export module settings.", 1);
        }
    }

    private static void handleFriend(List<String> args) {
        handleFriendEnemy(args, true);
    }

    private static void handleEnemy(List<String> args) {
        handleFriendEnemy(args, false);
    }

    private static void handleFriendEnemy(List<String> args, boolean friend) {
        if (args.size() != 2) {
            print(INVALID_SYNTAX_BASE + (friend ? "friend" : "enemy") + " [name/clear/list]", 1);
            return;
        }

        String action = args.get(1);
        HashSet<String> list = friend ? Utils.friends : Utils.enemies;

        if (action.equalsIgnoreCase("clear")) {
            list.clear();
            print("&a" + (friend ? "Friends" : "Enemies") + " cleared.", 1);
            return;
        } else if (action.equalsIgnoreCase("list")) {
            print("&a" + (friend ? "Friends" : "Enemies") + ":", 1);
            if (list.isEmpty()) {
                print("None", 0);
            } else {
                if (mc.theWorld != null) {
                    list.forEach(name -> {
                        boolean isOnline = mc.theWorld.playerEntities.stream()
                                .anyMatch(player -> player.getName().equalsIgnoreCase(name));
                        print(String.format("- &a%s%s", name, isOnline ? "&l" : ""), 0);
                    });
                } else {
                    list.forEach(name -> print(String.format("- &a%s", name), 0));
                }
            }
            return;
        }

        String name = args.get(1);
        String lowerName = name.toLowerCase();
        boolean wasPresent = list.contains(lowerName);

        if (wasPresent) {
            list.remove(lowerName);
            print("&aRemoved " + (friend ? "friend" : "enemy") + ": " + name, 1);
        } else {
            list.add(lowerName);
            print("&aAdded " + (friend ? "friend" : "enemy") + ": " + name, 1);
        }
    }

    private static void handleProfiles(List<String> args) {
        if (args.size() == 1) {
            print("&aAvailable profiles:", 1);
            if (Client.profileManager.profiles.isEmpty()) {
                print("None", 0);
            } else {
                for (int i = 0; i < Client.profileManager.profiles.size(); ++i) {
                    print(i + 1 + ". " + Client.profileManager.profiles.get(i).getName(), 0);
                }
            }
        } else {
            String action = args.get(1).toLowerCase();
            switch (action) {
                case "save":
                case "s":
                    if (args.size() != 3) {
                        print(INVALID_SYNTAX_BASE + "profiles save [profile]", 1);
                        return;
                    }
                    String name = args.get(2);
                    if (name.length() < 2 || name.length() > 10 || !name.chars().allMatch(Character::isLetterOrDigit)) {
                        print("&cInvalid name.", 1);
                        return;
                    }
                    Client.profileManager.saveProfile(new Profile(name, 0));
                    print("&aSaved profile:", 1);
                    print(name, 0);
                    Client.profileManager.loadProfiles();
                    break;

                case "load":
                case "l":
                    if (args.size() != 3) {
                        print(INVALID_SYNTAX_BASE + "profiles load [profile]", 1);
                        return;
                    }
                    String profileToLoad = args.get(2);
                    Optional<Profile> foundProfile = Client.profileManager.profiles.stream()
                            .filter(profile -> profile.getName().equals(profileToLoad))
                            .findFirst();

                    if (foundProfile.isPresent()) {
                        Profile profile = foundProfile.get();
                        Client.profileManager.loadProfile(profile.getName());
                        print("&aLoaded profile:", 1);
                        print(profileToLoad, 0);
                        if (Settings.sendMessage.isToggled()) {
                            Utils.sendMessage("&7Enabled profile: &b" + profileToLoad);
                        }
                    } else {
                        print("&cInvalid profile.", 1);
                    }
                    break;
                case "remove":
                case "r":
                    if (args.size() != 3) {
                        print(INVALID_SYNTAX_BASE + "profiles remove [profile]", 1);
                        return;
                    }
                    String profileToRemove = args.get(2);
                    if (Client.profileManager.profiles.removeIf(profile -> profile.getName().equals(profileToRemove))) {
                        print("&aRemoved profile:", 1);
                        print(profileToRemove, 0);
                        Client.profileManager.loadProfiles();
                    } else {
                        print("&cInvalid profile.", 1);
                    }
                    break;

                default:
                    print(INVALID_SYNTAX_BASE + "profiles [save/load/remove] [profile]", 1);
            }
        }
    }

    private static void handleChat(List<String> args) {
        if (args.size() < 2) {
            print(INVALID_SYNTAX_BASE + "chat <args>", 1);
            return;
        }

        String message = String.join(" ", args.subList(1, args.size()));
        ModuleManager.chatAI.onChat(message);
    }

    private static void handleHelp(List<String> args) {
        print("&eAvailable commands:", 1);
        print("1 setkey [key]", 0);
        print("2 friend/enemy [name/clear]", 0);
        print("3 duels [player]", 0);
        print("4 nick [name/reset]", 0);
        print("5 ping", 0);
        print("6 hide/show [module]", 0);
        print("7 rename [module] [name] <info>", 0);
        print("8 say [message]", 0);
        print("9 panic", 0);
        print("10 resetGUI", 0);
        print("11 folder", 0);
        print("&eProfiles:", 0);
        print("1 profiles", 0);
        print("2 profiles save [profile]", 0);
        print("3 profiles load [profile]", 0);
        print("4 profiles remove [profile]", 0);
        print("5 binds", 0);
        print("6 bind [module] [key]", 0);
        print("7 import [module]", 0);
        print("8 export [module]", 0);
        print("&eModule-specific:", 0);
        print("1 cname [name]", 0);
        print("2 " + FakeChat.command + " [msg]", 0);
        print("4 killmessage [message]", 0);
        print("4 clientspoofer [brand]", 0);
        print(String.format("5 clientname [name (current is '%s')]", Watermark.customName), 0);
        print("6 chat <args>", 0);
    }


    public static void print(String message, int type) {
        if (mc.currentScreen instanceof GuiChat || mc.currentScreen == null) {
            if (type == 1 || type == 2) {
                Utils.sendRawMessage("");
            }
            Utils.sendMessage(message);
            if (type == 2 || type == 3) {
                Utils.sendRawMessage("");
            }
        } else {
            if (type == 1 || type == 2) {
                recentMessages.add("");
            }
            recentMessages.add(message);
            if (type == 2 || type == 3) {
                recentMessages.add("");
            }
        }
    }

    public static void rc(IFont fontRenderer, int height, int width, int scale) {
        int x = width - 195;
        int y = height - 130;
        int startY = height - 345;
        int scissorHeight = 230;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        int scaledWidth = width * scale;
        GL11.glScissor(0, mc.displayHeight - (startY + scissorHeight) * scale, scaledWidth - (scaledWidth < 2 ? 0 : 2), scissorHeight * scale - 2);

        RenderUtils.db(1000, 1000, currentColorIndex);
        renderRecentMessages(fontRenderer, recentMessages, x, y);

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private static void renderRecentMessages(IFont fontRenderer, List<String> messages, int x, int y) {
        if (firstRun) {
            firstRun = false;
            print("Welcome,", 0);
            print("Use \"help\" for help.", 0);
        }

        if (!messages.isEmpty()) {
            for (int i = messages.size() - 1; i >= 0; --i) {
                String message = messages.get(i);
                int color = -1;

                if (message.contains("&a")) {
                    message = message.replace("&a", "");
                    color = Color.green.getRGB();
                } else if (message.contains("&c")) {
                    message = message.replace("&c", "");
                    color = Color.red.getRGB();
                } else if (message.contains("&e")) {
                    message = message.replace("&e", "");
                    color = Color.yellow.getRGB();
                }

                fontRenderer.drawString(message, x, y, color);
                y -= (int) Math.round(fontRenderer.height() + 5);
            }
        }
    }


    public static void setccs() {
        int value = Utils.getRandom().nextInt(colors.size());
        if (value == lastColorIndex) {
            value += value == 3 ? -3 : 1;
        }

        lastColorIndex = value;
        currentColorIndex = colors.get(value);
    }

    public static void od() {
        Ping.rs();
    }

    @FunctionalInterface
    private interface CommandHandler {
        void execute(List<String> args);
    }
}