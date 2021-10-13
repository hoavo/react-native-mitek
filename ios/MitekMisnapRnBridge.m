#import "MitekMisnapRnBridge.h"

@implementation MitekMisnapRnBridge

RCT_EXPORT_MODULE(MitekMisnapRnBridge)

RCT_EXPORT_METHOD(setServerTypeAndVersion:(NSString *)type version:(NSString *)version)
{
    RCTLogInfo(@"Set server type %@ and version %@", type, version);
    
    self.serverType = type;
    self.serverVersion = version;
}

RCT_REMAP_METHOD(doMiSnapWorkflow,
                 docType:(NSString *)docType
                 resolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
    RCTLogInfo(@"Snap It...");
    self.selectedJobType = docType;
    self.resolveBlock = resolve;
    self.rejectBlock = reject;
    
    [self startSnapFlow];
}

- (void)startSnapFlow
{
    dispatch_async(dispatch_get_main_queue(), ^{
        self.miSnapController = [[MiSnapSDKViewController alloc] init];
        self.miSnapImage = [[UIImage alloc] init];
        
        // Get the MiSnapViewController selected by the UIControl
        self.miSnapController = [self getMiSnapViewControllerForSelectedUX];
        
        // Setup delegate, parameters, and transition style
        self.miSnapController.delegate = self;
        
        // Parameters will use auto capture
        [self.miSnapController setupMiSnapWithParams:[self getMiSnapParameters:YES]];
        
        self.miSnapController.modalTransitionStyle = UIModalTransitionStyleCrossDissolve;
        
        // For iOS 13, UIModalPresentationFullScreen is not the default, so be explicit
        self.miSnapController.modalPresentationStyle = UIModalPresentationFullScreen;
        
        self.miSnapController.useBarcodeScannerLight = TRUE;
        
        UIViewController *rootViewController = [UIApplication sharedApplication].keyWindow.rootViewController;
        
        if ([self.selectedJobType isEqualToString:@"PDF417"])
        {
            MiSnapBarcodeScannerViewController *viewController = [MiSnapBarcodeScannerViewController instantiateFromStoryboard];
            viewController.delegate = self;
            viewController.modalTransitionStyle = UIModalTransitionStyleCrossDissolve;
            viewController.modalPresentationStyle = UIModalPresentationFullScreen;
            [rootViewController presentViewController:viewController animated:YES completion:nil];
        }
        else
        {
            [rootViewController presentViewController:self.miSnapController animated:YES completion:nil];
        }
    });
}

- (MiSnapSDKViewController *)getMiSnapViewControllerForSelectedUX {
    
    return (MiSnapSDKViewController *)[[UIStoryboard storyboardWithName:@"MiSnapUX2" bundle:nil] instantiateViewControllerWithIdentifier:@"MiSnapSDKViewControllerUX2"];
}

- (void)afterDismissMiSnap
{
    if ([self.selectedJobType isEqualToString:kMiSnapDocumentTypeDriverLicense] && ([self.encodedImage length] > 0))
    {
        NSDictionary *resultDict = @{@"image": self.encodedImage,
                                     @"data": self.miSnapMibiData[@"MiSnapMIBIData"]};
        self.resolveBlock(resultDict);
    }
    // Reset
    self.miSnapImage = [[UIImage alloc] init];
    self.miSnapController = nil;
}

- (void)miSnapCancelledWithResults:(NSDictionary *)results forDocumentType:(NSString *)docType
{
    self.miSnapImage = [[UIImage alloc] init];
    
    if ([docType isEqualToString:kMiSnapDocumentTypeDriverLicense])
    {
        RCTLogInfo(@"Handle Driver License cancel event");
        self.rejectBlock(@"cancel", @"Capture Driver License cancelled!", [self cancelError]);
    }
    else if ([docType isEqualToString:kMiSnapDocumentTypeIdCardFront])
    {
        RCTLogInfo(@"Handle Id Card Front cancel event");
        self.rejectBlock(@"cancel", @"Capture Id Card Front cancelled!", [self cancelError]);
    }
    else if ([docType isEqualToString:kMiSnapDocumentTypeIdCardBack])
    {
        RCTLogInfo(@"Handle Id Card Back cancel event");
        self.rejectBlock(@"cancel", @"Capture Id Card Back cancelled!", [self cancelError]);
    }
    else if ([docType isEqualToString:kMiSnapDocumentTypePDF417])
    {
        RCTLogInfo(@"Handle PDF417 cancel event");
        self.rejectBlock(@"cancel", @"Capture PDF417 cancelled!", [self cancelError]);
    }
}

