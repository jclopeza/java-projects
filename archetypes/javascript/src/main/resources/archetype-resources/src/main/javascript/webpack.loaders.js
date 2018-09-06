module.exports = [
  {
    test: /\.js$/,
    exclude: /node_modules/,
    loader: 'babel'
  },
  {
    test: /\.json$/,
    exclude: /node_modules/,
    loader: 'json'
  }
];
