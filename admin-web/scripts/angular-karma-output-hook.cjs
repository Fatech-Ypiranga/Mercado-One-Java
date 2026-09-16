const Module = require('node:module');

const originalCompile = Module.prototype._compile;
const expectedPath = [
  '@angular',
  'build',
  'src',
  'builders',
  'karma',
  'application_builder.js',
].join(require('node:path').sep);

Module.prototype._compile = function patchedAngularKarmaCompile(source, filename) {
  if (filename.endsWith(expectedPath)) {
    source = source.replace(
      "context.workspaceRoot, 'dist/test-out'",
      "context.workspaceRoot, '../output/admin-web-test-out'",
    );
  }

  return originalCompile.call(this, source, filename);
};
