package exerelin.campaign.intel.fleets;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager;
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel;
import com.fs.starfarer.api.impl.campaign.intel.raid.TravelStage;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exerelin.utilities.StringHelper;
import java.awt.Color;
import java.util.List;

public class NexTravelStage extends TravelStage {
	
	protected OffensiveFleetIntel offFltIntel;

	public NexTravelStage(OffensiveFleetIntel intel, SectorEntityToken from, SectorEntityToken to, boolean requireNearTarget) {
		super(intel, from, to, requireNearTarget);
		offFltIntel = intel;
	}
	
	protected Object readResolve() {
		if (offFltIntel == null)
			offFltIntel = (OffensiveFleetIntel)intel;
		
		return this;
	}
	
	@Override
	protected boolean enoughMadeIt(List<RouteManager.RouteData> routes, List<RouteManager.RouteData> stragglers) {
		return OffensiveFleetIntel.enoughMadeIt(offFltIntel, abortFP, routes, stragglers);
	}
	
	@Override
	public void showStageInfo(TooltipMakerAPI info) {
		int curr = intel.getCurrentStage();
		int index = intel.getStageIndex(this);
		
		Color h = Misc.getHighlightColor();
		Color g = Misc.getGrayColor();
		Color tc = Misc.getTextColor();
		float pad = 3f;
		float opad = 10f;
		
		String key = "stageTravel";
		String loc = intel.getSystem().getNameWithLowercaseType();
		if (isFailed(curr, index)) {
			key = "stageTravelFailed";
		} else if (curr == index) {
		} else {
			return;
		}
		String str = StringHelper.getStringAndSubstituteToken("nex_fleetIntel", key, "$location", loc);
		str = StringHelper.substituteToken(str, "$theForceType", offFltIntel.getForceTypeWithArticle(), true);
		str = StringHelper.substituteToken(str, "$isOrAre", offFltIntel.getForceTypeIsOrAre());
		str = StringHelper.substituteToken(str, "$hasOrHave", offFltIntel.getForceTypeHasOrHave());
		info.addPara(str, opad);
	}
	
	protected boolean isFailed(int curr, int index) {
		if (status == RaidIntel.RaidStageStatus.FAILURE)
			return true;
		if (curr == index && offFltIntel.getOutcome() == OffensiveFleetIntel.OffensiveOutcome.FAIL)
			return true;
		
		return false;
	}
}
