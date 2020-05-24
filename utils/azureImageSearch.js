const azureImageSearch = async (request, Bastion, query, count, offset) => {
  let baseURL = 'https://api.cognitive.microsoft.com/bing/v7.0/images/search';

  const qOptions = [];
  qOptions.push('SafeSearch=strict');
  qOptions.push(`count=${count || 1}`);
  const randomOffset = Math.round(Math.random() * 200);
  qOptions.push(`offset=${offset || randomOffset}`);

  let options = {
    url: `${baseURL}/?q=${encodeURIComponent(query)}&${qOptions.join('&')}`,
    json: true,
    headers: {
      'Ocp-Apim-Subscription-Key': Bastion.credentials.azureCognitiveSearchAPIKey,
    },
  };
  let response = await request(options);
  return response;
};

const getRandomImageFromQuery = async (request, Bastion, query) => {
  const response = await azureImageSearch(request, Bastion, query, 1);
  return response.value[0].contentUrl;
};

module.exports = {
  azureImageSearch,
  getRandomImageFromQuery,
};
