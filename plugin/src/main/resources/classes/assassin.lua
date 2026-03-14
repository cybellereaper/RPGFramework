return {
  id = "assassin",

  classItems = { "GHAST_TEAR" },

  requiredArmor = {
    HEAD = "LEATHER_HELMET",
    CHEST = "LEATHER_CHESTPLATE",
    LEGS = "LEATHER_LEGGINGS",
    FEET = "LEATHER_BOOTS"
  },

  cooldowns = { 30, 60, 300 },

  abilities = {
    INVISIBILITY = 0,
    POISONED_BLADE = 1,
    HEROIC_DODGE = 2
  },

  constants = {
    PASSIVE_SPEED_SECONDS = 21,
    INVISIBILITY_SECONDS = 10,
    HEROIC_DODGE_SPEED_SECONDS = 10,
    HEROIC_DODGE_TRIGGER_REMAINING = 290,
    INVISIBILITY_TRIGGER_REMAINING = 20,
    RANDOM_DODGE_CHANCE = 0.10,
    POISONED_BLADE_TRIGGER_REMAINING = 50,
    POISONED_BLADE_SECONDS = 10,
    SLASH_DAMAGE_VS_PLAYERS = 5.0,
    SLASH_DAMAGE_VS_MOBS = 10.0
  },

  passive = function(clazz)
    if not clazz:hasRequiredArmorSet() then
      return
    end

    clazz:addPotion("SPEED", clazz.constants.PASSIVE_SPEED_SECONDS, 1, true, false)
  end,

  onLeftClick = function(clazz, event)
    if not clazz:hasRequiredArmorSet() then
      clazz:sendClassMessage("Use full leather armor to use assassin abilities.")
      return
    end

    if not clazz:isAbilityReady(clazz.abilities.POISONED_BLADE) then
      return
    end

    clazz:sendClassMessage("You've used poisoned blade!")
    clazz:restartAbilityCooldown(clazz.abilities.POISONED_BLADE)
  end,

  onRightClick = function(clazz, event)
    if not clazz:hasRequiredArmorSet() then
      clazz:sendClassMessage("Use full leather armor to use assassin abilities.")
      return
    end

    if not clazz:isAbilityReady(clazz.abilities.INVISIBILITY) then
      return
    end

    clazz:sendClassMessage("You've used invisibility!")
    clazz:addPotion("INVISIBILITY", clazz.constants.INVISIBILITY_SECONDS, 1, false, false)
    clazz:restartAbilityCooldown(clazz.abilities.INVISIBILITY)
  end,

  onDeath = function(clazz)
    if not clazz:hasRequiredArmorSet() then
      return true
    end

    if not clazz:isAbilityReady(clazz.abilities.HEROIC_DODGE) then
      return true
    end

    clazz:sendClassMessage("You've used Heroic Dodge!")
    clazz:addPotion("SPEED", clazz.constants.HEROIC_DODGE_SPEED_SECONDS, 2, false, false)
    clazz:restartAbilityCooldown(clazz.abilities.HEROIC_DODGE)
    return false
  end,

  onDamaged = function(clazz, event)
    local heroicDodgeRemaining = clazz:getRemainingAbilityCooldown(clazz.abilities.HEROIC_DODGE)
    local invisibilityRemaining = clazz:getRemainingAbilityCooldown(clazz.abilities.INVISIBILITY)
    local dodgeRoll = clazz:randomUnit()

    if heroicDodgeRemaining >= clazz.constants.HEROIC_DODGE_TRIGGER_REMAINING
        or invisibilityRemaining >= clazz.constants.INVISIBILITY_TRIGGER_REMAINING
        or dodgeRoll <= clazz.constants.RANDOM_DODGE_CHANCE then
      clazz:cancelDamageEvent(event)
    end
  end,

  onAttack = function(clazz, event)
    if not clazz:isPrimaryPhysical(event) then
      return
    end

    if clazz:targetIsPlayer(event) then
      clazz:addSlashDamage(event, clazz.constants.SLASH_DAMAGE_VS_PLAYERS)
    else
      clazz:addSlashDamage(event, clazz.constants.SLASH_DAMAGE_VS_MOBS)
    end

    if clazz:getRemainingAbilityCooldown(clazz.abilities.POISONED_BLADE) >= clazz.constants.POISONED_BLADE_TRIGGER_REMAINING then
      clazz:addPoisonedBladeEffects(event, clazz.constants.POISONED_BLADE_SECONDS)
    end
  end
}
