package exerelin.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.GenericPluginManagerAPI;
import com.fs.starfarer.api.campaign.PersistentUIDataAPI.AbilitySlotAPI;
import com.fs.starfarer.api.campaign.PersistentUIDataAPI.AbilitySlotsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.InstallableIndustryItemPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.ItemEffectsRepo;
import static com.fs.starfarer.api.impl.campaign.econ.impl.ItemEffectsRepo.CATALYTIC_CORE_BONUS;
import static com.fs.starfarer.api.impl.campaign.econ.impl.ItemEffectsRepo.SYNCHROTRON_FUEL_BONUS;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.FactionHostilityManager;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.historian.HistorianData;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.historian.HistorianData.HistorianOfferCreator;
import com.fs.starfarer.api.impl.campaign.intel.inspection.HegemonyInspectionManager;
import com.fs.starfarer.api.impl.campaign.intel.punitive.PunitiveExpeditionManager;
import com.fs.starfarer.api.impl.campaign.missions.cb.MilitaryCustomBounty;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.thoughtworks.xstream.XStream;
import exerelin.ExerelinConstants;
import exerelin.campaign.AcademyStoryVictoryScript;
import exerelin.campaign.AllianceManager;
import exerelin.campaign.ColonyManager;
import exerelin.campaign.CovertOpsManager;
import exerelin.campaign.DiplomacyManager;
import exerelin.campaign.ExerelinSetupData;
import exerelin.campaign.ExerelinSetupData.HomeworldPickMode;
import exerelin.campaign.ui.FieldOptionsScreenScript;
import exerelin.campaign.MarketDescChanger;
import exerelin.campaign.MiningCooldownDrawer;
import exerelin.campaign.MiscEventsManager;
import exerelin.campaign.PlayerFactionStore;
import exerelin.campaign.ui.PlayerFactionSetupNag;
import exerelin.campaign.StartSetupPostTimePass;
import exerelin.campaign.ui.ReinitScreenScript;
import exerelin.campaign.RevengeanceManager;
import exerelin.campaign.SectorManager;
import exerelin.campaign.StatsTracker;
import exerelin.campaign.battle.EncounterLootHandler;
import exerelin.campaign.econ.EconomyInfoHelper;
import exerelin.campaign.econ.Nex_BoostIndustryInstallableItemEffect;
import exerelin.utilities.*;
import exerelin.campaign.fleets.InvasionFleetManager;
import exerelin.campaign.fleets.MiningFleetManagerV2;
import exerelin.campaign.fleets.PlayerInSystemTracker;
import exerelin.campaign.fleets.VultureFleetManager;
import exerelin.campaign.intel.missions.ConquestMissionManager;
import exerelin.campaign.intel.FactionBountyManager;
import exerelin.campaign.intel.MilestoneTracker;
import exerelin.campaign.intel.Nex_HegemonyInspectionManager;
import exerelin.campaign.intel.Nex_PunitiveExpeditionManager;
import exerelin.campaign.intel.agents.AgentBarEventCreator;
import exerelin.campaign.intel.bar.historian.ShuntLocationOfferCreator;
import exerelin.campaign.intel.merc.MercSectorManager;
import exerelin.campaign.intel.missions.Nex_CBHegInspector;
import exerelin.campaign.intel.missions.remnant.RemnantQuestUtils;
import exerelin.campaign.intel.rebellion.RebellionCreator;
import exerelin.campaign.intel.specialforces.SpecialForcesManager;
import exerelin.campaign.submarkets.PrismMarket;
import exerelin.utilities.versionchecker.VCModPluginCustom;
import exerelin.world.ExerelinNewGameSetup;
import exerelin.world.ExerelinProcGen;
import exerelin.world.LandmarkGenerator;
import exerelin.world.SSP_AsteroidTracker;
import exerelin.world.VanillaSystemsGenerator;
import exerelin.world.scenarios.DerelictEmpireOfficerGeneratorPlugin;
import exerelin.world.scenarios.ScenarioManager;
import java.io.IOException;
import java.util.Random;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class ExerelinModPlugin extends BaseModPlugin
{
    // call order: onNewGame -> onNewGameAfterProcGen -> onNewGameAfterEconomyLoad -> onEnabled -> onNewGameAfterTimePass -> onGameLoad
    public static final boolean HAVE_SWP = Global.getSettings().getModManager().isModEnabled("swp");
    public static final boolean HAVE_DYNASECTOR = Global.getSettings().getModManager().isModEnabled("dynasector");
    public static final boolean HAVE_UNDERWORLD = Global.getSettings().getModManager().isModEnabled("underworld");
    //public static final boolean HAVE_STELLAR_INDUSTRIALIST = Global.getSettings().getModManager().isModEnabled("stellar_industrialist");
    public static final boolean HAVE_VERSION_CHECKER = Global.getSettings().getModManager().isModEnabled("lw_version_checker");
    public static boolean isNexDev = false;
    
    public static Logger log = Global.getLogger(ExerelinModPlugin.class);
    protected static boolean isNewGame = false;
    
    
    public static void replaceSubmarket(MarketAPI market, String submarketId) {
        if (!market.hasSubmarket(submarketId)) return;
        
        CargoAPI current = market.getSubmarket(submarketId).getCargo();
        FleetDataAPI ships = current.getMothballedShips();
        boolean haveAccess = Misc.playerHasStorageAccess(market);
        
        market.removeSubmarket(submarketId);
        market.addSubmarket(submarketId);
        SubmarketAPI submarket = market.getSubmarket(submarketId);
        
        // migrate cargo
        CargoAPI newCargo = market.getSubmarket(submarketId).getCargo();
        newCargo.clear();
        newCargo.addAll(current);
        newCargo.sort();
        
        // move ships to new cargo
        newCargo.initMothballedShips(submarket.getFaction().getId());
        for (FleetMemberAPI ship : ships.getMembersListCopy()) {
            newCargo.getMothballedShips().addFleetMember(ship);
        }
        
        if (submarketId.equals(Submarkets.SUBMARKET_STORAGE)) {
            ((StoragePlugin)submarket.getPlugin()).setPlayerPaidToUnlock(haveAccess);
        }
    }
    
    protected void applyToExistingSave()
    {
        log.info("Applying Nexerelin to existing game");
        
        SectorAPI sector = Global.getSector();
        addScripts();
        
        // debugging
        //im.advance(sector.getClock().getSecondsPerDay() * ExerelinConfig.invasionGracePeriod);
        //am.advance(sector.getClock().getSecondsPerDay() * ExerelinConfig.allianceGracePeriod);
        
        // replace or remove relevant intel items
        ScriptReplacer.replaceScript(sector, FactionHostilityManager.class, null);
        ScriptReplacer.replaceScript(sector, HegemonyInspectionManager.class, new Nex_HegemonyInspectionManager());
        ScriptReplacer.replaceScript(sector, PunitiveExpeditionManager.class, new Nex_PunitiveExpeditionManager());
        //ScriptReplacer.replaceMissionCreator(ProcurementMissionCreator.class, new Nex_ProcurementMissionCreator());
        
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy())
        {
            replaceSubmarket(market, Submarkets.LOCAL_RESOURCES);
            replaceSubmarket(market, Submarkets.SUBMARKET_OPEN);
            replaceSubmarket(market, Submarkets.GENERIC_MILITARY);
            replaceSubmarket(market, Submarkets.SUBMARKET_BLACK);
            replaceSubmarket(market, Submarkets.SUBMARKET_STORAGE);
        }
        
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy())
        {
            if (!market.getMemoryWithoutUpdate().contains(ExerelinConstants.MEMKEY_MARKET_STARTING_FACTION))
                market.getMemoryWithoutUpdate().set(ExerelinConstants.MEMKEY_MARKET_STARTING_FACTION, market.getFactionId());
            market.getMemoryWithoutUpdate().set("$startingFreeMarket", market.hasCondition(Conditions.FREE_PORT) || market.isFreePort());
            ColonyManager.updateFreePortSetting(market);
        }
        
        StatsTracker.create();
        
        SectorManager.reinitLiveFactions();
        NexUtilsReputation.syncFactionRelationshipsToPlayer();
        
        // add Follow Me ability
        Global.getSector().getCharacterData().addAbility("exerelin_follow_me");
        AbilitySlotsAPI slots = Global.getSector().getUIData().getAbilitySlotsAPI();
        for (AbilitySlotAPI slot: slots.getCurrSlotsCopy())
        {
            if (slot.getAbilityId() == null || slot.getAbilityId().isEmpty())
            {
                slot.setAbilityId("exerelin_follow_me");
                break;
            }
        }
        
        sector.addTransientScript(new ReinitScreenScript());
    }
    
    protected void refreshTariffsAndGrowthRate()
    {
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy())
        {
            NexUtilsMarket.setTariffs(market);
            if (ColonyManager.getManager() != null)	// will be null if loading non-Nex save
                ColonyManager.getManager().setGrowthRate(market);
        }
    }
    
    protected void reverseCompatibility()
    {
        SectorManager.getManager().reverseCompatibility();
        
        boolean hasShuntOffer = false;
        HistorianData hd = HistorianData.getInstance();
        for (HistorianOfferCreator oc : hd.getCreators()) {
            if (oc instanceof ShuntLocationOfferCreator) {
                hasShuntOffer = true;
                break;
            }
        }
        if (!hasShuntOffer) hd.getCreators().add(new ShuntLocationOfferCreator());
    }
    
    // runcode exerelin.plugins.ExerelinModPlugin.debug();
    public static void debug() {
        
    }
    
    protected static void addBarEvents() {
        BarEventManager bar = BarEventManager.getInstance();
        if (bar != null && !bar.hasEventCreator(AgentBarEventCreator.class)) {
            bar.addEventCreator(new AgentBarEventCreator());
        }
    }
    
    public static void addScripts() {
        SectorAPI sector = Global.getSector();
        sector.addScript(SectorManager.create());
        sector.addScript(DiplomacyManager.create());
        sector.addScript(InvasionFleetManager.create());
        //sector.addScript(ResponseFleetManager.create());
        sector.addScript(MiningFleetManagerV2.create());
        sector.addScript(VultureFleetManager.create());
        sector.addScript(CovertOpsManager.create());
        sector.addScript(AllianceManager.create());
        new ColonyManager().init();
        new RevengeanceManager().init();
        new SpecialForcesManager().init();
        RebellionCreator.generate();
        
        sector.addScript(new ConquestMissionManager());
        //sector.addScript(new DisruptMissionManager());
        sector.addScript(new FactionBountyManager());
        
        new MilestoneTracker().init();
        
        addBarEvents();
        
        if (!ExerelinSetupData.getInstance().skipStory)
            new AcademyStoryVictoryScript().init();
    }
    
    // Stuff here should be moved to new game once it is expected that no existing saves lack them
    protected void addScriptsAndEventsIfNeeded() {
        if (MercSectorManager.getInstance() == null) {
            new MercSectorManager().init();
        }
    }
    
    protected void alphaSiteWorkaround() {
        // workaround for blacksite NPE that some mods have
        StarSystemAPI sys = Global.getSector().getStarSystem("Unknown Location");
        if (sys != null && sys.getType() != StarSystemType.NEBULA) {
            log.info("Fixing secret location");
            sys.setType(StarSystemType.NEBULA);
        }
    }
    
    public static void modifySynchrotronAndCatCore() {
        ItemEffectsRepo.ITEM_EFFECTS.put(Items.SYNCHROTRON, new Nex_BoostIndustryInstallableItemEffect(
                                Items.SYNCHROTRON, SYNCHROTRON_FUEL_BONUS, 0) {
            @Override
            protected void addItemDescriptionImpl(Industry industry, TooltipMakerAPI text, SpecialItemData data,
                                                    InstallableIndustryItemPlugin.InstallableItemDescriptionMode mode, String pre, float pad) {
                //text.addPara(pre + "Increases fuel production and demand for volatiles by %s.",
                text.addPara(pre + StringHelper.getString("nex_industry", "effect_synchrotron"),
                        pad, Misc.getHighlightColor(), "" + SYNCHROTRON_FUEL_BONUS);
            }
            @Override
            public String[] getSimpleReqs(Industry industry) {
                return new String [] {Nex_BoostIndustryInstallableItemEffect.STATION_OR_NO_ATMO};
            }
        });
        
        ItemEffectsRepo.ITEM_EFFECTS.put(Items.CATALYTIC_CORE, new Nex_BoostIndustryInstallableItemEffect(
                                    Items.CATALYTIC_CORE, CATALYTIC_CORE_BONUS, 0) {
            @Override
            protected void addItemDescriptionImpl(Industry industry, TooltipMakerAPI text, SpecialItemData data, 
                                                  InstallableIndustryItemPlugin.InstallableItemDescriptionMode mode, String pre, float pad) {
                text.addPara(pre + StringHelper.getString("nex_industry", "effect_catalyticCore"),
                        pad, Misc.getHighlightColor(), 
                        "" + (int) CATALYTIC_CORE_BONUS);
            }
            @Override
            public String[] getSimpleReqs(Industry industry) {
                return new String [] {Nex_BoostIndustryInstallableItemEffect.STATION_OR_NO_ATMO};
            }
        });
    }
    
    protected void expandSector() {
        // Sector expander
        float expandLength = Global.getSettings().getFloat("nex_expandCoreLength");
        float expandMult = Global.getSettings().getFloat("nex_expandCoreMult");
        Vector2f center = new Vector2f(ExerelinNewGameSetup.SECTOR_CENTER);
        
        if (expandLength == 0 && expandMult == 1)
            return;
        
        if (expandLength != 0) {
            for (StarSystemAPI system : Global.getSector().getStarSystems()) {
                Vector2f loc = system.getLocation();
                Vector2f fromCenter = new Vector2f(loc.x - center.x, loc.y - center.y);
                Vector2f fromCenterNew = new Vector2f();
                fromCenterNew = VectorUtils.resize(fromCenter, fromCenter.length() + expandLength, fromCenterNew);
                loc.translate(fromCenterNew.x - fromCenter.x, fromCenterNew.y - fromCenter.y);
                //if (expandLength < 0) VectorUtils.rotate(loc, 180);
            }
        }
        if (expandMult != 1) {
            for (StarSystemAPI system : Global.getSector().getStarSystems()) {
                Vector2f loc = system.getLocation();
                Vector2f fromCenter = new Vector2f(loc.x - center.x, loc.y - center.y);
                Vector2f fromCenterNew = new Vector2f(fromCenter);
                fromCenterNew.scale(expandMult);
                loc.translate(fromCenterNew.x - fromCenter.x, fromCenterNew.y - fromCenter.y);
                if (expandMult < 0) VectorUtils.rotate(loc, 180);
            }
        }
        Global.getSector().getHyperspace().updateAllOrbits();
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
            ExerelinNewGameSetup.clearDeepHyper(system.getHyperspaceAnchor(), 
                    system.getMaxRadiusInHyperspace() + plugin.getTileSize() * 2);
        }
    }
    
    @Override
    public void onGameLoad(boolean newGame) {
        log.info("Game load");
        isNewGame = newGame;
        
        ScenarioManager.clearScenario();
        
        addScriptsAndEventsIfNeeded();
        
        reverseCompatibility();
        refreshTariffsAndGrowthRate();
        
        SectorAPI sector = Global.getSector();
        sector.registerPlugin(new ExerelinCampaignPlugin());
        sector.addTransientScript(new FieldOptionsScreenScript());
        sector.addTransientScript(new SSP_AsteroidTracker());
        //sector.removeScriptsOfClass(FactionHostilityManager.class);
        
        PrismMarket.clearSubmarketCache();
        
        ColonyManager.getManager().updatePlayerBonusAdmins();
        ColonyManager.updateIncome();
        
        if (!HAVE_VERSION_CHECKER && Global.getSettings().getBoolean("nex_enableVersionChecker"))
            VCModPluginCustom.onGameLoad(newGame);
        
        if (!Misc.isPlayerFactionSetUp())
            sector.addTransientScript(new PlayerFactionSetupNag());
        
        sector.addTransientListener(new EncounterLootHandler());
        if (!newGame)
            EconomyInfoHelper.createInstance();
        
        if (NexConfig.updateMarketDescOnCapture && MarketDescChanger.getInstance() == null) {
            sector.getListenerManager().addListener(new MarketDescChanger().registerInstance(), true);
        }
        
        PlayerInSystemTracker.create();
        MiscEventsManager.create();
        //sector.addTransientScript(new MiningCooldownDrawer());
        if (MiningCooldownDrawer.getEntity() == null) 
            MiningCooldownDrawer.create();
        
        GenericPluginManagerAPI plugins = sector.getGenericPlugins();
        if (!plugins.hasPlugin(DerelictEmpireOfficerGeneratorPlugin.class)) {
            plugins.addPlugin(new DerelictEmpireOfficerGeneratorPlugin(), true);
        }
        
        alphaSiteWorkaround();
    }
    
    @Override
    public void beforeGameSave()
    {
        log.info("Before game save");
    }
    
    @Override
    public void afterGameSave() {
        log.info("After game save");
    }
    
    @Override
    public void onApplicationLoad() throws Exception
    {
        boolean bla = NexConfig.countPiratesForVictory;	// just loading config class, not doing anything with it
        if (!HAVE_VERSION_CHECKER && Global.getSettings().getBoolean("nex_enableVersionChecker"))
            VCModPluginCustom.onApplicationLoad();
        
        // Nex dev check
        try {
            String str = Global.getSettings().readTextFileFromCommon("nex_dev");
            if (str != null && !str.isEmpty()) {
                log.info("Nex dev mode on: " + str);
                isNexDev = true;
            }
            else {
                log.info("Nex dev mode off");
            }
        }
        catch (IOException ex)
        {
            // Do nothing
        }
        
        loadRaidBPBlocker();
        RemnantQuestUtils.setupRemnantContactMissions();
        modifySynchrotronAndCatCore();
        
        //MilitaryCustomBounty.CREATORS.clear();	// for debugging
        MilitaryCustomBounty.CREATORS.add(new Nex_CBHegInspector());
    }
    
    @Override
    public void onNewGame() {
        log.info("New game");
        isNewGame = true;
        //ExerelinSetupData.resetInstance();
        //ExerelinCheck.checkModCompatability();
        addScriptsAndEventsIfNeeded();
    }
    
    @Override
    public void onEnabled(boolean wasEnabledBefore) {
        log.info("On enabled; " + wasEnabledBefore);
        if (!isNewGame && !wasEnabledBefore)
        {
            log.info(!isNewGame + ", " + !wasEnabledBefore);
            applyToExistingSave();
        }
    }
    
    @Override
    public void onNewGameAfterProcGen() {
        log.info("New game after proc gen; " + isNewGame);
        
        alphaSiteWorkaround();
        
		// random sector: populate the sector
        if (!SectorManager.getManager().isCorvusMode()) {
            new ExerelinProcGen().generate(false);
			// second pass: get player homeworld if we didn't pick it before (due to requiring a non-core world)
			if (ExerelinSetupData.getInstance().homeworldPickMode == HomeworldPickMode.NON_CORE) 
			{
				new ExerelinProcGen().generate(true);
			}
        }
        
        ScenarioManager.afterProcGen(Global.getSector());
    }
    
    @Override
    public void onNewGameAfterEconomyLoad() {
        log.info("New game after economy load; " + isNewGame);
        
        if (SectorManager.getManager().isCorvusMode()) {
            VanillaSystemsGenerator.enhanceVanillaMarkets();
            VanillaSystemsGenerator.enhanceVanillaAdmins();
        }
        
		// non-random sector: pick player homeworld in own faction start
        ExerelinSetupData setupData = ExerelinSetupData.getInstance();
        if (SectorManager.getManager().isCorvusMode() 
                && PlayerFactionStore.getPlayerFactionIdNGC().equals(Factions.PLAYER) 
                && !setupData.freeStart) {
            new ExerelinProcGen().generate(true);
        }
        
        expandSector();
        
        ScenarioManager.afterEconomyLoad(Global.getSector());
        
        SectorManager.reinitLiveFactions();
        
        if (SectorManager.getManager().isCorvusMode())
        {
            DiplomacyManager.initFactionRelationships(false);    // the mod factions set their own relationships, so we have to re-randomize if needed afterwards
        }
        
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy())
        {
            if (!market.getMemoryWithoutUpdate().contains(ExerelinConstants.MEMKEY_MARKET_STARTING_FACTION))
                market.getMemoryWithoutUpdate().set(ExerelinConstants.MEMKEY_MARKET_STARTING_FACTION, market.getFactionId());
            market.getMemoryWithoutUpdate().set("$startingFreeMarket", market.hasCondition(Conditions.FREE_PORT) || market.isFreePort());
        }
        
        new LandmarkGenerator().generate(Global.getSector(), SectorManager.getManager().isCorvusMode());
        
        addBarEvents();
        EconomyInfoHelper.createInstance();
    }
    
    @Override
    public void onNewGameAfterTimePass() {
        log.info("New game after time pass; " + isNewGame);
        ScenarioManager.afterTimePass(Global.getSector());
        StartSetupPostTimePass.execute();
        
        // generate random additional colonies
        // note: faction picking relies on live faction list having been generated
        if (ExerelinSetupData.getInstance().corvusMode) {
            if (NexConfig.updateMarketDescOnCapture && MarketDescChanger.getInstance() == null) {
                Global.getSector().getListenerManager().addListener(new MarketDescChanger().registerInstance(), true);
            }
            
            int count = ExerelinSetupData.getInstance().randomColonies;
            int tries = 0;
            Random random = new Random(NexUtils.getStartingSeed());
            while (count > 0) {
                boolean success = ColonyManager.getManager().generateInstantColony(random);
                if (success)
                    count--;
                tries++;
                if (tries >= 1000)
                    break;
            }
        }
    }
    
    @Override
    public void configureXStream(XStream x) {
        XStreamConfig.configureXStream(x);
    }
    
    public void loadRaidBPBlocker() {
        
        try {
            JSONArray csv = Global.getSettings().getMergedSpreadsheetDataForMod("id", "data/config/exerelin/raid_bp_blacklist.csv", "nexerelin");
            for (int i=0; i < csv.length(); i++) {
                JSONObject row = csv.getJSONObject(i);
                String id = row.getString("id");
                if (!id.isEmpty() && !id.startsWith("#")) {
                    ShipHullSpecAPI hull = Global.getSettings().getHullSpec(id);
                    if (hull != null) hull.addTag(Tags.NO_BP_DROP);
                }
            }
        }
        catch (IOException | JSONException ex) {
            log.warn("Failed to load blueprint raid blacklist", ex);
        }
    }
}
