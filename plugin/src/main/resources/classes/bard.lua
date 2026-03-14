return {
  id = "bard",
  classItems = { "NOTE_BLOCK" },
  requiredArmor = {},
  cooldowns = { 30, 300 },

  onLeftClick = function(clazz, event)
    if clazz:isAbilityReady(0) then
      clazz:sendClassMessage("You've used hym!")
      clazz:restartAbilityCooldown(0)
    end
  end,

  onDeath = function(clazz)
    if not clazz:isAbilityReady(1) then
      return true
    end

    clazz:sendClassMessage("You've used A Life of Music!")
    clazz:addPotion("REGENERATION", 10, 2, false, false)
    clazz:addPotion("SPEED", 10, 2, false, false)
    clazz:addPotion("STRENGTH", 10, 2, false, false)
    clazz:restartAbilityCooldown(1)
    return false
  end
}
