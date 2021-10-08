import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'mitek-misnap-rn-bridge' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo managed workflow\n';

const MitekMisnapRnBridge = NativeModules.MitekMisnapRnBridge
  ? NativeModules.MitekMisnapRnBridge
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export enum MISNAPTYPE {
  CHECK_FRONT = "CheckFront",
  CHECK_BACK = "CheckBack",
  DRIVER_LICENSE = "DRIVER_LICENSE",
  ID_CARD_FRONT = "ID_CARD_FRONT",
  ID_CARD_BACK = "ID_CARD_BACK",
  BUSINESS_CARD = "BUSINESS_CARD",
  AUTO_INSURANCE = "AUTO_INSURANCE",
  PASSPORT = "PASSPORT",
  CREDIT_CARD = "CREDIT_CARD",
  PDF417 = "PDF417",
  BARCODES = "BARCODES"
}

export function startMiSnapWorkflow(type: string): Promise<number> {
  return MitekMisnapRnBridge.doMiSnapWorkflow(type);
}