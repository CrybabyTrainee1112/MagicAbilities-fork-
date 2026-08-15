package net.trduc.magicabilitiesfork;

import net.trduc.magicabilitiesfork.Boss.bosses.demonlord.DemonLordBossType;
import net.trduc.magicabilitiesfork.Boss.core.BossFactory;
import net.trduc.magicabilitiesfork.Boss.core.BossManager;
import net.trduc.magicabilitiesfork.Boss.spawn.FortressBossSpawner;
import net.trduc.magicabilitiesfork.Boss.event.BossEventListener;
import net.trduc.magicabilitiesfork.Boss.mastery.BossMasteryStore;
import net.trduc.magicabilitiesfork.commands.*;
import net.trduc.magicabilitiesfork.cooldowns.Cooldowns;
import net.trduc.magicabilitiesfork.data.DataEventsHandler;
import net.trduc.magicabilitiesfork.data.DbManager;
import net.trduc.magicabilitiesfork.data.PlayerData;
import net.trduc.magicabilitiesfork.events.ExecutionEvents;
import net.trduc.magicabilitiesfork.guis.AnimationManager;
import net.trduc.magicabilitiesfork.guis.GuiManager;
import net.trduc.magicabilitiesfork.intrinsics.IntrinsicDropListener;
import net.trduc.magicabilitiesfork.intrinsics.gui.IntrinsicGui;
import net.trduc.magicabilitiesfork.intrinsics.item.IntrinsicBookRecipes;
import net.trduc.magicabilitiesfork.intrinsics.IntrinsicListener;
import net.trduc.magicabilitiesfork.intrinsics.IntrinsicManager;
import net.trduc.magicabilitiesfork.intrinsics.player.PlayerIntrinsicStorage;
import net.trduc.magicabilitiesfork.commands.PowerteamOwnerCommands;
import net.trduc.magicabilitiesfork.misc.ConfigMigrator;
import net.trduc.magicabilitiesfork.misc.CooldownValidator;
import net.trduc.magicabilitiesfork.misc.ParticleApi;
import net.trduc.magicabilitiesfork.misc.DisplayApi;
import net.trduc.magicabilitiesfork.players.PowerPlayer;
import net.trduc.magicabilitiesfork.powers.Power;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.logging.Logger;

import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;

public final class MagicAbilitiesfork extends JavaPlugin {

    private static final Logger log = Logger.getLogger("Minecraft");
    public static ParticleApi particleApi;
    public static DisplayApi displayApi;
    private DbManager dbManager;
    private static FileConfiguration config;
    public static MagicAbilitiesfork magicPlugin;
    public net.trduc.magicabilitiesfork.guis.PowerTeamGui powerTeamGui;
    public net.trduc.magicabilitiesfork.guis.PowerTeamListGui powerTeamListGui;
    private BossManager bossManager;
    private BossFactory bossFactory;
    private FortressBossSpawner fortressBossSpawner;
    private BossMasteryStore bossMasteryStore;
    private BukkitTask bossTickTask;
    private IntrinsicManager intrinsicManager;
    private BukkitTask intrinsicTickTask;
    private IntrinsicListener intrinsicListener;
    private PlayerIntrinsicStorage playerIntrinsicStorage;
    private IntrinsicGui intrinsicGui;
    private BukkitTask intrinsicPruneTask;

