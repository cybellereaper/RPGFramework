return {
  id = "bard",

  classItems = { "NOTE_BLOCK" },

  cooldowns = { 30, 300 },

  abilities = {
    HYM = 0,
    LIFE_OF_MUSIC = 1
  },

  buffs = {
    REGENERATION = 0,
    SWIFTNESS = 1,
    STRENGTH = 2
  },

  constants = {
    HYM_DURATION_SECONDS = 45,
    HYM_AMPLIFIER = 1,

    LIFE_OF_MUSIC_HEAL_AMOUNT = 10.0,
    LIFE_OF_MUSIC_DURATION_SECONDS = 10,
    LIFE_OF_MUSIC_AMPLIFIER = 2,

    TOTEM_SOUND_VOLUME = 0.8,
    TOTEM_SOUND_PITCH = 1.0,

    BLOCK_PLACEMENT_WARNING = "Shift + right click if you are trying to place that block.",
    HYM_CAST_MESSAGE = "&aYou've used hym!",
    LIFE_OF_MUSIC_CAST_MESSAGE = "&aYou've used &6A Life of Music&a!",
    HYM_BROADCAST_SUFFIX = "&a has used hym!",

    BUFF_DISPLAY_NAMES = {
      "&dRegeneration",
      "&bSwiftness",
      "&cStrength"
    },

    BUFF_POTION_TYPES = {
      "REGENERATION",
      "SPEED",
      "STRENGTH"
    }
  },

  selectedBuff = 0,

  getSelectedBuffIndex = function(clazz)
    if clazz.selectedBuff == nil then
      clazz.selectedBuff = clazz.buffs.REGENERATION
    end

    return clazz.selectedBuff
  end,

  getSelectedBuffDisplayName = function(clazz)
    local index = clazz:getSelectedBuffIndex()
    return clazz.constants.BUFF_DISPLAY_NAMES[index + 1]
  end,

  getSelectedBuffPotionType = function(clazz)
    local index = clazz:getSelectedBuffIndex()
    return clazz.constants.BUFF_POTION_TYPES[index + 1]
  end,

  cycleSelectedBuff = function(clazz)
    local current = clazz:getSelectedBuffIndex()

    if current == clazz.buffs.REGENERATION then
      clazz.selectedBuff = clazz.buffs.SWIFTNESS
      return
    end

    if current == clazz.buffs.SWIFTNESS then
      clazz.selectedBuff = clazz.buffs.STRENGTH
      return
    end

    clazz.selectedBuff = clazz.buffs.REGENERATION
  end,

  shouldPreventBlockPlacement = function(clazz, event)
    if event:getAction():name() ~= "RIGHT_CLICK_BLOCK" then
      return false
    end

    if event:getPlayer():isSneaking() then
      return false
    end

    event:setCancelled(true)
    clazz:sendClassMessage(clazz.constants.BLOCK_PLACEMENT_WARNING)
    return true
  end,

  applyHymBuff = function(clazz, target)
    target:addPotion(
      clazz:getSelectedBuffPotionType(),
      clazz.constants.HYM_DURATION_SECONDS,
      clazz.constants.HYM_AMPLIFIER,
      false,
      false
    )
  end,

  applyLifeOfMusicBuffs = function(clazz, target)
    target:addPotion("REGENERATION", clazz.constants.LIFE_OF_MUSIC_DURATION_SECONDS, clazz.constants.LIFE_OF_MUSIC_AMPLIFIER, false, false)
    target:addPotion("SPEED", clazz.constants.LIFE_OF_MUSIC_DURATION_SECONDS, clazz.constants.LIFE_OF_MUSIC_AMPLIFIER, false, false)
    target:addPotion("STRENGTH", clazz.constants.LIFE_OF_MUSIC_DURATION_SECONDS, clazz.constants.LIFE_OF_MUSIC_AMPLIFIER, false, false)
  end,

  onRightClick = function(clazz, event)
    if clazz:shouldPreventBlockPlacement(event) then
      return
    end

    clazz:cycleSelectedBuff()
    clazz:sendClassMessage(clazz:getSelectedBuffDisplayName())
  end,

  onLeftClick = function(clazz, event)
    if not clazz:isAbilityReady(clazz.abilities.HYM) then
      return
    end

    local allies = clazz:friendlyCasterTargets()
    local casterDisplayName = clazz:getPlayerRarity() .. clazz:getPlayer():getName()

    clazz:sendClassMessage(clazz.constants.HYM_CAST_MESSAGE)

    for _, ally in ipairs(allies) do
      clazz:applyHymBuff(ally)
      ally:sendMessage(casterDisplayName .. clazz.constants.HYM_BROADCAST_SUFFIX)
    end

    clazz:restartAbilityCooldown(clazz.abilities.HYM)
  end,

  onDeath = function(clazz)
    if not clazz:isAbilityReady(clazz.abilities.LIFE_OF_MUSIC) then
      return true
    end

    clazz:sendClassMessage(clazz.constants.LIFE_OF_MUSIC_CAST_MESSAGE)
    clazz:playSound("ITEM_TOTEM_USE", clazz.constants.TOTEM_SOUND_VOLUME, clazz.constants.TOTEM_SOUND_PITCH)

    clazz:heal(clazz.constants.LIFE_OF_MUSIC_HEAL_AMOUNT)
    clazz:applyLifeOfMusicBuffs(clazz)
    clazz:restartAbilityCooldown(clazz.abilities.LIFE_OF_MUSIC)

    return false
  end
}