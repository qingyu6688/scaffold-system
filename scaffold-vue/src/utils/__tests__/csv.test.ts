import test from 'node:test'
import assert from 'node:assert/strict'
import { toCsv } from '../csv.ts'

test('toCsv serializes headers, rows and escaped cell values', () => {
  const csv = toCsv(
    [
      { title: '名称', key: 'name' },
      { title: '备注', key: 'remark' },
      { title: '状态', value: row => row.status === 1 ? '正常' : '停用' },
      { title: '空值', key: 'missing' },
    ],
    [
      { name: '研发,平台', remark: '包含"引号"', status: 1 },
      { name: '测试质量部', remark: '跨行\n说明', status: 0 },
    ]
  )

  assert.equal(csv, [
    '名称,备注,状态,空值',
    '"研发,平台","包含""引号""",正常,',
    '测试质量部,"跨行\n说明",停用,',
  ].join('\n'))
})


test('导出时将公式前缀视为文本，保留引号和换行转义', () => {
  const values = ['=1+1', '+SUM(1)', '-1+2', '@SUM(1)', '  =1', '\t=1']
  const csv = toCsv([{ title: '内容', key: 'text' }], values.map(text => ({ text })))
  assert.equal(csv, ['内容', ...values.map(value => "'" + value)].join('\n'))
})
