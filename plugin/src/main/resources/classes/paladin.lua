return {
  id = "paladin",

  classItems = { "GOLDEN_SHOVEL" },

  requiredArmor = {
    HEAD = "GOLDEN_HELMET",
    CHEST = "GOLDEN_CHESTPLATE",
    LEGS = "GOLDEN_LEGGINGS",
    FEET = "GOLDEN_BOOTS"
  },

  cooldowns = { 45, 60, 300 },

  abilities = {
    PURIFY = 0,
    MEND = 1,
    PROTECTION_OF_THE_HEALER = 2
  },

  constants = {
    ARMOR_REQUIRED_MESSAGE = "Use full golden armor to use paladin spells.",

    MEND_DURATION_SECONDS = 15,
    MEND_AMPLIFIER = 3,

    PURIFY_HEAL_AMOUNT = 5.0,

    PROTECTION_DURATION_SECONDS = 10,
    PROTECTION_REGEN_AMPLIFIER = 4,
    PROTECTION_HEALTH_BOOST_AMPLIFIER = 1,
    PROTECTION_HEAL_AMOUNT = 2.0,

    PALADIN_RESISTANCES = {
      "HOLY",
      "IMPACT",
      "SLASH",
      "UNSPECIFIED",
      "PUNCTURE"
    },

    NEGATIVE_EFFECTS_TO_CLEANSE = {
      "BLINDNESS",
      "BAD_OMEN",
      "NAUSEA",
      "DARKNESS",
      "INSTANT_DAMAGE",
      "HUNGER",
      "POISON",
      "SLOWNESS",
      "LEVITATION",
      "MINING_FATIGUE",
      "UNLUCK",
      "WEAKNESS",
      "WITHER"
    },

    MEND_CAST_MESSAGE = "&aYou've used mend!",
    PURIFY_CAST_MESSAGE = "&aYou've used purify!",
    PROTECTION_CAST_MESSAGE = "&aYou've used &6Protection of the Healer&a!",

    MEND_BROADCAST_SUFFIX = "&a has used mend!",
    PURIFY_BROADCAST_SUFFIX = "&a has used purify!",
    PROTECTION_BROADCAST_SUFFIX = "&a has used &6Protection of the Healer&a!",

    TOTEM_SOUND_VOLUME = 0.8,
    TOTEM_SOUND_PITCH = 1.0,

    ATTACK_DAMAGE_REDUCTION = 2.5
  },

  canCastSpells = function(clazz)
    if clazz:correctArmor() then
      return true
    end

    clazz:sendClassMessage(clazz.constants.ARMOR_REQUIRED_MESSAGE)
    return false
  end,

  getCasterDisplayName = function(clazz)
    return clazz:getPlayerRarity() .. clazz:getPlayer():getName()
  end,

  applyProtectionOfTheHealer = function(clazz, ally, casterDisplayName)
    ally:addPotion("REGENERATION", clazz.constants.PROTECTION_DURATION_SECONDS, clazz.constants.PROTECTION_REGEN_AMPLIFIER, false, false)
    ally:addPotion("HEALTH_BOOST", clazz.constants.PROTECTION_DURATION_SECONDS, clazz.constants.PROTECTION_HEALTH_BOOST_AMPLIFIER, false, false)
    ally:heal(clazz.constants.PROTECTION_HEAL_AMOUNT)
    ally:sendMessage(casterDisplayName .. clazz.constants.PROTECTION_BROADCAST_SUFFIX)
  end,

  onLeftClick = function(clazz, event)
    local item = clazz:getCastItem(event)
    if item ~= "GOLDEN_SHOVEL" then
      return
    end

    if not clazz:canCastSpells() then
      return
    end

    if not clazz:isAbilityReady(clazz.abilities.MEND) then
      return
    end

    local allies = clazz:friendlyCasterTargets()
    local casterDisplayName = clazz:getCasterDisplayName()

    clazz:sendClassMessage(clazz.constants.MEND_CAST_MESSAGE)

    for _, ally in ipairs(allies) do
      ally:addPotion("REGENERATION", clazz.constants.MEND_DURATION_SECONDS, clazz.constants.MEND_AMPLIFIER, false, false)
      ally:sendMessage(casterDisplayName .. clazz.constants.MEND_BROADCAST_SUFFIX)
    end

    clazz:restartAbilityCooldown(clazz.abilities.MEND)
  end,

  onRightClick = function(clazz, event)
    local item = clazz:getCastItem(event)
    if item ~= "GOLDEN_SHOVEL" then
      return
    end

    if not clazz:canCastSpells() then
      return
    end

    if not clazz:isAbilityReady(clazz.abilities.PURIFY) then
      return
    end

    local allies = clazz:friendlyCasterTargets()
    local casterDisplayName = clazz:getCasterDisplayName()

    clazz:sendClassMessage(clazz.constants.PURIFY_CAST_MESSAGE)

    for _, ally in ipairs(allies) do
      ally:heal(clazz.constants.PURIFY_HEAL_AMOUNT)
      ally:setFireTicks(0)
      ally:cleanseEffects(clazz.constants.NEGATIVE_EFFECTS_TO_CLEANSE)
      ally:sendMessage(casterDisplayName .. clazz.constants.PURIFY_BROADCAST_SUFFIX)
    end

    clazz:restartAbilityCooldown(clazz.abilities.PURIFY)
  end,

  onDeath = function(clazz)
    if not clazz:isAbilityReady(clazz.abilities.PROTECTION_OF_THE_HEALER) then
      return true
    end

    local allies = clazz:friendlyCasterTargets()
    local casterDisplayName = clazz:getCasterDisplayName()

    clazz:sendClassMessage(clazz.constants.PROTECTION_CAST_MESSAGE)
    clazz:playSound("ITEM_TOTEM_USE", clazz.constants.TOTEM_SOUND_VOLUME, clazz.constants.TOTEM_SOUND_PITCH)

    for _, ally in ipairs(allies) do
      clazz:applyProtectionOfTheHealer(ally, casterDisplayName)
    end

    clazz:restartAbilityCooldown(clazz.abilities.PROTECTION_OF_THE_HEALER)
    return false
  end,

  onDamaged = function(clazz, event)
    local resistances = clazz.constants.PALADIN_RESISTANCES

    for i = 1, #resistances do
      event:setResistance(resistances[i], "RESISTANCE")
    end
  end,

  onAttack = function(clazz, event)
    clazz:addDamage(event, -clazz.constants.ATTACK_DAMAGE_REDUCTION, event:getPrimaryDamage())
  end
}