/**
 * @file rainbow6 command
 * @author Sankarsan Kampa (a.k.a k3rn31p4nic)
 * @license GPL-3.0
 */

const request = xrequire('request-promise-native');

exports.exec = async (Bastion, message, args) => {
  if (args.length < 2) {
    return Bastion.emit('commandUsage', message, this.help);
  }
  if (!/^(pc|ps4|xone)$/.test((args[0] = args[0].toLowerCase()))) {
    return Bastion.emit(
      'error',
      '',
      Bastion.i18n.error(message.guild.language, 'invalidPlatform', '`PC`, `PS4` and `XOne`'),
      message.channel,
    );
  }
  if (!/^[a-zA-Z][\w-. ]{2,14}$/.test((args[1] = args.slice(1).join(' ')))) {
    return Bastion.emit(
      'error',
      '',
      Bastion.i18n.error(message.guild.language, 'invalidInput', 'username'),
      message.channel,
    );
  }

  let options = {
    url: `https://api2.r6stats.com/public-api/stats/${args[1]}/${args[0]}/generic`,
    json: true,
    headers: {
      Authorization: `Bearer ${Bastion.credentials.r6statsAPIKey}`,
    },
  };

  console.log(options);

  const data = await request(options);

  let stats = [
    {
      name: 'Player Name',
      value: data.username,
    },
    {
      name: 'Level',
      value: `${data.progression.level}`,
      inline: true,
    },
    {
      name: 'XP',
      value: `${data.progression.total_xp}`,
      inline: true,
    },
  ];
  if (
    data.stats.queue.ranked &&
    (data.stats.queue.ranked.wins !== 0 || data.stats.queue.ranked.losses !== 0)
  ) {
    stats.push(
      {
        name: 'Ranked',
        value: `${args[1]} has played Ranked games for **${(
          data.stats.queue.ranked.playtime /
          60 /
          60
        ).toFixed(2)}** Hours.`,
      },
      {
        name: 'Wins',
        value: `${data.stats.queue.ranked.wins}`,
        inline: true,
      },
      {
        name: 'Losses',
        value: `${data.stats.queue.ranked.losses}`,
        inline: true,
      },
      {
        name: 'Kills',
        value: `${data.stats.queue.ranked.kills}`,
        inline: true,
      },
      {
        name: 'Deaths',
        value: `${data.stats.queue.ranked.deaths}`,
        inline: true,
      },
      {
        name: 'Win/Lose Ratio',
        value: `${data.stats.queue.ranked.wl}`,
        inline: true,
      },
      {
        name: 'Kill/Death Ratio',
        value: `${data.stats.queue.ranked.kd}`,
        inline: true,
      },
    );
  } else {
    stats.push({
      name: 'Ranked',
      value: `${args[1]} has not played any Ranked game.`,
    });
  }
  if (
    data.stats.queue.casual &&
    (data.stats.queue.casual.wins !== 0 || data.stats.queue.casual.losses !== 0)
  ) {
    stats.push(
      {
        name: 'Casual',
        value: `${args[1]} has played Casual games for **${(
          data.stats.queue.casual.playtime /
          60 /
          60
        ).toFixed(2)}** Hours.`,
      },
      {
        name: 'Wins',
        value: `${data.stats.queue.casual.wins}`,
        inline: true,
      },
      {
        name: 'Losses',
        value: `${data.stats.queue.casual.losses}`,
        inline: true,
      },
      {
        name: 'Kills',
        value: `${data.stats.queue.casual.kills}`,
        inline: true,
      },
      {
        name: 'Deaths',
        value: `${data.stats.queue.casual.deaths}`,
        inline: true,
      },
      {
        name: 'Win/Lose Ratio',
        value: `${data.stats.queue.casual.wl}`,
        inline: true,
      },
      {
        name: 'Kill/Death Ratio',
        value: `${data.stats.queue.casual.kd}`,
        inline: true,
      },
    );
  } else {
    stats.push({
      name: 'Casual',
      value: `${args[1]} has not played any Casual game.`,
    });
  }

  await message.channel.send({
    embed: {
      color: Bastion.colors.BLUE,
      title: 'Rainbow 6',
      url: `https://r6stats.com/stats/${data.uplay_id}`,
      fields: stats,
      thumbnail: {
        url:
          'https://vignette1.wikia.nocookie.net/rainbowsix/images/0/06/Rainbow_(Clear_Background)_logo.png',
      },
    },
  });
};

exports.config = {
  aliases: ['r6'],
  enabled: true,
};

exports.help = {
  name: 'rainbow6',
  description: 'Get stats of any Rainbow Six player.',
  botPermission: '',
  userTextPermission: '',
  userVoicePermission: '',
  usage: 'rainbow6 <pc|ps4|xone> <username>',
  example: ['rainbow6 pc SaffronPants'],
};
