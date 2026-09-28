module.exports = function configureKarma(config) {
  const collectCoverage = process.env.MERCADO_ONE_COVERAGE === '1';
  config.set({
    frameworks: ['jasmine'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
    ],
    reporters: collectCoverage ? ['progress', 'coverage'] : ['progress', 'kjhtml'],
    coverageReporter: {
      dir: '../output/admin-web-coverage',
      reporters: [{ type: 'text-summary' }, { type: 'lcovonly' }],
      check: { global: { statements: 80, lines: 80 } },
    },
    jasmineHtmlReporter: {
      suppressAll: true,
    },
    browsers: ['ChromeHeadlessNoGpu'],
    customLaunchers: {
      ChromeHeadlessNoGpu: {
        base: 'ChromeHeadless',
        flags: [
          '--disable-gpu',
          '--disable-gpu-compositing',
          '--disable-dev-shm-usage',
          '--no-sandbox',
        ],
      },
    },
  });
};
