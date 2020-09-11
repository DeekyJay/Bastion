/**
 * @file voice state update
 * @author Deek
 * @license GPL-3.0
 */

module.exports = async (oldState, newState) => {
  try {
    if (!newState.guild) return;

    if (oldState.guild.id !== newState.guild.id) return;

    let guildModel = await newState.client.database.models.guild.findOne({
      attributes: ['serverLog'],
      where: {
        guildID: newState.guild.id,
      },
    });

    if (!guildModel || !guildModel.dataValues.serverLog) return;

    let logChannel = newState.guild.channels.get(guildModel.dataValues.serverLog);
    if (!logChannel) return;

    let sAction = '';

    if (!oldState.selfMute && newState.selfMute) {
      sAction += 'Muted Self, ';
    }

    if (oldState.selfMute && !newState.selfMute) {
      sAction += 'Unmuted Self, ';
    }

    if (!oldState.selfDeaf && newState.selfDeaf) {
      sAction += 'Deafend Self, ';
    }

    if (oldState.selfDeaf && !newState.selfDeaf) {
      sAction += 'Undeafend Self, ';
    }

    if (!oldState.voiceChannelID && newState.voiceChannelID) {
      sAction += 'Joined, ';
    }

    if (oldState.voiceChannelID && !newState.voiceChannelID) {
      sAction += 'Left, ';
    }

    if (
      oldState.voiceChannelID &&
      newState.voiceChannelID &&
      oldState.voiceChannelID !== newState.voiceChannelID
    ) {
      sAction += `Joined from ${oldState.voiceChannelID}`;
    }

    if (!sAction) return;

    logChannel
      .send({
        embed: {
          color: newState.client.colors.GOLD,
          title: 'Voice Update',
          fields: [
            {
              name: 'Voice Channel ID',
              value: newState.voiceChannelID || oldState.voiceChannelID,
              inline: true,
            },
            {
              name: 'Voice Action Author',
              value: `${newState.user.username}#${newState.user.discriminator}`,
              inline: true,
            },
            {
              name: 'Voice Action Author ID',
              value: newState.user.id,
              inline: true,
            },
            {
              name: 'Action',
              value: sAction.length > 3 ? sAction.substring(0, sAction.length - 2) : 'unknown',
            },
          ],
          timestamp: new Date(),
        },
      })
      .catch((e) => {
        newState.client.log.error(e);
      });
  } catch (e) {
    newState.client.log.error(e);
  }
};