// Called when the cancel button is pressed during capture
- (void)miSnapCancelledWithResults:(NSDictionary *)results
{
    self.miSnapImage = [[UIImage alloc] init];
    self.rejectBlock(@"cancel", @"Capture cancelled!", [self cancelError]);
}

#pragma mark - MiSnapViewControllerDelegate, MiSnapBarcodeScannerDelegate

// Called when an image has been captured in either automatic or manual mode
- (void)miSnapFinishedReturningEncodedImage:(NSString *)encodedImage
                              originalImage:(UIImage *)originalImage
                                 andResults:(NSDictionary *)results
                            forDocumentType:(NSString *)docType
{
    if ([docType isEqualToString:kMiSnapDocumentTypeDriverLicense])
    {
        RCTLogInfo(@"Handle Driver License results");
    }
    else if ([docType isEqualToString:kMiSnapDocumentTypeIdCardFront])
    {
        RCTLogInfo(@"Handle Id Card Front results");
    }
    else if ([docType isEqualToString:kMiSnapDocumentTypeIdCardBack])
    {
        RCTLogInfo(@"Handle Id Card Back results");
    }
    else if ([docType isEqualToString:kMiSnapDocumentTypePDF417])
    {
        RCTLogInfo(@"Handle PDF417 results");
    }
    // Handle results
    [self processResults:results encodedImage:encodedImage originalImage:originalImage];
}

// Called when an image has been captured in either automatic or manual mode
- (void)miSnapFinishedReturningEncodedImage:(NSString *)encodedImage
                              originalImage:(UIImage *)originalImage
                                 andResults:(NSDictionary *)results
{
    // Handle results
    [self processResults:results encodedImage:encodedImage originalImage:originalImage];
}

#pragma mark - Handle snap results

- (void)processResults:(NSDictionary *)results encodedImage:(NSString *)encodedImage originalImage:(UIImage *)image
{
    // Save the encoded image
    self.encodedImage = encodedImage;
    
    NSString* resultCode = results[kMiSnapResultCode];
    
    self.miSnapMibiData = results;
    
    if ([resultCode isEqualToString:kMiSnapBarcodeScannerResultSuccessPDF417])
    {
        NSString* pdf417Data = results[kMiSnapPDF417Data];
        NSString* miSnapBarcodeScannerMIBIData = results[@"MiSnapBarcodeScannerMIBIData"];
        
        //    NSString* message = [self processPDF417:results resultCode:resultCode];
        //    NSDictionary *resultDict = @{@"pdf417results": message};
        
        [self showAlert];
        
        NSDictionary *resultDict = @{@"miSnapBarcodeScannerMIBIData": miSnapBarcodeScannerMIBIData,
                                     @"miSnapPDF417Data": pdf417Data};
        
        self.resolveBlock(resultDict);
    }
    else
    {
        self.miSnapImage = [image copy];
    }
    
    [self afterDismissMiSnap];
}

