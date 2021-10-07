import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-mitek' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo managed workflow\n';

const Mitek = NativeModules.Mitek
  ? NativeModules.Mitek
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export function setDocumentType(type: string) {
  Mitek.setDocumentType(type);
}
export function setServerTypeAndVersion(type: string, value: string) {
  Mitek.setServerTypeAndVersion(type, value);
}

export function doCaptureFace(): Promise<number> {
  return Mitek.doCaptureFace();
}

export function doSnap(): Promise<number> {
  return Mitek.doSnap();
}
