package com.gmail.nossr50.core.util.experience;

import com.gmail.nossr50.core.config.MainConfig;
import com.gmail.nossr50.core.config.experience.ExperienceConfig;
import com.gmail.nossr50.core.datatypes.experience.FormulaType;
import com.gmail.nossr50.core.skills.PrimarySkillType;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FormulaManager {
    private static File formulaFile = new File(mcMMO.getFlatFileDirectory() + "formula.yml");

    // Experience needed to reach a level, cached values to improve conversion speed
    private final Map<Integer, Integer> experienceNeededLinear = new HashMap<Integer, Integer>();
    private final Map<Integer, Integer> experienceNeededExponential = new HashMap<Integer, Integer>();

    private FormulaType previousFormula;

    //Used for XP formula scaling
    private boolean retroModeEnabled;

    public FormulaManager() {
        /* Setting for Classic Mode (Scales a lot of stuff up by * 10) */
        retroModeEnabled = MainConfig.getInstance().getIsRetroMode();
        loadFormula();
    }

    /**
     * Get the formula type that was used before converting
     *
     * @return previously used formula type
     */
    public FormulaType getPreviousFormulaType() {
        return previousFormula;
    }

    /**
     * Set the formula type that was used before converting
     *
     * @param previousFormulaType The {@link FormulaType} previously used
     */
    public void setPreviousFormulaType(FormulaType previousFormulaType) {
        this.previousFormula = previousFormulaType;
    }

    /**
     * Calculate the total amount of experience earned based on
     * the amount of levels and experience, using the previously
     * used formula type.
     *
     * @param skillLevel   Amount of levels
     * @param skillXPLevel Amount of experience
     * @return The total amount of experience
     */
    public int calculateTotalExperience(int skillLevel, int skillXPLevel) {
        int totalXP = 0;

        for (int level = 0; level < skillLevel; level++) {
            totalXP += getCachedXpToLevel(level, previousFormula);
        }

        totalXP += skillXPLevel;

        return totalXP;
    }

    /**
     * Calculate how many levels a player should have using
     * the new formula type.
     *
     * @param primarySkillType skill where new levels and experience are calculated for
     * @param experience       total amount of experience
     * @param formulaType      The new {@link FormulaType}
     * @return the amount of levels and experience
     */
    public int[] calculateNewLevel(PrimarySkillType primarySkillType, int experience, FormulaType formulaType) {
        int newLevel = 0;
        int remainder = 0;
        int maxLevel = MainConfig.getInstance().getLevelCap(primarySkillType);

        while (experience > 0 && newLevel < maxLevel) {
            int experienceToNextLevel = getCachedXpToLevel(newLevel, formulaType);

            if (experience - experienceToNextLevel < 0) {
                remainder = experience;
                break;
            }

            newLevel++;
            experience -= experienceToNextLevel;
        }

        return new int[]{newLevel, remainder};
    }

    /**
     * Get the cached amount of experience needed to reach the next level,
     * if cache doesn't contain the given value it is calculated and added
     * to the cached data.
     *
     * @param level       level to check
     * @param formulaType The {@link FormulaType} used
     * @return amount of experience needed to reach next level
     */
    public int getCachedXpToLevel(int level, FormulaType formulaType) {
        int experience;

        /**
         * Retro mode XP requirements are the default requirements
         * Standard mode XP requirements are multiplied by a factor of 10
         */
        int xpNeededMultiplier = retroModeEnabled ? 1 : 10;

        if (formulaType == FormulaType.UNKNOWN) {
            formulaType = FormulaType.LINEAR;
        }

        int base = ExperienceConfig.getInstance().getBase(formulaType);
        double multiplier = ExperienceConfig.getInstance().getMultiplier(formulaType);
        double exponent = ExperienceConfig.getInstance().getExponent(formulaType);

        switch (formulaType) {
            case LINEAR:
                if (!experienceNeededLinear.containsKey(level)) {
                    experience = (int) Math.floor(xpNeededMultiplier * (base + level * multiplier));
                    experienceNeededLinear.put(level, experience);
                }

                return experienceNeededLinear.get(level);

            case EXPONENTIAL:
                if (!experienceNeededExponential.containsKey(level)) {
                    experience = (int) Math.floor(xpNeededMultiplier * (multiplier * Math.pow(level, exponent) + base));
                    experienceNeededExponential.put(level, experience);
                }

                return experienceNeededExponential.get(level);

            default:
                return 0;
        }
    }

    /**
     * Load formula file.
     */
    public void loadFormula() {
        if (!formulaFile.exists()) {
            previousFormula = FormulaType.UNKNOWN;
            return;
        }

        previousFormula = FormulaType.getFormulaType(YamlConfiguration.loadConfiguration(formulaFile).getString("Previous_Formula", "UNKNOWN"));
    }

    /**
     * Save formula file.
     */
    public void saveFormula() {
        mcMMO.p.debug("Saving previous XP formula type...");
        YamlConfiguration formulasFile = new YamlConfiguration();
        formulasFile.set("Previous_Formula", previousFormula.toString());

        try {
            formulasFile.save(formulaFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}