    @Override
    public void onEnable() {
        magicPlugin = this;
        saveDefaultConfig();
        ConfigMigrator.migrate(this);
        config = getConfig();

        int pluginId = 32200;
        Metrics metrics = new Metrics(this, pluginId);

        Cooldowns cdInstance = new Cooldowns(createCooldownsConfig());
        CooldownValidator.validate(this, cdInstance);
        net.trduc.magicabilitiesfork.powers.RandomPowerAssigner.loadPool(createRandomPowersConfig());
        particleApi = new ParticleApi(this);
        displayApi = new DisplayApi(this);
        dbManager = new DbManager(this);
        dbManager.init();
        checkDb(dbManager);
        setPlayerData(dbManager);
        ExecutionEvents executionEvents = new ExecutionEvents();
        getServer().getPluginManager().registerEvents(executionEvents, this);
        getServer().getPluginManager().registerEvents(new net.trduc.magicabilitiesfork.events.PowerTeamEvents(dbManager), this);

        net.trduc.magicabilitiesfork.guis.PowerTeamGui ptGui = new net.trduc.magicabilitiesfork.guis.PowerTeamGui(dbManager);
        this.powerTeamGui = ptGui;
        getServer().getPluginManager().registerEvents(ptGui, this);

        net.trduc.magicabilitiesfork.guis.PowerTeamListGui ptListGui = new net.trduc.magicabilitiesfork.guis.PowerTeamListGui(dbManager);
        this.powerTeamListGui = ptListGui;
        getServer().getPluginManager().registerEvents(ptListGui, this);

        net.trduc.magicabilitiesfork.misc.TeamColorSync.syncAllOnline(dbManager);
        registerCommand(new Binds(), "binds");
        registerCommand(new Destination(), "destination");
        registerCommand(new Setpower(), "setpower");
        registerCommand(new Enable(), "enable");
        registerCommand(new Disable(), "disable");
        registerCommand(new Powerset(), "powerset");
        registerCommand(new Powersetaura(), "powersetaura");
        registerCommand(new Combos(), "combos");
        registerCommand(new PowerTeamCommand(), "powerteam");
        registerCommand(new PowerteamOwnerCommands(), "powerteamowner");
        final GuiManager guiManager = new GuiManager(this);
        final AnimationManager animationManager = new AnimationManager(this, guiManager);

        setupBossSystem();
        registerCommand(new PowerBossCommand(), "powerboss");

        setupIntrinsicSystem();
        registerCommand(new net.trduc.magicabilitiesfork.commands.IntrinsicCommand(intrinsicManager, playerIntrinsicStorage), "intrinsic");

        getServer().getPluginManager().registerEvents(
                new DataEventsHandler(dbManager, executionEvents, intrinsicManager, playerIntrinsicStorage), this);

        particleApi.spawnParticles(Bukkit.getWorlds().get(0).getSpawnLocation(), Particle.ASH, 1, 1, 1, 1, 1);
    }

    private void setupIntrinsicSystem() {
        intrinsicManager = new IntrinsicManager();
        intrinsicListener = new IntrinsicListener(intrinsicManager);
        getServer().getPluginManager().registerEvents(intrinsicListener, this);

        playerIntrinsicStorage = new PlayerIntrinsicStorage(this);
        playerIntrinsicStorage.init();
        IntrinsicBookRecipes.registerAll(this);
        intrinsicGui = new IntrinsicGui(this, intrinsicManager, playerIntrinsicStorage);
        getServer().getPluginManager().registerEvents(intrinsicGui, this);
        getServer().getPluginManager().registerEvents(
                new IntrinsicDropListener(this, playerIntrinsicStorage, bossMasteryStore, intrinsicGui), this);
        registerCommand(new MyIntrinsicsCommand(intrinsicManager, playerIntrinsicStorage, intrinsicGui), "myintrinsics");

        BukkitRunnable tickRunnable = new BukkitRunnable() {
            @Override
            public void run() {
                intrinsicManager.tickAll();
            }
        };
        intrinsicTickTask = tickRunnable.runTaskTimer(this, 20L, 20L);

        BukkitRunnable pruneRunnable = new BukkitRunnable() {
            @Override
            public void run() {
                intrinsicListener.pruneStaleDamagers();
            }
        };
        intrinsicPruneTask = pruneRunnable.runTaskTimer(this, 20L * 60 * 5, 20L * 60 * 5);
    }

    private void setupBossSystem() {
        bossManager = new BossManager();
        bossMasteryStore = new BossMasteryStore(this);
        bossMasteryStore.init();
        bossFactory = new BossFactory(bossManager, bossMasteryStore, getConfig());

        boolean debug = getConfig().getBoolean("debug", false);
        net.trduc.magicabilitiesfork.Boss.core.Boss.setDebugEnabled(debug);
        net.trduc.magicabilitiesfork.Boss.ai.executor.Brain.setDebugEnabled(debug);
        net.trduc.magicabilitiesfork.Boss.ai.skill.SkillExecutor.setDebugEnabled(debug);

        DemonLordBossType.register();
        net.trduc.magicabilitiesfork.Boss.bosses.tempestsovereign.TempestSovereignBossType.register();

        getServer().getPluginManager().registerEvents(new BossEventListener(bossManager, bossMasteryStore, getConfig()), this);

        fortressBossSpawner = new FortressBossSpawner(this, bossFactory);
        getServer().getPluginManager().registerEvents(fortressBossSpawner, this);
        fortressBossSpawner.start();

        BukkitRunnable tickRunnable = new BukkitRunnable() {
            @Override
            public void run() {
                bossManager.tickAll();
            }
        };
        bossTickTask = tickRunnable.runTaskTimer(this, 4L, 4L);
    }

