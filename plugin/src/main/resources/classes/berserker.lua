return {
  id = "berserker",
  classItems = { "ROTTEN_FLESH" },
  requiredArmor = {
    CHEST = "LEATHER_CHESTPLATE",
    LEGS = "LEATHER_LEGGINGS"
  },
  cooldowns = { 30, 60, 180 },

  onLeftClick = function(clazz, event)
    if clazz:hasRequiredArmorSet() and clazz:isAbilityReady(1) then
      clazz:sendClassMessage("You've used rage!")
      clazz:addPotion("STRENGTH", 10, 2, false, false)
      clazz:restartAbilityCooldown(1)
    end
  end,

  onRightClick = function(clazz, event)
    if clazz:hasRequiredArmorSet() and clazz:isAbilityReady(0) then
      clazz:sendClassMessage("You've used haste!")
      clazz:addPotion("SPEED", 10, 2, false, false)
      clazz:restartAbilityCooldown(0)
    end
  end,

  onDeath = function(clazz)
    if clazz:isAbilityReady(2) then
      clazz:sendClassMessage("You've used Indomitable Spirit!")
      clazz:addPotion("STRENGTH", 5, 3, false, false)
      clazz:restartAbilityCooldown(2)
      return false
    end

    return true
  end
}
