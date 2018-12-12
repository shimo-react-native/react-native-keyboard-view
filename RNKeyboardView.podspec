require 'json'
version = JSON.parse(File.read('package.json'))["version"]

Pod::Spec.new do |s|
  s.name             = 'RNKeyboardView'
  s.version          = version
  s.summary          = 'react-native-keyboard-view'

  s.description      = <<-DESC
TODO: Add long description of the pod here.
                       DESC

  s.homepage         = 'https://github.com/shimohq/react-native-keyboard-view'
  s.license          = { :type => 'MIT', :file => 'LICENSE' }
  s.author           = { 'zhongjiaren' => 'zhongjiaren@shimo.im' }
  s.source           = { :git => 'https://github.com/shimohq/react-native-keyboard-view.git', :tag => s.version.to_s }

  s.ios.deployment_target = '8.0'
  
  s.source_files = 'ios/**/*.{h,m,mm}'
  
  s.dependency 'React'

end