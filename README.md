# mitek-misnap-rn-bridge

mitek-misnap-rn-bridge

## Demo Examples
https://user-images.githubusercontent.com/2727930/136515785-ff1ea3ad-0eb1-42c5-a5e8-c559d0abbb87.MP4

https://user-images.githubusercontent.com/2727930/136515625-93f00717-db20-4c2b-a04d-b2e884b2d1e3.mp4

Run Demo Example:

```sh
npm install

npm run example android  

npm run example ios
```

## Installation



```sh
npm install mitek-misnap-rn-bridge
```


## Config

### iOS
Add this values to info.plist before
```
  <key>NSCameraUsageDescription</key>
  <string>Camera access is required to take a picture of your document.</string>
  <key>NSMicrophoneUsageDescription</key>
  <string>MiSnap needs access to Microphone to record audio</string>
  <key>NSPhotoLibraryAddUsageDescription</key>
  <string>MiSnap needs access to PhotoLibrary to save recorded videos and images</string>
  <key>NSPhotoLibraryUsageDescription</key>
  <string>MiSnap needs access to PhotoLibrary to save recorded videos and images</string>
```

### Android

update `android/app/build.gradle`

```
android {
   
    defaultConfig {
        ...
        
        // Enabling multidex support.
        multiDexEnabled true
    }
    ...
}

dependencies {
  implementation 'androidx.multidex:multidex:2.0.1'  //with androidx libraries
  //implementation 'com.android.support:multidex:1.0.3'  //with support libraries
  
}
```
## Usage



```js
import { startMiSnapWorkflow, MISNAPTYPE, setServerTypeAndVersion } from 'mitek-misnap-rn-bridge';

// ...

setServerTypeAndVersion('test', '0.0');
const snapResults = await startMiSnapWorkflow(MISNAPTYPE.DRIVER_LICENSE);
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT
