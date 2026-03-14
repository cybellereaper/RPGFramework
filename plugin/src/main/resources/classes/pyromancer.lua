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
    SCORCH_RANGE_XZ = 4.5,
    SCORCH_RANGE_Y = 3.0,
    SCORCH_FIRE_SECONDS = 5,
    SCORCH_DAMAGE = 2.0,
    SCORCH_MAX_HEAL = 2.0,

    KINDLE_DURATION_SECONDS = 8,

    FIREBALL_SPEED = 0.9,
    FLARE_VOLLEY_OFFSETS = { -0.18, 0.0, 0.18 },

    CAUTERIZE_HEAL = 4.0,
    CAUTERIZE_BUFF_SECONDS = 8,

    PHOENIX_DURATION_SECONDS = 12,
    PHOENIX_COOLDOWN_SECONDS = 180,
    PHOENIX_HEAL = 8.0,
    PHOENIX_DEFENSIVE_REDUCTION = 5.0,

    BASE_FIRE_DAMAGE = 2.5,
    BURNING_TARGET_BONUS_DAMAGE = 2.5,
    KINDLED_BONUS_DAMAGE = 5.0,
    PHOENIX_BONUS_DAMAGE = 5.0,
    PHOENIX_ATTACK_HEAL = 1.0,

    NORMAL_ATTACK_FIRE_SECONDS = 5,
    PHOENIX_ATTACK_FIRE_SECONDS = 8
  },

  isEffectWindowActive = function(clazz, abilityIndex, cooldownSeconds, activeWindowSeconds)
    return clazz:getRemainingCooldown(abilityIndex) >= (cooldownSeconds - activeWindowSeconds)
  end,

  isKindled = function(clazz)
    return clazz:isEffectWindowActive(
      clazz.abilities.KINDLE,
      20,
      clazz.constants.KINDLE_DURATION_SECONDS
    )
  end,

  isPhoenixEmpowered = function(clazz)
    return clazz:isEffectWindowActive(
      clazz.abilities.PHOENIX_RENEWAL,
      clazz.constants.PHOENIX_COOLDOWN_SECONDS,
      clazz.constants.PHOENIX_DURATION_SECONDS
    )
  end,

  onLeftClick = function(clazz, event)
    local item = clazz:getCastItem(event)

    if item == "BLAZE_POWDER" then
      if not clazz:isAbilityReady(clazz.abilities.SCORCH) then
        return
      end

      clazz:sendClassMessage("Scorch erupts around you!")
      clazz:spawnParticle("FLAME", 45, 1.8, 0.6, 1.8, 0.02)
      clazz:playSound("ITEM_FIRECHARGE_USE", 1.0, 0.9)

      local targets = clazz:getNearbyEnemyCasterTargets(
        clazz.constants.SCORCH_RANGE_XZ,
        clazz.constants.SCORCH_RANGE_Y,
        clazz.constants.SCORCH_RANGE_XZ
      )

      local hitCount = 0
      for _, target in ipairs(targets) do
        target:setFireTicks(clazz.constants.SCORCH_FIRE_SECONDS * 20)
        target:damage(clazz.constants.SCORCH_DAMAGE, clazz:getPlayer():getBukkitPlayer())
        clazz:spawnParticleAtEntity(target, "LAVA", 6, 0.3, 0.4, 0.3, 0.01)
        hitCount = hitCount + 1
      end

      if hitCount > 0 then
        clazz:heal(math.min(hitCount, clazz.constants.SCORCH_MAX_HEAL))
      end

      clazz:restartAbilityCooldown(clazz.abilities.SCORCH)
      return
    end

    if item == "BLAZE_ROD" then
      if not clazz:isAbilityReady(clazz.abilities.FLARE_VOLLEY) then
        return
      end

      clazz:sendClassMessage("Flare Volley streaks forward!")

      local offsets = clazz.constants.FLARE_VOLLEY_OFFSETS
      for i = 1, #offsets do
        clazz:launchSmallFireball(offsets[i], clazz.constants.FIREBALL_SPEED)
      end

      clazz:playSound("ENTITY_BLAZE_SHOOT", 1.0, 1.1)
      clazz:restartAbilityCooldown(clazz.abilities.FLARE_VOLLEY)
      return
    end
  end,

  onRightClick = function(clazz, event)
    local item = clazz:getCastItem(event)

    if item == "BLAZE_POWDER" then
      if not clazz:isAbilityReady(clazz.abilities.KINDLE) then
        return
      end

      clazz:sendClassMessage("Kindle empowers your flames!")
      clazz:addPotion("SPEED", clazz.constants.KINDLE_DURATION_SECONDS, 1, false, false)
      clazz:addPotion("FIRE_RESISTANCE", clazz.constants.KINDLE_DURATION_SECONDS, 1, false, false)

      clazz:playSound("ENTITY_BLAZE_AMBIENT", 0.8, 1.3)
      clazz:spawnParticle("FLAME", 30, 0.4, 0.6, 0.4, 0.01)

      clazz:restartAbilityCooldown(clazz.abilities.KINDLE)
      return
    end

    if item == "BLAZE_ROD" then
      if not clazz:isAbilityReady(clazz.abilities.CAUTERIZE) then
        return
      end

      clazz:sendClassMessage("Cauterize seals your wounds.")
      clazz:setFireTicks(0)
      clazz:heal(clazz.constants.CAUTERIZE_HEAL)
      clazz:addPotion("REGENERATION", clazz.constants.CAUTERIZE_BUFF_SECONDS, 2, false, false)
      clazz:addPotion("FIRE_RESISTANCE", clazz.constants.CAUTERIZE_BUFF_SECONDS, 1, false, false)

      clazz:playSound("BLOCK_FIRE_EXTINGUISH", 0.8, 1.4)
      clazz:spawnParticle("ASH", 25, 0.5, 0.8, 0.5, 0.02)

      clazz:restartAbilityCooldown(clazz.abilities.CAUTERIZE)
      return
    end
  end,

  onDeath = function(clazz)
    if not clazz:isAbilityReady(clazz.abilities.PHOENIX_RENEWAL) then
      return true
    end

    clazz:sendClassMessage("You've used Phoenix Renewal!")
    clazz:playSound("ITEM_TOTEM_USE", 0.8, 1.0)
    clazz:spawnParticle("FLAME", 80, 0.6, 0.9, 0.6, 0.03)

    clazz:setFireTicks(0)
    clazz:heal(clazz.constants.PHOENIX_HEAL)
    clazz:addPotion("REGENERATION", clazz.constants.PHOENIX_DURATION_SECONDS, 3, false, false)
    clazz:addPotion("STRENGTH", clazz.constants.PHOENIX_DURATION_SECONDS, 1, false, false)
    clazz:addPotion("SPEED", clazz.constants.PHOENIX_DURATION_SECONDS, 1, false, false)

    clazz:restartAbilityCooldown(clazz.abilities.PHOENIX_RENEWAL)
    return false
  end,

  onDamaged = function(clazz, event)
    clazz:extinguishAndImmuneFire(event)

    if clazz:isPhoenixEmpowered() then
      clazz:reducePrimaryDamage(event, clazz.constants.PHOENIX_DEFENSIVE_REDUCTION)
    end
  end,

  onAttack = function(clazz, event)
    local fireDamage = clazz.constants.BASE_FIRE_DAMAGE
    local target = event:getEntity()

    if target:getFireTicks() > 0 then
      fireDamage = fireDamage + clazz.constants.BURNING_TARGET_BONUS_DAMAGE
    end

    if clazz:isKindled() then
      fireDamage = fireDamage + clazz.constants.KINDLED_BONUS_DAMAGE
    end

    if clazz:isPhoenixEmpowered() then
      fireDamage = fireDamage + clazz.constants.PHOENIX_BONUS_DAMAGE
      clazz:heal(clazz.constants.PHOENIX_ATTACK_HEAL)
    end

    clazz:addFireDamage(event, fireDamage)

    local fireSeconds = clazz.constants.NORMAL_ATTACK_FIRE_SECONDS
    if clazz:isPhoenixEmpowered() then
      fireSeconds = clazz.constants.PHOENIX_ATTACK_FIRE_SECONDS
    end

    target:setFireTicks(math.max(target:getFireTicks(), fireSeconds * 20))
  end
}
