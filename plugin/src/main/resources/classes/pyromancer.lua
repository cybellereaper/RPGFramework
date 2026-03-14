return {
  id = "pyromancer",
  classItems = { "BLAZE_POWDER", "BLAZE_ROD" },
  requiredArmor = {
    HEAD = "GOLDEN_HELMET",
    CHEST = "GOLDEN_CHESTPLATE",
    LEGS = "GOLDEN_LEGGINGS",
    FEET = "GOLDEN_BOOTS"
  },
  cooldowns = { 12, 20, 18, 35, 180 },

  onLeftClick = function(clazz, event)
    if clazz:isAbilityReady(0) then
      clazz:sendClassMessage("Scorch erupts around you!")
      clazz:restartAbilityCooldown(0)
    end
  end,

  onRightClick = function(clazz, event)
    if clazz:isAbilityReady(1) then
      clazz:sendClassMessage("Kindle empowers your flames!")
      clazz:addPotion("SPEED", 8, 1, false, false)
      clazz:addPotion("FIRE_RESISTANCE", 8, 1, false, false)
      clazz:restartAbilityCooldown(1)
    end
  end,

  onDeath = function(clazz)
    if clazz:isAbilityReady(4) then
      clazz:sendClassMessage("You've used Phoenix Renewal!")
      clazz:addPotion("REGENERATION", 8, 2, false, false)
      clazz:restartAbilityCooldown(4)
      return false
    end

    return true
  end
}
