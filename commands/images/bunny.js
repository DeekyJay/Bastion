/**
 * @file bunny command
 * @author Derek Jensen (DeekyJay)
 * @license GPL-3.0
 */

const request = xrequire('request-promise-native');

exports.exec = async (Bastion, message) => {
  let baseURL = 'https://api.cognitive.microsoft.com/bing/v7.0/images/search';

  const qOptions = [];
  qOptions.push('SafeSearch=strict');
  qOptions.push('count=1');
  const randomOffset = Math.round(Math.random() * 200);
  qOptions.push('offset=' + randomOffset);

  let options = {
    url: `${baseURL}/?q=bunny+rabbit&${qOptions.join('&')}`,
    json: true,
    headers: {
      'Ocp-Apim-Subscription-Key': Bastion.credentials.azureCognitiveSearchAPIKey,
    },
  };
  let response = await request(options);

  await message.channel.send({
    files: [response.value[0].contentUrl],
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
