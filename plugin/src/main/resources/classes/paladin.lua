return {
  id = "paladin",

  classItems = { "GOLDEN_SHOVEL" },

  requiredArmor = {
    HEAD = "GOLDEN_HELMET",
    CHEST = "GOLDEN_CHESTPLATE",
    LEGS = "GOLDEN_LEGGINGS",
    FEET = "GOLDEN_BOOTS"
  },

  cooldowns = { 60, 45, 300 },

  abilities = {
    MEND = 0,
    PURIFY = 1,
    PROTECTION_OF_THE_HEALER = 2
  },

  constants = {
    MEND_SECONDS = 8,
    PURIFY_SECONDS = 6,
    PROTECTION_SECONDS = 8
  },

  onLeftClick = function(clazz, event)
    if not clazz:isAbilityReady(clazz.abilities.MEND) then
      return
    end

    clazz:sendClassMessage("You've used mend!")
    clazz:addPotion("REGENERATION", clazz.constants.MEND_SECONDS, 2, false, false)
    clazz:restartAbilityCooldown(clazz.abilities.MEND)
  end,

  onRightClick = function(clazz, event)
    if not clazz:isAbilityReady(clazz.abilities.PURIFY) then
      return
    end

    clazz:sendClassMessage("You've used purify!")
    clazz:addPotion("ABSORPTION", clazz.constants.PURIFY_SECONDS, 1, false, false)
    clazz:restartAbilityCooldown(clazz.abilities.PURIFY)
  end,

  onDeath = function(clazz)
    if not clazz:isAbilityReady(clazz.abilities.PROTECTION_OF_THE_HEALER) then
      return true
    end

    clazz:sendClassMessage("You've used Protection of the Healer!")
    clazz:addPotion("RESISTANCE", clazz.constants.PROTECTION_SECONDS, 2, false, false)
    clazz:restartAbilityCooldown(clazz.abilities.PROTECTION_OF_THE_HEALER)
    return false
  end
}
