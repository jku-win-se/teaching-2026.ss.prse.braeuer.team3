// @ts-check
const eslint = require("@eslint/js");
const tseslint = require("typescript-eslint");
const angular = require("@angular-eslint/eslint-plugin");
const angularTemplate = require("@angular-eslint/eslint-plugin-template");
const angularTemplateParser = require("@angular-eslint/template-parser");

module.exports = tseslint.config(
  {
    files: ["**/*.ts"],
    extends: [
      eslint.configs.recommended,
      ...tseslint.configs.recommended,
    ],
    plugins: {
      "@angular-eslint": angular,
    },
    rules: {
      ...angular.configs.recommended.rules,
      "@typescript-eslint/no-unused-vars": ["error", { "argsIgnorePattern": "^_" }],
    },
    languageOptions: {
      parserOptions: {
        project: true,
        tsconfigRootDir: __dirname,
      },
    },
  },
  {
    files: ["**/*.html"],
    plugins: {
      "@angular-eslint/template": angularTemplate,
    },
    languageOptions: {
      parser: angularTemplateParser,
    },
    rules: {
      ...angularTemplate.configs.recommended.rules,
    },
  }
);
