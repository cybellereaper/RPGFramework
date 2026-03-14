return {
  id = "berserker",

  classItems = { "ROTTEN_FLESH" },

  requiredArmor = {
    CHEST = "LEATHER_CHESTPLATE",
    LEGS = "LEATHER_LEGGINGS"
  },

  cooldowns = { 30, 60, 180 },

  abilities = {
    HASTE = 0,
    RAGE = 1,
    INDOMITABLE_SPIRIT = 2
  },

  constants = {
    ARMOR_REQUIRED_MESSAGE = "Use leather middle pieces to use abilities.",

    HASTE_DURATION_SECONDS = 10,
    HASTE_AMPLIFIER = 2,

    RAGE_DURATION_SECONDS = 10,
    RAGE_AMPLIFIER = 2,

    INDOMITABLE_SPIRIT_COOLDOWN_SECONDS = 180,
    INDOMITABLE_SPIRIT_ACTIVE_SECONDS = 5,
    INDOMITABLE_SPIRIT_STRENGTH_DURATION_SECONDS = 5,
    INDOMITABLE_SPIRIT_STRENGTH_AMPLIFIER = 3,

    DEFENSIVE_DAMAGE_REDUCTION = 10.0,
    BONUS_AXE_DAMAGE = 10.0,

    VALID_AXES = {
      WOODEN_AXE = true,
      STONE_AXE = true,
      IRON_AXE = true,
      DIAMOND_AXE = true,
      NETHERITE_AXE = true,
      GOLDEN_AXE = true
    },

    HASTE_CAST_MESSAGE = "&aYou've used haste!",
    RAGE_CAST_MESSAGE = "&aYou've used rage!",
    INDOMITABLE_SPIRIT_CAST_MESSAGE = "&aYou've used &6Indomitable Spirit&a!",

    TOTEM_SOUND_VOLUME = 0.8,
    TOTEM_SOUND_PITCH = 1.0
  },

  canUseAbilities = function(clazz)
    if clazz:correctArmor() then
      return true
    end

    clazz:sendClassMessage(clazz.constants.ARMOR_REQUIRED_MESSAGE)
    return false
  end,

  isUsingAxe = function(clazz)
    local heldItem = clazz:getHeldItem()
    if heldItem == nil then
      return false
    end

    return clazz.constants.VALID_AXES[heldItem] == true
  end,

  isIndomitableSpiritActive = function(clazz)
    local remainingCooldown = clazz:getRemainingCooldown(clazz.abilities.INDOMITABLE_SPIRIT)
    return remainingCooldown >= (clazz.constants.INDOMITABLE_SPIRIT_COOLDOWN_SECONDS - clazz.constants.INDOMITABLE_SPIRIT_ACTIVE_SECONDS)
  end,

  onLeftClick = function(clazz, event)
    local item = clazz:getCastItem(event)
    if item ~= "ROTTEN_FLESH" then
      return
    end

    if not clazz:canUseAbilities() then
      return
    end

    if not clazz:isAbilityReady(clazz.abilities.RAGE) then
      return
    end

    clazz:sendClassMessage(clazz.constants.RAGE_CAST_MESSAGE)
    clazz:addPotion("STRENGTH", clazz.constants.RAGE_DURATION_SECONDS, clazz.constants.RAGE_AMPLIFIER, false, false)
    clazz:restartAbilityCooldown(clazz.abilities.RAGE)
  end,

  onRightClick = function(clazz, event)
    local item = clazz:getCastItem(event)
    if item ~= "ROTTEN_FLESH" then
      return
    end

    if not clazz:canUseAbilities() then
      return
    end

    if not clazz:isAbilityReady(clazz.abilities.HASTE) then
      return
    end

    clazz:sendClassMessage(clazz.constants.HASTE_CAST_MESSAGE)
    clazz:addPotion("SPEED", clazz.constants.HASTE_DURATION_SECONDS, clazz.constants.HASTE_AMPLIFIER, false, false)
    clazz:restartAbilityCooldown(clazz.abilities.HASTE)
  end,

  onDeath = function(clazz)
    if not clazz:isAbilityReady(clazz.abilities.INDOMITABLE_SPIRIT) then
      return true
    end

    clazz:sendClassMessage(clazz.constants.INDOMITABLE_SPIRIT_CAST_MESSAGE)
    clazz:playSound("ITEM_TOTEM_USE", clazz.constants.TOTEM_SOUND_VOLUME, clazz.constants.TOTEM_SOUND_PITCH)
    clazz:addPotion(
      "STRENGTH",
      clazz.constants.INDOMITABLE_SPIRIT_STRENGTH_DURATION_SECONDS,
      clazz.constants.INDOMITABLE_SPIRIT_STRENGTH_AMPLIFIER,
      false,
      false
    )
    clazz:restartAbilityCooldown(clazz.abilities.INDOMITABLE_SPIRIT)

    return false
  end,

  onDamaged = function(clazz, event)
    if clazz:correctArmor() then
      clazz:reducePrimaryDamage(event, clazz.constants.DEFENSIVE_DAMAGE_REDUCTION)
    end

    if clazz:isIndomitableSpiritActive() then
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

    if not clazz:isUsingAxe() then
      return
    end

    clazz:addDamage(event, clazz.constants.BONUS_AXE_DAMAGE, "SLASH")
  end
}