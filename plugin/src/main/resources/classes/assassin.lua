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
    ARMOR_REQUIRED_MESSAGE = "Use full leather armor to use assassin abilities.",

    INVISIBILITY_DURATION_SECONDS = 10,
    POISONED_BLADE_EFFECT_DURATION_SECONDS = 10,
    HEROIC_DODGE_SPEED_DURATION_SECONDS = 10,
    PASSIVE_SPEED_DURATION_SECONDS = 21,

    INVISIBILITY_AMPLIFIER = 1,
    HEROIC_DODGE_SPEED_AMPLIFIER = 2,
    PASSIVE_SPEED_AMPLIFIER = 1,
    POISONED_BLADE_EFFECT_AMPLIFIER = 0,

    PLAYER_BONUS_DAMAGE = 5.0,
    NON_PLAYER_BONUS_DAMAGE = 10.0,
    RANDOM_DODGE_CHANCE = 0.10,

    HEROIC_DODGE_IMMUNITY_THRESHOLD = 290,
    INVISIBILITY_IMMUNITY_THRESHOLD = 20,
    POISONED_BLADE_ACTIVE_THRESHOLD = 50,

    POISONED_BLADE_EFFECTS = { "BLINDNESS", "POISON", "SLOWNESS" },

    POISONED_BLADE_CAST_MESSAGE = "&aYou've used poisoned blade!",
    INVISIBILITY_CAST_MESSAGE = "&aYou've used invisibility!",
    HEROIC_DODGE_CAST_MESSAGE = "&aYou've used &6Heroic Dodge&a!"
  },

  shouldNegateIncomingDamage = function(clazz, heroicDodgeRemaining, invisibilityRemaining, dodgeRoll)
    return heroicDodgeRemaining >= clazz.constants.HEROIC_DODGE_IMMUNITY_THRESHOLD
      or invisibilityRemaining >= clazz.constants.INVISIBILITY_IMMUNITY_THRESHOLD
      or dodgeRoll <= clazz.constants.RANDOM_DODGE_CHANCE
  end,

  isPoisonedBladeActive = function(clazz)
    return clazz:getRemainingCooldown(clazz.abilities.POISONED_BLADE) >= clazz.constants.POISONED_BLADE_ACTIVE_THRESHOLD
  end,

  bonusDamage = function(clazz, target)
    if target:getType() == "PLAYER" then
      return clazz.constants.PLAYER_BONUS_DAMAGE
    end

    return clazz.constants.NON_PLAYER_BONUS_DAMAGE
  end,

  canUseAbilities = function(clazz)
    if clazz:correctArmor() then
      return true
    end

    clazz:sendClassMessage(clazz.constants.ARMOR_REQUIRED_MESSAGE)
    return false
  end,

  applyPoisonedBladeEffects = function(clazz, target)
    local effects = clazz.constants.POISONED_BLADE_EFFECTS
    for i = 1, #effects do
      target:addPotion(effects[i], clazz.constants.POISONED_BLADE_EFFECT_DURATION_SECONDS, clazz.constants.POISONED_BLADE_EFFECT_AMPLIFIER, false, false)
    end
  end,

  onLeftClick = function(clazz, event)
    if not clazz:canUseAbilities() then
      return
    end

    if not clazz:isAbilityReady(clazz.abilities.POISONED_BLADE) then
      return
    end

    clazz:sendClassMessage(clazz.constants.POISONED_BLADE_CAST_MESSAGE)
    clazz:restartAbilityCooldown(clazz.abilities.POISONED_BLADE)
  end,

  onRightClick = function(clazz, event)
    if not clazz:canUseAbilities() then
      return
    end

    if not clazz:isAbilityReady(clazz.abilities.INVISIBILITY) then
      return
    end

    clazz:sendClassMessage(clazz.constants.INVISIBILITY_CAST_MESSAGE)
    clazz:addPotion("INVISIBILITY", clazz.constants.INVISIBILITY_DURATION_SECONDS, clazz.constants.INVISIBILITY_AMPLIFIER, false, false)
    clazz:restartAbilityCooldown(clazz.abilities.INVISIBILITY)
  end,

  onDeath = function(clazz)
    if not clazz:correctArmor() then
      return true
    end

    if not clazz:isAbilityReady(clazz.abilities.HEROIC_DODGE) then
      return true
    end

    clazz:sendClassMessage(clazz.constants.HEROIC_DODGE_CAST_MESSAGE)
    clazz:playSound("ITEM_TOTEM_USE", 0.8, 1.0)
    clazz:addPotion("SPEED", clazz.constants.HEROIC_DODGE_SPEED_DURATION_SECONDS, clazz.constants.HEROIC_DODGE_SPEED_AMPLIFIER, false, false)
    clazz:restartAbilityCooldown(clazz.abilities.HEROIC_DODGE)

    return false
  end,

  onDamaged = function(clazz, event)
    local dodgeRoll = math.random()

    if clazz:shouldNegateIncomingDamage(
      clazz:getRemainingCooldown(clazz.abilities.HEROIC_DODGE),
      clazz:getRemainingCooldown(clazz.abilities.INVISIBILITY),
      dodgeRoll
    ) then
      event:setCancelled(true)
    end
  end,

  onAttack = function(clazz, event)
    local target = event:getEntity()
    if target == nil then
      return
    end

    if not event:getPrimaryDamage():isPhysical() then
      return
    end

    clazz:addDamage(event, clazz:bonusDamage(target), "SLASH")

    if not clazz:isPoisonedBladeActive() then
      return
    end

    clazz:applyPoisonedBladeEffects(target)
  end,

  passive = function(clazz)
    if not clazz:correctArmor() then
      return
    end

    clazz:addPotion("SPEED", clazz.constants.PASSIVE_SPEED_DURATION_SECONDS, clazz.constants.PASSIVE_SPEED_AMPLIFIER, true, false)
  end
}