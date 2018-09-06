import chai from 'chai';

const context = require.context(
  /*directory*/    'mocha!../tests',
  /*recursive*/    true,
  /*match files*/  /-spec.js$/
);

chai.should();
context.keys().forEach(context);
