/**
 * @file bird command
 * @author Derek Jensen (DeekyJay)
 * @license GPL-3.0
 */

const request = xrequire('request-promise-native');

const { getRandomImageFromQuery } = require('../../utils/azureImageSearch');

exports.exec = async (Bastion, message) => {
  const contentUrl = await getRandomImageFromQuery(request, Bastion, 'bird');

  await message.channel.send({
    files: [contentUrl],
  });
};

exports.config = {
  aliases: [],
  enabled: true,
};

exports.help = {
  name: 'bird',
  description: 'Shows a random picture of a bird.',
  botPermission: '',
  userTextPermission: '',
  userVoicePermission: '',
  usage: 'bird',
  example: [],
};
