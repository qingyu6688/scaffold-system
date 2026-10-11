import vue from 'eslint-plugin-vue'
import tsParser from '@typescript-eslint/parser'

export default [
  { ignores: ['dist/**', 'node_modules/**', 'src/**/__tests__/**'] },
  ...vue.configs['flat/essential'],
  {
    files: ['src/**/*.{ts,vue}'],
    languageOptions: { parserOptions: { parser: tsParser, extraFileExtensions: ['.vue'] } },
    rules: {
      'no-debugger': 'error',
      'no-empty': 'error',
      'no-constant-condition': ['error', { checkLoops: false }],
      'vue/multi-word-component-names': 'off',
      // 列表组件接收共享状态对象，允许更新其中的引用，禁止替换整个 prop。
      'vue/no-mutating-props': ['error', { shallowOnly: true }],
    },
  },
  { files: ['src/**/*.ts'], languageOptions: { parser: tsParser } },
]
