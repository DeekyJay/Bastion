/**
 * @file cuddle command
 * @author Sankarsan Kampa (a.k.a k3rn31p4nic)
 * @license GPL-3.0
 */

const request = require('request-promise-native');

exports.exec = async (Bastion, message) => {
  let options = {
    url: 'http://api.giphy.com/v1/gifs/search',
    qs: {
      q: 'cuddle',
      api_key: 'mVoRdelEC0rJkVqAgQ5jhkc3p0NrBUTT',
      limit: 10,
      offset: 0,
    },
    json: true,
  };

  let response = await request(options);

  if (!response.data.length) {
    return Bastion.emit(
      'error',
      Bastion.strings.error(message.guild.language, 'notFound'),
      Bastion.strings.error(message.guild.language, 'notFound', true, 'image'),
      message.channel,
    );
  }

  await message.channel.send({
    embed: {
      color: Bastion.colors.BLUE,
      title: `${message.author.tag} is cuddling you.`,
      image: {
        url: response.data[Math.floor(Math.random() * response.data.length)].images.original.url,
      },
      footer: {
        text: 'Powered by GIPHY',
      },
    },
  });
};

exports.config = {
  aliases: [],
  enabled: true,
};

exports.help = {
  name: 'cuddle',
  description: 'Cuddle someone!',
  botPermission: '',
  userTextPermission: '',
  userVoicePermission: '',
  usage: 'cuddle',
  example: ['cuddle'],
};
