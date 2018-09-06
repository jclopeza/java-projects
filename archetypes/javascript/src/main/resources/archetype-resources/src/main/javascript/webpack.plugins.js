const
  webpack            = require('webpack'),
  WebpackShellPlugin = require('webpack-shell-plugin'),
  HtmlWebpackPlugin  = require('html-webpack-plugin'),
  header             = require('./shell-header.js')
;

module.exports = [
  new webpack.optimize.UglifyJsPlugin({
    compress: {warnings: false, drop_console: true},
    output:   {comments: false}
  }),
  new HtmlWebpackPlugin({
    filename: 'index.html',
    inject: 'body',
    excludeChunks: ['spec']
  }),
  new HtmlWebpackPlugin({
    filename: 'spec/index.html',
    inject: 'body',
    excludeChunks: ['emptylib']
  }),
  new WebpackShellPlugin({
    onBuildStart: [
      `echo "${header}"`,
      'echo "Building ..."'
    ],
    onBuildEnd: [
      'echo "\nDone!\n"'
    ]
  })
];