// Setting MiSnap parameters for each specific document type
- (NSDictionary *)getMiSnapParameters:(BOOL)useAutoCapture
{
    NSMutableDictionary* parameters = [NSMutableDictionary
                                       dictionaryWithDictionary:[MiSnapSDKViewController defaultParametersForACH]];
    
    // Must set specific server type and server version
    [parameters setObject:self.serverType forKey:kMiSnapServerType];
    [parameters setObject:self.serverVersion forKey:kMiSnapServerVersion];
    
    // LanguageOverride forces only English "en". Uncomment this to enforce just English localization.
    // [parameters setObject:@"en" forKey:@"LanguageOverride"];
    
    if([self.selectedJobType isEqualToString:@"CheckFront"])
    {
        parameters = [NSMutableDictionary
                      dictionaryWithDictionary:[MiSnapSDKViewController defaultParametersForCheckFront]];
        [parameters setObject:@"Mobile Deposit Check Front" forKey:kMiSnapShortDescription];
        [parameters setObject:@"50" forKey:kMiSnapImageQuality];
    }
    else if([self.selectedJobType isEqualToString:@"CheckBack"])
    {
        parameters = [NSMutableDictionary
                      dictionaryWithDictionary:[MiSnapSDKViewController defaultParametersForCheckBack]];
        [parameters setObject:@"Mobile Deposit Check Back" forKey:kMiSnapShortDescription];
        [parameters setObject:@"50" forKey:kMiSnapImageQuality];
    }
    else if([self.selectedJobType isEqualToString:kMiSnapDocumentTypePassport]) //@"PASSPORT"
    {
        parameters = [NSMutableDictionary
                      dictionaryWithDictionary:[MiSnapSDKViewController defaultParametersForPassport]];
        [parameters setObject:@"Passport" forKey:kMiSnapShortDescription];
        //        [parameters setObject:@"1" forKey:kMiSnapSeamlessFailover];
        //        [parameters setObject:@"0" forKey:kMiSnapTorchMode];
    }
    else if([self.selectedJobType isEqualToString:kMiSnapDocumentTypeDriverLicense]) //@"DRIVER_LICENSE"
    {
        parameters = [NSMutableDictionary
                      dictionaryWithDictionary:[MiSnapSDKViewController defaultParametersForDriversLicense]];
        [parameters setObject:@"530" forKey:kMiSnapMinLandscapeHorizontalFill];
        [parameters setObject:@"License Front" forKey:kMiSnapShortDescription];
        [parameters setObject:@"0" forKey:kMiSnapTorchMode];
    }
    else if([self.selectedJobType isEqualToString:kMiSnapDocumentTypeIdCardFront]) //@"ID_CARD_FRONT"
    {
        parameters = [NSMutableDictionary
                      dictionaryWithDictionary:[MiSnapSDKViewController defaultParametersForIdCardFront]];
        [parameters setObject:@"ID Card Front" forKey:kMiSnapShortDescription];
        [parameters setObject:@"0" forKey:kMiSnapTorchMode];
    }
    else if([self.selectedJobType isEqualToString:kMiSnapDocumentTypeIdCardBack]) //@"ID_CARD_FRONT"
    {
        parameters = [NSMutableDictionary
                      dictionaryWithDictionary:[MiSnapSDKViewController defaultParametersForIdCardBack]];
        [parameters setObject:@"1" forKey:kMiSnapMaxCaptures]; // Shows how to set explicitly
        [parameters setObject:@"ID Card Back" forKey:kMiSnapShortDescription];
        [parameters setObject:@"0" forKey:kMiSnapTorchMode];
    }
    else if([self.selectedJobType isEqualToString:@"PDF417"])
    {
        [parameters setObject:kMiSnapDocumentTypePDF417 forKey:kMiSnapDocumentType];
        [parameters setObject:@"PDF417 Barcode Scanner" forKey:kMiSnapShortDescription];
    }
    else if([self.selectedJobType isEqualToString:@"PDF417 Light"])
    {
        [parameters setObject:kMiSnapDocumentTypePDF417 forKey:kMiSnapDocumentType];
        [parameters setObject:@"PDF417 Barcode Scanner Light" forKey:kMiSnapShortDescription];
    }
    else if([self.selectedJobType isEqualToString:@"LandscapeDoc"])
    {
        parameters = [NSMutableDictionary
                      dictionaryWithDictionary:[MiSnapSDKViewController defaultParametersForLandscapeDocument]];
        [parameters setObject:@"Landscape Document" forKey:kMiSnapShortDescription];
    }
    
    if(useAutoCapture == NO)
    {
        [parameters setObject:@"1" forKey:kMiSnapCaptureMode];
    }
    
    return [parameters copy];
}

#pragma mark - Utils

- (void)showAlert
{
    UIAlertController * alert = [UIAlertController
                                 alertControllerWithTitle:@"Success!"
                                 message:@"Driver's License Back scanned successfully!"
                                 preferredStyle:UIAlertControllerStyleAlert];
    UIAlertAction* okButton = [UIAlertAction
                               actionWithTitle:@"OK"
                               style:UIAlertActionStyleDefault
                               handler:^(UIAlertAction * action) {}];
    [alert addAction:okButton];
    UIViewController *rootViewController = [UIApplication sharedApplication].keyWindow.rootViewController;
    [rootViewController presentViewController:alert animated:YES completion:nil];
}

// Util to get an error object
- (NSError *)cancelError
{
    NSError *error = [NSError errorWithDomain: [[NSBundle mainBundle] bundleIdentifier]
                                         code: NSUserCancelledError
                                     userInfo: @{@"Error reason": @"User Cancelled"}];
    return error;
}
@end
