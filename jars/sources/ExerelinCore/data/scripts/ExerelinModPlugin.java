package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import exerelin.campaign.AllianceManager;
import exerelin.campaign.CovertOpsManager;
import exerelin.campaign.DiplomacyManager;
import exerelin.campaign.DirectoryScreenScript;
import exerelin.campaign.ExerelinSetupData;
import exerelin.campaign.PlayerFactionStore;
import exerelin.campaign.ReinitScreenScript;
import exerelin.campaign.SectorManager;
import exerelin.campaign.StatsTracker;
import exerelin.plugins.ExerelinCoreCampaignPlugin;
import exerelin.utilities.*;
import exerelin.world.InvasionFleetManager;
import exerelin.world.MiningFleetManager;
import exerelin.world.ResponseFleetManager;
import org.lazywizard.omnifac.OmniFacSettings;

public class ExerelinModPlugin extends BaseModPlugin
{
    
    protected void applyToExistingSave()
    {
        Global.getLogger(this.getClass()).info("Applying Nexerelin to existing game");
        
        SectorAPI sector = Global.getSector();
        InvasionFleetManager im = InvasionFleetManager.create();
        AllianceManager am = AllianceManager.create();
        sector.addScript(SectorManager.create());
        sector.addScript(DiplomacyManager.create());
        sector.addScript(im);
        sector.addScript(ResponseFleetManager.create());
        sector.addScript(MiningFleetManager.create());
        sector.addScript(CovertOpsManager.create());
        sector.addScript(am);
        im.advance(sector.getClock().getSecondsPerDay() * ExerelinConfig.invasionGracePeriod);
        am.advance(sector.getClock().getSecondsPerDay() * ExerelinConfig.allianceGracePeriod);
        
        StatsTracker.create();
        
        sector.registerPlugin(new ExerelinCoreCampaignPlugin());
        SectorManager.setCorvusMode(true);
        SectorManager.reinitLiveFactions();
        PlayerFactionStore.setPlayerFactionId("player_npc");
        
        sector.addTransientScript(new ReinitScreenScript());
    }
    
    @Override
    public void beforeGameSave()
    {
        //SectorManager.getCurrentSectorManager().getCommandQueue().executeAllCommands();
    }

    @Override
    public void onNewGame() {
        ExerelinSetupData.resetInstance();
        ExerelinConfig.loadSettings();
        //ExerelinCheck.checkModCompatability();
    }
    
    protected void reverseCompatibility()
    {
        // fix famous bounties fighting people they shouldn't
        for (FactionAPI faction : Global.getSector().getAllFactions()) 
        {
            if (!faction.isPlayerFaction() && !faction.getId().equals("famous_bounty"))
            faction.setRelationship("famous_bounty", 0);
        }
        
    }
    
    @Override
    public void onEnabled(boolean wasEnabledBefore) {
        Global.getLogger(this.getClass()).info("On enabled; " + wasEnabledBefore);
    }
    
    @Override
    public void onGameLoad() {
        Global.getLogger(this.getClass()).info("Game load; " + SectorManager.isSectorManagerSaved());
        //if (!SectorManager.isSectorManagerSaved())
        //    applyToExistingSave();
        
        ExerelinConfig.loadSettings();
        SectorManager.create();
        DiplomacyManager.create();
        InvasionFleetManager.create();
        ResponseFleetManager.create();
        MiningFleetManager.create();
        CovertOpsManager.create();
        AllianceManager.create();
        
        if (!Global.getSector().getEventManager().isOngoing(null, "exerelin_faction_salary")) {
            Global.getSector().getEventManager().startEvent(null, "exerelin_faction_salary", null);
        }
        if (!Global.getSector().getEventManager().isOngoing(null, "exerelin_faction_insurance")) {
            Global.getSector().getEventManager().startEvent(null, "exerelin_faction_insurance", null);
        }
        if (SectorManager.getHardMode() && ExerelinUtils.isSSPInstalled())
        {
            if (!Global.getSector().getEventManager().isOngoing(null, "player_bounty")) {
                Global.getSector().getEventManager().startEvent(null, "player_bounty", null);
            }
        }
        
        reverseCompatibility();
        
        Global.getSector().addTransientScript(new DirectoryScreenScript());
    }
    
    @Override
    public void onApplicationLoad() throws Exception
    {
        OmniFacSettings.reloadSettings();
        ExerelinConfig.loadSettings();
    }
    
    @Override
    public void onNewGameAfterTimePass() {
    }

    @Override
    public void onNewGameAfterEconomyLoad() {
        SectorManager.reinitLiveFactions();
        
        if (SectorManager.getCorvusMode())
        {
            DiplomacyManager.initFactionRelationships(false);    // the mod factions set their own relationships, so we have to re-randomize if needed afterwards
            
            // fix Corvus mode tariffs
            for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy())
            {
                if (market.hasCondition("free_market")) 
                    market.getTariff().modifyMult("isFreeMarket", 0.5f);
            }
        }
    }
}
