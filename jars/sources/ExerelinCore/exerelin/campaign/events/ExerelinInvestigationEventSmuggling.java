package exerelin.campaign.events;

import com.fs.starfarer.api.impl.campaign.events.InvestigationEventSmugglingV2;

public class ExerelinInvestigationEventSmuggling extends InvestigationEventSmugglingV2 {
    
    protected String factionId = "neutral";
    
    @Override
    public void startEvent() {
        //String alignedFactionId = PlayerFactionStore.getPlayerFactionId();
        if (market != null) {
            String marketFactionId = market.getFactionId();
            if (marketFactionId.equals("player_npc"))
            {
                log.info("Investigation by own faction; aborting");
                endEvent();
                return;
            }
        }
        super.startEvent();
        factionId = market.getFactionId();
    }
    
    @Override
    public void advance(float amount) {
        if (!isEventStarted()) return;
        if (isDone()) return;

        // market has changed hands since investigation started; kill it
        if (!market.getFactionId().equals(factionId))
        {
            log.info("Market changed hands; cancelling investigation");
            super.endEvent();
            return;
        }
        
        super.advance(amount);
    }
}