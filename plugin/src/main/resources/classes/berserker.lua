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
    RAGE_SECONDS = 10,
    HASTE_SECONDS = 10,
    INDOMITABLE_SPIRIT_SECONDS = 5
  },

  onLeftClick = function(clazz, event)
    if not clazz:hasRequiredArmorSet() then
      return
    end

    if not clazz:isAbilityReady(clazz.abilities.RAGE) then
      return
    end

    clazz:sendClassMessage("You've used rage!")
    clazz:addPotion("STRENGTH", clazz.constants.RAGE_SECONDS, 2, false, false)
    clazz:restartAbilityCooldown(clazz.abilities.RAGE)
  end,

  onRightClick = function(clazz, event)
    if not clazz:hasRequiredArmorSet() then
      return
    end

    if not clazz:isAbilityReady(clazz.abilities.HASTE) then
      return
    end

    clazz:sendClassMessage("You've used haste!")
    clazz:addPotion("SPEED", clazz.constants.HASTE_SECONDS, 2, false, false)
    clazz:restartAbilityCooldown(clazz.abilities.HASTE)
  end,

  onDeath = function(clazz)
    if not clazz:isAbilityReady(clazz.abilities.INDOMITABLE_SPIRIT) then
      return true
    end

    clazz:sendClassMessage("You've used Indomitable Spirit!")
    clazz:addPotion("STRENGTH", clazz.constants.INDOMITABLE_SPIRIT_SECONDS, 3, false, false)
    clazz:restartAbilityCooldown(clazz.abilities.INDOMITABLE_SPIRIT)
    return false
  end
}
