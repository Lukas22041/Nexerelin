package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.Nex_FactionDirectoryHelper.FactionListGrouping;
import com.fs.starfarer.api.util.Misc;
import exerelin.campaign.SectorManager;
import exerelin.utilities.ExerelinUtils;
import exerelin.utilities.StringHelper;
import java.awt.Color;
import java.util.List;
import java.util.Map;
import org.lwjgl.input.Keyboard;

public class Nex_TransferMarket extends BaseCommandPlugin {
	
	public static final String FACTION_GROUPS_KEY = "$nex_factionDirectoryGroups";
	public static final float GROUPS_CACHE_TIME = 0f;
	public static final String SELECT_FACTION_PREFIX = "nex_transferMarket_";
	static final int PREFIX_LENGTH = SELECT_FACTION_PREFIX.length();
		
	@Override
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
		String arg = params.get(0).getString(memoryMap);
		MarketAPI market = dialog.getInteractionTarget().getMarket();
		switch (arg)
		{
			// list faction groupings
			case "listGroups":
				listGroups(dialog, memoryMap.get(MemKeys.LOCAL));
				return true;
				
			// list factions within a faction grouping
			case "listFactions":
				OptionPanelAPI opts = dialog.getOptionPanel();
				opts.clearOptions();
				int num = (int)params.get(1).getFloat(memoryMap);
				//memoryMap.get(MemKeys.LOCAL).set("$nex_dirFactionGroup", num);
				List<FactionListGrouping> groups = (List<FactionListGrouping>)(memoryMap.get(MemKeys.LOCAL).get(FACTION_GROUPS_KEY));
				FactionListGrouping group = groups.get(num - 1);
				for (FactionAPI faction : group.factions)
				{
					String optKey = SELECT_FACTION_PREFIX + faction.getId();
					opts.addOption(Nex_FactionDirectoryHelper.getFactionDisplayName(faction), optKey);
					String warningString = StringHelper.getStringAndSubstituteToken("exerelin_markets", "transferMarketWarning", 
							"$market", dialog.getInteractionTarget().getMarket().getName());
					warningString = StringHelper.substituteFactionTokens(warningString, faction);
					
					opts.addOptionConfirmation(optKey, warningString, 
							Misc.ucFirst(StringHelper.getString("yes")), Misc.ucFirst(StringHelper.getString("no")));
				}
				
				opts.addOption(Misc.ucFirst(StringHelper.getString("back")), "nex_transferMarketMain");
				opts.setShortcut("nex_transferMarketMain", Keyboard.KEY_ESCAPE, false, false, false, false);
				
				ExerelinUtils.addDevModeDialogOptions(dialog, false);
				
				return true;
			
			// actually transfer the market to the specified faction
			case "transfer":
				String option = memoryMap.get(MemKeys.LOCAL).getString("$option");
				//if (option == null) throw new IllegalStateException("No $option set");
				String factionId = option.substring(PREFIX_LENGTH);
				transferMarket(dialog, market, factionId);
				return true;
			
			// prints a string listing the reputation change from performing the transfer
			case "printRepChange":
				TextPanelAPI text = dialog.getTextPanel();
				text.setFontSmallInsignia();
				String repChange = getRepChange(market, true) + "";
				String str = StringHelper.getString("exerelin_markets", "transferMarketRep");
				str = StringHelper.substituteToken(str, "$repChange", repChange);
				str = StringHelper.substituteToken(str, "$market", market.getName() + "");
				text.addParagraph(str);
				text.highlightLastInLastPara(repChange, Misc.getHighlightColor());
				text.setFontInsignia();
				return true;
		}
		return false;
	}
	
	
	protected float getRepChange(MarketAPI market, boolean playerFacing)
	{
		float repChange = (5 + market.getSize()*5) * ((market.getStabilityValue() + 10) / 20);
		if (!playerFacing) repChange *= 0.01f;
		return repChange;
	}
	
	public void transferMarket(InteractionDialogAPI dialog, MarketAPI market, String newFactionId)
	{
		String oldFactionId = market.getFactionId();
		FactionAPI newFaction = Global.getSector().getFaction(newFactionId);
		FactionAPI oldFaction = Global.getSector().getFaction(oldFactionId);
		float repChange = getRepChange(market, false);
		
		SectorManager.transferMarket(market, newFaction, oldFaction, true, false, null, repChange);
				
		//ExerelinUtilsReputation.adjustPlayerReputation(newFaction, null, repChange, null, dialog.getTextPanel());	// done in event
		market.getPrimaryEntity().getMemoryWithoutUpdate().set("$_newFaction", newFaction.getDisplayNameWithArticle(), 0);
	}
	
	/**
	 * Creates dialog options for the faction list subgroups
	 * @param dialog
	 */
	public static void listGroups(InteractionDialogAPI dialog, MemoryAPI memory)
	{		
		OptionPanelAPI opts = dialog.getOptionPanel();
		opts.clearOptions();
		List<FactionListGrouping> groups;
		
		if (memory.contains(FACTION_GROUPS_KEY))
		{
			groups = (List<FactionListGrouping>)memory.get(FACTION_GROUPS_KEY);
		}
		else
		{
			List<String> factionsForDirectory = Nex_FactionDirectoryHelper.getFactionsForDirectory(Nex_FactionDirectory.ARRAYLIST_FOLLOWERS);
			groups = Nex_FactionDirectoryHelper.getFactionGroupings(factionsForDirectory);
			memory.set(FACTION_GROUPS_KEY, groups, GROUPS_CACHE_TIME);
		}

		int groupNum = 0;
		for (FactionListGrouping group : groups)
		{
			groupNum++;
			String optionId = "nex_transferMarketList" + groupNum;
			opts.addOption(group.getGroupingRangeString(),
					optionId, group.tooltip);
			opts.setTooltipHighlights(optionId, group.getFactionNames().toArray(new String[0]));
			opts.setTooltipHighlightColors(optionId, group.getTooltipColors().toArray(new Color[0]));
		}
		
		String exitOpt = "exerelinBaseCommanderMenuRepeat";
		opts.addOption(Misc.ucFirst(StringHelper.getString("back")), exitOpt);
		opts.setShortcut(exitOpt, Keyboard.KEY_ESCAPE, false, false, false, false);
		
		ExerelinUtils.addDevModeDialogOptions(dialog, false);
	}
}