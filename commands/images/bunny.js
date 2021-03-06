/**
 * @file bunny command
 * @author Derek Jensen (DeekyJay)
 * @license GPL-3.0
 */

const request = xrequire('request-promise-native');

const { getRandomImageFromQuery } = require('../../utils/azureImageSearch');

exports.exec = async (Bastion, message) => {
  const contentUrl = await getRandomImageFromQuery(request, Bastion, 'bunny rabbit');

  await message.channel.send({
    files: [contentUrl],
  });
};

exports.config = {
  aliases: [],
  enabled: true,
};

exports.help = {
  name: 'bunny',
  description: 'Shows a random picture of a bunny.',
  botPermission: '',
  userTextPermission: '',
  userVoicePermission: '',
  usage: 'bunny',
  example: [],
};
