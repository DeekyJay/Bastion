/**
 * @file tft command
 * @author Derek Jensen (Deek#0001)
 * @license MIT
 */

exports.exec = async (Bastion, message) => {
  try {
    var date = new Date('2019/09/01');
    var now = new Date();

    message.channel
      .send({
        embed: {
          color: Bastion.colors.BLUE,
          title: `Days Since We Lost The Boys to Team Fight Tactics`,
          description: now.getDate() - date.getDate(),
        },
      })
      .catch(e => {
        Bastion.log.error(e);
      });
  } catch (e) {
    if (e.response) {
      return Bastion.emit(
        'error',
        e.response.statusCode,
        e.response.statusMessage,
        message.channel,
      );
    }
    Bastion.log.error(e);
  }
};

exports.config = {
  aliases: [],
  enabled: true,
};

exports.help = {
  name: 'tft',
  description: 'See how many days the boys have been forever lost due to Team Fight Tactics',
  botPermission: '',
  userTextPermission: '',
  userVoicePermission: '',
  usage: 'tft',
  example: ['tft'],
};
