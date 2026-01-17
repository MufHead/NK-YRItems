function main(arg1, arg2) {
  // 返回一个字符串，演示JS节点调用
  print('ExampleScript.main called with', arg1, arg2);
  return '脚本返回-' + (arg1 || '') + '-' + (arg2 || '');
}