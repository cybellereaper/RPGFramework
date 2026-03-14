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

  passive = function(clazz)
    if clazz:hasRequiredArmorSet() then
      clazz:addPotion("SPEED", 21, 1, true, false)
    end
  end,

  onLeftClick = function(clazz, event)
    if not clazz:hasRequiredArmorSet() then
      clazz:sendClassMessage("Use full leather armor to use assassin abilities.")
      return
    end

    if clazz:isAbilityReady(1) then
      clazz:sendClassMessage("You've used poisoned blade!")
      clazz:restartAbilityCooldown(1)
    end
  end,

  onRightClick = function(clazz, event)
    if not clazz:hasRequiredArmorSet() then
      clazz:sendClassMessage("Use full leather armor to use assassin abilities.")
      return
    end

    if clazz:isAbilityReady(0) then
      clazz:sendClassMessage("You've used invisibility!")
      clazz:addPotion("INVISIBILITY", 10, 1, false, false)
      clazz:restartAbilityCooldown(0)
    end
  end,

  onDeath = function(clazz)
    if not clazz:hasRequiredArmorSet() then
      return true
    end

    if not clazz:isAbilityReady(2) then
      return true
    end

    clazz:sendClassMessage("You've used Heroic Dodge!")
    clazz:addPotion("SPEED", 10, 2, false, false)
    clazz:restartAbilityCooldown(2)
    return false
  end,

  onDamaged = function(clazz, event)
    local heroicDodgeRemaining = clazz:getRemainingAbilityCooldown(2)
    local invisibilityRemaining = clazz:getRemainingAbilityCooldown(0)
    local dodgeRoll = clazz:randomUnit()

    if heroicDodgeRemaining >= 290 or invisibilityRemaining >= 20 or dodgeRoll <= 0.10 then
      clazz:cancelDamageEvent(event)
    end
  end,

  onAttack = function(clazz, event)
    if not clazz:isPrimaryPhysical(event) then
      return
    end

    if clazz:targetIsPlayer(event) then
      clazz:addSlashDamage(event, 5.0)
    else
      clazz:addSlashDamage(event, 10.0)
    end

    if clazz:getRemainingAbilityCooldown(1) >= 50 then
      clazz:addPoisonedBladeEffects(event, 10)
    end
  end
}
