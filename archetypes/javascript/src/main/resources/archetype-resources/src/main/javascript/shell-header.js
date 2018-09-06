const
  chalk  = require('chalk'),
  name   = chalk.blue.bold  ('    vocento ${package-name}'),
  author = chalk.cyan.italic('    ivanperez@vocento.com')
;

let output = chalk.yellow(`
    ████████████████████████
    ████████████████████████
    ████████████████████████
    ████████████████████████
    ████████████████████████
    ██████████  ████    ████
    ██████████  ██  ████████
    ██████████  ████    ████
    ██████████  ████████  ██
    ██████████  ██  ████  ██
    ██████    ██████    ████
    ████████████████████████
`);

module.exports = `${output}\n${name}\n${author}\n`;
