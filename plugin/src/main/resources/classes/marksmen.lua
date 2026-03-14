return {
  id = "gunslinger",

  classItems = { "CROSSBOW", "BOW" },

  requiredArmor = {
    HEAD = "CHAINMAIL_HELMET",
    CHEST = "LEATHER_CHESTPLATE",
    LEGS = "LEATHER_LEGGINGS",
    FEET = "CHAINMAIL_BOOTS"
  },

  cooldowns = { 10, 18, 16, 28, 180 },

  abilities = {
    QUICKDRAW = 0,
    DEAD_EYE = 1,
    VOLLEY_FIRE = 2,
    SMOKESCREEN = 3,
    LAST_STAND = 4
  },

  constants = {
    QUICKDRAW_RANGE_XZ = 5.0,
    QUICKDRAW_RANGE_Y = 3.0,
    QUICKDRAW_BONUS_DAMAGE = 4.0,
    QUICKDRAW_SLOW_SECONDS = 2,

    DEAD_EYE_DURATION_SECONDS = 8,
    DEAD_EYE_SPEED_AMPLIFIER = 1,

    VOLLEY_FIRE_RANGE_XZ = 7.0,
    VOLLEY_FIRE_RANGE_Y = 3.5,
    VOLLEY_FIRE_DAMAGE = 3.0,
    VOLLEY_FIRE_MAX_TARGETS = 3,

    SMOKESCREEN_RADIUS = 4.0,
    SMOKESCREEN_BLIND_SECONDS = 4,
    SMOKESCREEN_SLOW_SECONDS = 5,
    SMOKESCREEN_SPEED_SECONDS = 5,
    SMOKESCREEN_HEAL = 3.0,

    LAST_STAND_DURATION_SECONDS = 10,
    LAST_STAND_COOLDOWN_SECONDS = 180,
    LAST_STAND_HEAL = 8.0,
    LAST_STAND_RESISTANCE = 6.0,
    LAST_STAND_ATTACK_HEAL = 1.0,

    BASE_RANGED_DAMAGE = 2.5,
    DEAD_EYE_BONUS_DAMAGE = 4.0,
    DISTANT_TARGET_BONUS_DAMAGE = 2.0,
    LAST_STAND_BONUS_DAMAGE = 5.0,

    PASSIVE_SPEED_SECONDS = 6,
    PASSIVE_SPEED_AMPLIFIER = 0,

    DISTANT_TARGET_RANGE = 12.0
  },

  isEffectWindowActive = function(clazz, abilityIndex, cooldownSeconds, activeWindowSeconds)
    return clazz:getRemainingCooldown(abilityIndex) >= (cooldownSeconds - activeWindowSeconds)
  end,

  isDeadEyeActive = function(clazz)
    return clazz:isEffectWindowActive(
      clazz.abilities.DEAD_EYE,
      18,
      clazz.constants.DEAD_EYE_DURATION_SECONDS
    )
  end,

  isLastStandActive = function(clazz)
    return clazz:isEffectWindowActive(
      clazz.abilities.LAST_STAND,
      clazz.constants.LAST_STAND_COOLDOWN_SECONDS,
      clazz.constants.LAST_STAND_DURATION_SECONDS
    )
  end,

  getTargetDistance = function(clazz, target)
    local player = clazz:getPlayer():getBukkitPlayer()
    if player == nil or target == nil then
      return 0.0
    end

    return player:getLocation():distance(target:getLocation())
  end,

  isDistantTarget = function(clazz, target)
    return clazz:getTargetDistance(target) >= clazz.constants.DISTANT_TARGET_RANGE
  end,

  onLeftClick = function(clazz, event)
    local item = clazz:getCastItem(event)

    if item == "CROSSBOW" then
      if not clazz:isAbilityReady(clazz.abilities.QUICKDRAW) then
        return
      end

      clazz:sendClassMessage("Quickdraw snaps a shot into your foe!")
      clazz:playSound("ENTITY_ARROW_SHOOT", 1.0, 1.2)

      local targets = clazz:getNearbyEnemyCasterTargets(
        clazz.constants.QUICKDRAW_RANGE_XZ,
        clazz.constants.QUICKDRAW_RANGE_Y,
        clazz.constants.QUICKDRAW_RANGE_XZ
      )

      for _, target in ipairs(targets) do
        target:damage(clazz.constants.QUICKDRAW_BONUS_DAMAGE, clazz:getPlayer():getBukkitPlayer())
        target:addPotion("SLOWNESS", clazz.constants.QUICKDRAW_SLOW_SECONDS, 1, false, false)
        clazz:spawnParticleAtEntity(target, "CRIT", 12, 0.3, 0.5, 0.3, 0.01)
        break
      end

      clazz:restartAbilityCooldown(clazz.abilities.QUICKDRAW)
      return
    end

    if item == "BOW" then
      if not clazz:isAbilityReady(clazz.abilities.VOLLEY_FIRE) then
        return
      end

      clazz:sendClassMessage("Volley Fire peppers the battlefield!")
      clazz:playSound("ENTITY_ARROW_SHOOT", 1.0, 0.9)

      local targets = clazz:getNearbyEnemyCasterTargets(
        clazz.constants.VOLLEY_FIRE_RANGE_XZ,
        clazz.constants.VOLLEY_FIRE_RANGE_Y,
        clazz.constants.VOLLEY_FIRE_RANGE_XZ
      )

      local hitCount = 0
      for _, target in ipairs(targets) do
        target:damage(clazz.constants.VOLLEY_FIRE_DAMAGE, clazz:getPlayer():getBukkitPlayer())
        clazz:spawnParticleAtEntity(target, "CRIT", 10, 0.35, 0.45, 0.35, 0.01)
        hitCount = hitCount + 1

        if hitCount >= clazz.constants.VOLLEY_FIRE_MAX_TARGETS then
          break
        end
      end

      clazz:restartAbilityCooldown(clazz.abilities.VOLLEY_FIRE)
      return
    end

    clazz:sendClassMessage("Quickdraw snaps a shot into your foe!")
    clazz:restartAbilityCooldown(clazz.abilities.QUICKDRAW)
  end,

  onRightClick = function(clazz, event)
    local item = clazz:getCastItem(event)

    if item == "CROSSBOW" then
      if not clazz:isAbilityReady(clazz.abilities.DEAD_EYE) then
        return
      end

      clazz:sendClassMessage("Dead Eye sharpens your aim!")
      clazz:addPotion("SPEED", clazz.constants.DEAD_EYE_DURATION_SECONDS, clazz.constants.DEAD_EYE_SPEED_AMPLIFIER, false, false)

      clazz:playSound("BLOCK_NOTE_BLOCK_SNARE", 0.9, 1.3)
      clazz:spawnParticle("CRIT", 20, 0.4, 0.6, 0.4, 0.01)

      clazz:restartAbilityCooldown(clazz.abilities.DEAD_EYE)
      return
    end

    if item == "BOW" then
      if not clazz:isAbilityReady(clazz.abilities.SMOKESCREEN) then
        return
      end

      clazz:sendClassMessage("Smokescreen covers your retreat!")
      clazz:spawnParticle("CLOUD", 55, 1.2, 0.8, 1.2, 0.03)
      clazz:playSound("ENTITY_GENERIC_EXTINGUISH_FIRE", 0.8, 1.2)

      local targets = clazz:getNearbyEnemyCasterTargets(
        clazz.constants.SMOKESCREEN_RADIUS,
        3.0,
        clazz.constants.SMOKESCREEN_RADIUS
      )

      for _, target in ipairs(targets) do
        target:addPotion("BLINDNESS", clazz.constants.SMOKESCREEN_BLIND_SECONDS, 0, false, false)
        target:addPotion("SLOWNESS", clazz.constants.SMOKESCREEN_SLOW_SECONDS, 1, false, false)
      end

      clazz:addPotion("SPEED", clazz.constants.SMOKESCREEN_SPEED_SECONDS, 1, false, false)
      clazz:heal(clazz.constants.SMOKESCREEN_HEAL)
      clazz:restartAbilityCooldown(clazz.abilities.SMOKESCREEN)
      return
    end

    clazz:sendClassMessage("Dead Eye sharpens your aim!")
    clazz:addPotion("SPEED", clazz.constants.DEAD_EYE_DURATION_SECONDS, clazz.constants.DEAD_EYE_SPEED_AMPLIFIER, false, false)
    clazz:restartAbilityCooldown(clazz.abilities.DEAD_EYE)
  end,

  onDeath = function(clazz)
    if not clazz:isAbilityReady(clazz.abilities.LAST_STAND) then
      return true
    end

    clazz:sendClassMessage("You've used Last Stand!")
    clazz:playSound("ITEM_TOTEM_USE", 0.8, 1.0)
    clazz:spawnParticle("SMOKE", 70, 0.7, 0.9, 0.7, 0.04)

    clazz:heal(clazz.constants.LAST_STAND_HEAL)
    clazz:addPotion("SPEED", clazz.constants.LAST_STAND_DURATION_SECONDS, 1, false, false)
    clazz:addPotion("STRENGTH", clazz.constants.LAST_STAND_DURATION_SECONDS, 1, false, false)

    clazz:restartAbilityCooldown(clazz.abilities.LAST_STAND)
    return false
  end,

  onDamaged = function(clazz, event)
    if clazz:isLastStandActive() then
      clazz:reducePrimaryDamage(event, clazz.constants.LAST_STAND_RESISTANCE)
    end
  end,

  onAttack = function(clazz, event)
    local target = event:getEntity()
    local bonusDamage = clazz.constants.BASE_RANGED_DAMAGE

    if target == nil then
      return
    end

    if clazz:isDeadEyeActive() then
      bonusDamage = bonusDamage + clazz.constants.DEAD_EYE_BONUS_DAMAGE
    end

    if clazz:isDistantTarget(target) then
      bonusDamage = bonusDamage + clazz.constants.DISTANT_TARGET_BONUS_DAMAGE
    end

    if clazz:isLastStandActive() then
      bonusDamage = bonusDamage + clazz.constants.LAST_STAND_BONUS_DAMAGE
      clazz:heal(clazz.constants.LAST_STAND_ATTACK_HEAL)
    end

    clazz:addDamage(event, bonusDamage, "PUNCTURE")
  end,

  passive = function(clazz)
    clazz:addPotion("SPEED", clazz.constants.PASSIVE_SPEED_SECONDS, clazz.constants.PASSIVE_SPEED_AMPLIFIER, true, false)
  end
}