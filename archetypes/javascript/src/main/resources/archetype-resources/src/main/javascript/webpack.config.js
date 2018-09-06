const
  plugins = require('./webpack.plugins'),
  loaders = require('./webpack.loaders'),
  Case    = require('case'),
  pkg     = require('./package.json')
;

module.exports = {
  entry: (function IIFE() {
    const entry = {};

    entry[pkg.name] = ['./src/index.js'];
    entry.spec      = './tests/index.js';

    return entry
  }()),
  output: {
    path:          './dist',
    filename:      '[name].js',
    library:       Case.pascal(pkg.name),
    libraryTarget: 'umd'
  },
  devtool: 'source-map',
  module: {loaders},
  plugins
};
