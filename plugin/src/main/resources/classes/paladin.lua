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

  onLeftClick = function(clazz, event)
    if clazz:isAbilityReady(0) then
      clazz:sendClassMessage("You've used mend!")
      clazz:addPotion("REGENERATION", 8, 2, false, false)
      clazz:restartAbilityCooldown(0)
    end
  end,

  onRightClick = function(clazz, event)
    if clazz:isAbilityReady(1) then
      clazz:sendClassMessage("You've used purify!")
      clazz:addPotion("ABSORPTION", 6, 1, false, false)
      clazz:restartAbilityCooldown(1)
    end
  end,

  onDeath = function(clazz)
    if clazz:isAbilityReady(2) then
      clazz:sendClassMessage("You've used Protection of the Healer!")
      clazz:addPotion("RESISTANCE", 8, 2, false, false)
      clazz:restartAbilityCooldown(2)
      return false
    end

    return true
  end
}
