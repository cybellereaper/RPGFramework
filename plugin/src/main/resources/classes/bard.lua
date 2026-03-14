return {
  id = "bard",

  classItems = { "NOTE_BLOCK" },

  requiredArmor = {},

  cooldowns = { 30, 300 },

  abilities = {
    HYMN = 0,
    A_LIFE_OF_MUSIC = 1
  },

  constants = {
    DEATH_BUFF_SECONDS = 10
  },

  onLeftClick = function(clazz, event)
    if not clazz:isAbilityReady(clazz.abilities.HYMN) then
      return
    end

    clazz:sendClassMessage("You've used hym!")
    clazz:restartAbilityCooldown(clazz.abilities.HYMN)
  end,

  onDeath = function(clazz)
    if not clazz:isAbilityReady(clazz.abilities.A_LIFE_OF_MUSIC) then
      return true
    end

    clazz:sendClassMessage("You've used A Life of Music!")
    clazz:addPotion("REGENERATION", clazz.constants.DEATH_BUFF_SECONDS, 2, false, false)
    clazz:addPotion("SPEED", clazz.constants.DEATH_BUFF_SECONDS, 2, false, false)
    clazz:addPotion("STRENGTH", clazz.constants.DEATH_BUFF_SECONDS, 2, false, false)
    clazz:restartAbilityCooldown(clazz.abilities.A_LIFE_OF_MUSIC)
    return false
  end
}
