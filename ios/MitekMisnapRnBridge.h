#import <React/RCTBridgeModule.h>
#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTViewManager.h>
#import "MiSnapSDKViewControllerUX2.h"
#import "MiSnapBarcodeScanner/MiSnapBarcodeScanner.h"
#import "MiSnapBarcodeScannerViewController.h"
#import "MiSnapBarcodeScannerLightViewController.h"

@interface MitekMisnapRnBridge : RCTViewManager <MiSnapViewControllerDelegate, MiSnapBarcodeScannerDelegate,
MiSnapBarcodeScannerLightDelegate>

@property (nonatomic, strong) MiSnapSDKViewController* miSnapController;
@property (nonatomic) NSString* selectedJobType;
@property (nonatomic) NSString* serverType;
@property (nonatomic) NSString* serverVersion;
@property (nonatomic) NSString* encodedImage;
@property (nonatomic, strong) UIImage* miSnapImage;
@property (nonatomic) NSDictionary* miSnapMibiData;
@property (nonatomic) RCTPromiseResolveBlock resolveBlock;
@property (nonatomic) RCTPromiseRejectBlock rejectBlock;

@end
