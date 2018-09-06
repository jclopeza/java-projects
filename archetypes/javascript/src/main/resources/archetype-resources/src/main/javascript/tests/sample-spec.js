import {VERSION} from '../src/index';

describe('Test case', function() {
  it('should check version number', function() {
    VERSION.should.exist;
  });

  it('should pass', function(done) {
    setTimeout(done, 1000);
  });
});