    @Override
    public void onDisable() {
        if (fortressBossSpawner != null) {
            fortressBossSpawner.stop();
        }
        if (bossTickTask != null) {
            bossTickTask.cancel();
        }
        if (bossManager != null) {
            bossManager.clearAll();
        }
        if (intrinsicTickTask != null) {
            intrinsicTickTask.cancel();
        }
        if (intrinsicPruneTask != null) {
            intrinsicPruneTask.cancel();
        }
        if (intrinsicManager != null) {
            intrinsicManager.clearAll();
        }
        dbManager.disconnect();
        savePlayers(dbManager);
    }

    private void registerCommand(CommandExecutor cmd, String cmdName){
        if (!(cmd instanceof TabCompleter)){
            throw new RuntimeException("Provided object is not a command executor and a tab completer at the same time!");
        }
        PluginCommand command = getCommand(cmdName);
        if (command == null) {
            throw new IllegalStateException("Cannot register '" + cmdName + "' (" + cmd.getClass().getSimpleName()
                    + "): no matching entry found under 'commands:' in plugin.yml. Add one or fix the name mismatch.");
        }
        command.setExecutor(cmd);
        command.setTabCompleter((TabCompleter) cmd);
    }

    public static Logger getLog(){
        return log;
    }

    public DbManager getDbManager() {
        return dbManager;
    }

    public BossManager getBossManager() {
        return bossManager;
    }

    public BossFactory getBossFactory() {
        return bossFactory;
    }

    public BossMasteryStore getBossMasteryStore() {
        return bossMasteryStore;
    }

    public IntrinsicManager getIntrinsicManager() {
        return intrinsicManager;
    }

    public PlayerIntrinsicStorage getPlayerIntrinsicStorage() {
        return playerIntrinsicStorage;
    }

    public static void debugLog(String msg, boolean warning){
        if (!config.getBoolean("debug")){
            return;
        }
        if (warning) log.warning("[MagicAbilitiesfork:Debug] " + msg);
        else log.info("[MagicAbilitiesfork:Debug] " + msg);
    }
    private void checkDb(DbManager db){
        db.connect();
        if (db.isDbEnabled()){
            log.info("Database is operational");
        } else log.warning("Database is offline!");
    }
    private void setPlayerData(DbManager db){
        for (Player p : getServer().getOnlinePlayers()){
            PlayerData.setPlayerDataFromDb(p, db);
            new PowerPlayer(Power.getPowerFromPowerType(p, getPlayerData(p).getPower()), getPlayerData(p).getBinds(), getPlayerData(p).isEnabled(), getPlayerData(p).isAuraEnabled());
        }
    }

    private void savePlayers(DbManager db){
        for (Player p : getServer().getOnlinePlayers()){
            PlayerData.savePlayerDataToDb(p, db);
            PlayerData.removePlayerData(p);
            if (players.containsKey(p)) {
                players.get(p).remove();
                players.remove(p);
            }
        }
    }

    private FileConfiguration createCooldownsConfig() {
        File customConfigFile = new File(getDataFolder(), "cooldowns.yml");
        if (!customConfigFile.exists()) {
            customConfigFile.getParentFile().mkdirs();
            saveResource("cooldowns.yml", false);
        }

        return YamlConfiguration.loadConfiguration(customConfigFile);
    }

    private FileConfiguration createRandomPowersConfig() {
        File file = new File(getDataFolder(), "random-powers.yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            saveResource("random-powers.yml", false);
        }

        return YamlConfiguration.loadConfiguration(file);
    }
}

