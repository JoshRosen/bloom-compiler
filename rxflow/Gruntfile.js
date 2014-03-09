'use strict';

module.exports = function(grunt) {

  // Project configuration.
  grunt.initConfig({
    vows: {
      files: ['test/**/*_test.js'],
    },
    jshint: {
      options: {
        jshintrc: '.jshintrc'
      },
      gruntfile: {
        src: 'Gruntfile.js'
      },
      test: {
        src: ['test/**/*.js']
      },
    },
    tslint: {
      options: {
        configuration: grunt.file.readJSON(".tslintrc")
      },
      files: {
        src: ['src/**/*.ts']
      }
    },
    jsdoc: {
      dist: {
        src: ['src/**/*.js'],
        options: {
          destination: 'doc'
        }
      }
    },
    watch: {
      gruntfile: {
        files: '<%= jshint.gruntfile.src %>',
        tasks: ['jshint:gruntfile']
      },
      ts: {
        files: '<%= ts.all.src %>',
        tasks: ['tslint', 'ts:all', 'vows']
      },
      test: {
        files: '<%= jshint.test.src %>',
        tasks: ['jshint:test', 'vows']
      },
    },
    benchmark: {
      options: {
        displayResults: true
      },
      all: {
        src: ['benchmark/*.js'],
        dest: 'benchmark/results.csv'
      },
    },
    ts: {
      all: {
        src: ['src/**/*.ts'],
        options: {
          module: 'commonjs',
          removeComments: false
        }
      }
    },
    tsd: {
      all: {
        options: {
          command: 'reinstall',
          latest: false,
          config: './tsd.json'
        }
      }
    },
  });

  // These plugins provide necessary tasks.
  grunt.loadNpmTasks('grunt-benchmark');
  grunt.loadNpmTasks('grunt-contrib-jshint');
  grunt.loadNpmTasks('grunt-contrib-watch');
  grunt.loadNpmTasks("grunt-vows");
  grunt.loadNpmTasks("grunt-jsdoc");
  grunt.loadNpmTasks("grunt-ts");
  grunt.loadNpmTasks('grunt-tsd');
  grunt.loadNpmTasks('grunt-tslint');


  // Default task.
  grunt.registerTask('default', ['ts', 'tslint', 'jshint', 'vows']);

};
