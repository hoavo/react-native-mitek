require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "mitek-misnap-rn-bridge"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => "10.0" }
  s.source       = { :git => "https://github.com/Sherwood-forest/mitek-misnap-rn-bridge.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,m,mm}"
  s.vendored_frameworks = 'ios/MiSnapSDK/MiSnapSDK.xcframework', 'ios/MiSnapBarcodeScanner/MiSnapBarcodeScanner.xcframework', 'ios/MiSnapLivenessSDK/MiSnapLiveness.xcframework', 'ios/MiSnapSDK/MiSnapSDKScience.xcframework', 'ios/MiSnapSDK/MobileFlow.xcframework', 'ios/MiSnapSDK/MiSnapSDKMibiData.xcframework', 'ios/MiSnapSDK/MiSnapSDKCamera.xcframework', 'ios/MiSnapBarcodeScannerLight/MiSnapBarcodeScannerLight.xcframework'
  s.resources = "ios/MiSnapSDK/MiSnapUX/**/*.{png,jpeg,jpg,storyboard,xib,xcassets,lproj}", "ios/MiSnapBarcodeScanner/MiSnapBarcodeScannerUX/**/*.{png,jpeg,jpg,storyboard,xib,xcassets,lproj}", "ios/MiSnapBarcodeScannerLight/MiSnapBarcodeScannerLightUX/**/*.{png,jpeg,jpg,storyboard,xib,xcassets,lproj}"  

  s.dependency "React-Core"
end
