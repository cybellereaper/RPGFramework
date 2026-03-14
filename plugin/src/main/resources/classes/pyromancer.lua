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

  abilities = {
    SCORCH = 0,
    KINDLE = 1,
    FLARE_VOLLEY = 2,
    CAUTERIZE = 3,
    PHOENIX_RENEWAL = 4
  },

  constants = {
    KINDLE_DURATION_SECONDS = 8,
    PHOENIX_DURATION_SECONDS = 8,
    PHOENIX_COOLDOWN_SECONDS = 180
  },

  isEffectWindowActive = function(clazz, abilityIndex, cooldownSeconds, activeWindowSeconds)
    return clazz:getRemainingCooldown(abilityIndex) >= (cooldownSeconds - activeWindowSeconds)
  end,

  isPhoenixEmpowered = function(clazz)
    return clazz:isEffectWindowActive(
      clazz.abilities.PHOENIX_RENEWAL,
      clazz.constants.PHOENIX_COOLDOWN_SECONDS,
      clazz.constants.PHOENIX_DURATION_SECONDS
    )
  end,

  onLeftClick = function(clazz, event)
    if not clazz:isAbilityReady(clazz.abilities.SCORCH) then
      return
    end

    clazz:sendClassMessage("Scorch erupts around you!")
    clazz:restartAbilityCooldown(clazz.abilities.SCORCH)
  end,

  onRightClick = function(clazz, event)
    if not clazz:isAbilityReady(clazz.abilities.KINDLE) then
      return
    end

    clazz:sendClassMessage("Kindle empowers your flames!")
    clazz:addPotion("SPEED", clazz.constants.KINDLE_DURATION_SECONDS, 1, false, false)
    clazz:addPotion("FIRE_RESISTANCE", clazz.constants.KINDLE_DURATION_SECONDS, 1, false, false)
    clazz:restartAbilityCooldown(clazz.abilities.KINDLE)
  end,

  onDeath = function(clazz)
    if not clazz:isAbilityReady(clazz.abilities.PHOENIX_RENEWAL) then
      return true
    end

    clazz:sendClassMessage("You've used Phoenix Renewal!")
    clazz:addPotion("REGENERATION", clazz.constants.PHOENIX_DURATION_SECONDS, 2, false, false)
    clazz:restartAbilityCooldown(clazz.abilities.PHOENIX_RENEWAL)
    return false
  end
}
