require 'json'
version = JSON.parse(File.read('package.json'))["version"]

Pod::Spec.new do |s|
  s.name             = 'react-native-keyboard-view'
  s.version          = version
  s.summary          = 'react-native-keyboard-view'
  s.homepage         = 'https://github.com/shimohq/react-native-keyboard-view'
  s.license          = { :type => 'MIT', :file => 'LICENSE' }
  s.author           = { 'zhongjiaren' => 'zhongjiaren@shimo.im' }
  s.source           = { :git => 'https://github.com/shimohq/react-native-keyboard-view.git', :tag => "v#{s.version} }

  s.ios.deployment_target = '8.0'
  
  s.source_files = 'ios/**/*.{h,m,mm}'
  
  s.dependency 'React'

end
