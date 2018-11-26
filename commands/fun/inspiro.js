/**
 * @file inspiro command
 * @author Derek Jensen (Deek#0001)
 * @license MIT
 */

const request = require('request-promise-native');

exports.exec = async (Bastion, message) => {
  try {
    let options = {
      url: 'http://inspirobot.me/api?generate=true',
      json: true
    };

    let response = await request(options);

    if (response) {
      message.channel.send({
        embed: {
          color: Bastion.colors.BLUE,
          title: `${message.author.tag} wanted some inspiration`,
          image: {
            url: response
          }
        }
      }).catch(e => {
        Bastion.log.error(e);
      });
    }
    else {
      return Bastion.emit('error', Bastion.strings.error(message.guild.language, 'notFound'), Bastion.strings.error(message.guild.language, 'notFound', true, 'image'), message.channel);
    }
  }
  catch (e) {
    if (e.response) {
      return Bastion.emit('error', e.response.statusCode, e.response.statusMessage, message.channel);
    }
    Bastion.log.error(e);
  }
};

exports.config = {
  aliases: [],
  enabled: true
};

exports.help = {
  name: 'inspiro',
  description: 'Send an inspiration image from InspiroBot',
  botPermission: '',
  userTextPermission: '',
  userVoicePermission: '',
  usage: 'inspiro',
  example: [ 'inspiro' ]
};
