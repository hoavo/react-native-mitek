//
//  MiSnapBarcodeScannerViewController.m
//  MiSnapBarcodeScanner
//
//  Created by Runi Kovoor on 2/4/15.
//  Copyright (c) 2015 Runi Kovoor. All rights reserved.
//

#import <AudioToolbox/AudioToolbox.h>
#import "MiSnapBarcodeScannerViewController.h"
#import <sys/utsname.h>

@interface MiSnapBarcodeScannerViewController () <MiSnapBarcodeScannerCameraDelegate, MiSnapBarcodeScannerAnalyzerDelegate, MiSnapBarcodeScannerTutorialViewControllerDelegate>

@property (nonatomic, weak) IBOutlet MiSnapBarcodeScannerCamera *camera;
@property (nonatomic, weak) IBOutlet MiSnapBarcodeScannerOverlayView *overlayView;

@property (nonatomic, strong) MiSnapBarcodeScannerTutorialViewController *tutorialViewController;

@property (nonatomic) MiSnapBarcodeScannerAnalyzer *analyzer;

@property (nonatomic) UIInterfaceOrientation statusbarOrientation;

@property (nonatomic) CGFloat lineWidth;
@property (nonatomic) UIColor *lineColor;
@property (nonatomic) CGFloat lineAlpha;
@property (nonatomic) CGFloat vignetteAlpha;

@property (nonatomic) CGFloat cornerRadius;
@property (nonatomic) CGFloat landscapeFill;
@property (nonatomic) CGFloat portraitFill;
@property (nonatomic) CGFloat guideAspectRatio;

@property (nonatomic) BOOL firstTimeTutorialHasBeenShown;
@property (nonatomic) BOOL tutorialCancelled;

@property (nonatomic, assign) BOOL shouldSkipFrames;

@property (nonatomic) NSString *decodeResultString;

@end

@implementation MiSnapBarcodeScannerViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    if (self.encodedImageQuality == 0)
    {
        self.encodedImageQuality = 0.6;
    }
    
    self.analyzer = [[MiSnapBarcodeScannerAnalyzer alloc] init];
    self.analyzer.delegate = self;
    self.shouldSkipFrames = NO; // Default is to analyze without skipping

    if (self.navigationController)
    {
        self.navigationController.navigationBarHidden = YES;
    }
}

- (void)viewWillAppear:(BOOL)animated
{
    [super viewWillAppear:animated];
    
    self.statusbarOrientation = [UIApplication sharedApplication].statusBarOrientation;
    [self.overlayView hideAllUIElements];
    
    [UIApplication sharedApplication].idleTimerDisabled = YES;
}

- (void)viewDidAppear:(BOOL)animated
{
    if (self.tutorialCancelled) { return; }
    
    [super viewDidAppear:animated];
    
    if ([[NSUserDefaults standardUserDefaults] objectForKey:kMiSnapBarcodeScannerShowTutorial] == nil)
    {
        [[NSUserDefaults standardUserDefaults] setBool:YES forKey:kMiSnapBarcodeScannerShowTutorial];
    }
    
    if ([[NSUserDefaults standardUserDefaults] boolForKey:kMiSnapBarcodeScannerShowTutorial] && !self.firstTimeTutorialHasBeenShown)
    {
        self.firstTimeTutorialHasBeenShown = YES;
        [self showFirstTimeTutorial];
    }
    else
    {
        self.camera.cameraOrientation = UIInterfaceOrientationLandscapeRight;
        [self.camera setSessionPreset:AVCaptureSessionPreset1920x1080 pixelBufferFormat:kCVPixelFormatType_420YpCbCr8BiPlanarFullRange];
        self.camera.delegate = self;
        
        [self.overlayView initWithGuideMode:self.guideMode];
        [self.overlayView setLineWidth:self.lineWidth lineColor:self.lineColor lineAlpha:self.lineAlpha vignetteAlpha:self.vignetteAlpha];
        [self.overlayView setGuideLandscapeFill:self.landscapeFill portraitFill:self.portraitFill aspectRatio:self.guideAspectRatio cornerRadius:self.cornerRadius];
        [self.overlayView updateWithOrientation:self.statusbarOrientation];
    }
}

- (void)viewWillTransitionToSize:(CGSize)size withTransitionCoordinator:(id<UIViewControllerTransitionCoordinator>)coordinator
{
    [super viewWillTransitionToSize:size withTransitionCoordinator:coordinator];
    
    __block UIInterfaceOrientation toInterfaceOrientation = UIInterfaceOrientationUnknown;
    
    [coordinator animateAlongsideTransition:^(id<UIViewControllerTransitionCoordinatorContext> _Nonnull context)
     {
         toInterfaceOrientation = [UIApplication sharedApplication].statusBarOrientation;
         
         [self.camera updatePreviewLayer:toInterfaceOrientation];
         
         self.statusbarOrientation = toInterfaceOrientation;
         
         if (self.guideMode != MiSnapBarcodeGuideModeNoGuide)
         {
             [self.overlayView updateWithOrientation:self.statusbarOrientation];
         }
    }
    completion:nil];
}

- (void)didFinishConfiguringSession
{
    if ([self.delegate respondsToSelector:@selector(miSnapBarcodeDidStartSession:)])
    {
        [self.delegate miSnapBarcodeDidStartSession:self];
    }
    else
    {
        self.shouldSkipFrames = NO;
    }

    dispatch_async(dispatch_get_main_queue(), ^{
        [self.camera updatePreviewLayer:self.statusbarOrientation];
        [self.camera start];
        
        [self.overlayView showTorch:self.camera.hasTorch];
        [self.overlayView torchEnabled:self.camera.isTorchOn];
    });
}

- (void)resumeAnalysis
{
    self.shouldSkipFrames = NO;
    //NSLog(@"$$$$$$ startCapture");
}

- (void)pauseAnalysis
{
    self.shouldSkipFrames = YES;
    //NSLog(@"$$$$$$ pauseCapture");
}

- (void)didReceiveSampleBuffer:(CMSampleBufferRef)sampleBuffer
{
    if (self.shouldSkipFrames)
    {
        //NSLog(@"%%%%%% shouldSkipFrames %d", self.shouldSkipFrames);
        return;
    }
    [self.analyzer analyzeSampleBuffer:sampleBuffer];
}

- (void)analyzerDecodeResultString:(NSString *)decodeResultString image:(UIImage *)image
{
    if (![self.decodeResultString isEqualToString:decodeResultString])
    {
        self.decodeResultString = decodeResultString;
        
        NSString* mibiDataString = [self buildBarcodeMIBIDataWithResult:kMiSnapBarcodeScannerResultSuccessPDF417];

        if (mibiDataString == nil) {
            mibiDataString = @"NO MIBIDATA";
        }
        NSString *nonNilString;
        if (self.decodeResultString == nil)
        {
            nonNilString = [NSString new];
        }
        else
        {
            nonNilString = self.decodeResultString;
        }

        NSDictionary* returnResults = @{kMiSnapBarcodeScannerResultCode:kMiSnapBarcodeScannerResultSuccessPDF417,
                                        kMiSnapBarcodeScannerPDF417Data:nonNilString,
                                        kMiSnapBarcodeScannerMIBIData:mibiDataString
                                        };

        NSString *encodedImage = nil;
        if (image != nil)
        {
            NSData *imageData = UIImageJPEGRepresentation(image, self.encodedImageQuality);
            encodedImage = [imageData base64EncodedStringWithOptions:0];
        }

        [self shutdownAnimated:YES];

        [self playShutterSoundAndVibrate:YES];

        if ([self.delegate respondsToSelector:@selector(miSnapFinishedReturningEncodedImage:originalImage:andResults:forDocumentType:)])
        {
            [self.delegate miSnapFinishedReturningEncodedImage:encodedImage originalImage:image andResults:returnResults forDocumentType:@"PDF417"];
        }
        else if ([self.delegate respondsToSelector:@selector(miSnapFinishedReturningEncodedImage:originalImage:andResults:)])
        {
            [self.delegate miSnapFinishedReturningEncodedImage:encodedImage originalImage:image andResults:returnResults];
        }
    }
}

- (void)analyzerException:(NSException *)exception
{
    if ([self.delegate respondsToSelector:@selector(miSnapBarcodeDidCatchException:)])
    {
        [self.delegate miSnapBarcodeDidCatchException:exception];
    }
}

- (IBAction)torchButtonAction:(id)sender
{
    if (self.camera.isTorchOn)
    {
        [self.camera turnTorchOff];
        [self.overlayView torchEnabled:NO];
    }
    else
    {
        [self.camera turnTorchOn];
        [self.overlayView torchEnabled:YES];
    }
}

- (IBAction)cancelButtonAction:(id)sender
{
    [self shutdownAnimated:YES];
    
    NSString* mibiDataString = [self buildBarcodeMIBIDataWithResult:kMiSnapBarcodeScannerResultCancelled];

    NSDictionary* results = @{kMiSnapBarcodeScannerResultCode:  kMiSnapBarcodeScannerResultCancelled,
                              kMiSnapBarcodeScannerMIBIData:    mibiDataString};
    
    if ([self.delegate respondsToSelector:@selector(miSnapCancelledWithResults:forDocumentType:)])
    {
        [self.delegate miSnapCancelledWithResults:results forDocumentType:@"PDF417"];
    }
    else if ([self.delegate respondsToSelector:@selector(miSnapCancelledWithResults:)])
    {
        [self.delegate miSnapCancelledWithResults:results];
    }
}

- (void)shutdownAnimated:(BOOL)animated
{
    [self.camera stop];
    self.camera.delegate = nil;
    [self.camera shutdown];
    self.analyzer = nil;
    
    if (self.navigationController)
    {
        [self.navigationController popViewControllerAnimated:animated];
    }
    else
    {
        [self dismissViewControllerAnimated:animated completion:nil];
    }
}

- (NSString *)buildBarcodeMIBIDataWithResult:(NSString*)resultCode
{
    NSMutableDictionary* mibiData = [MiSnapBarcodeScannerMibiData getMibiDataWithResult:resultCode andTorchStatus:self.camera.isTorchOn? @"YES" : @"NO"];
    
    NSError* error;
    NSData* jsonData = [NSJSONSerialization dataWithJSONObject:mibiData options:kNilOptions error:&error];
    if (error)
    {
        NSLog(@"error");
    }
    
    // Now convert the JSON string into a regular string (human readable)
    return [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
}

- (void)setLineWidth:(CGFloat)lineWidth lineColor:(UIColor *)lineColor lineAlpha:(CGFloat)lineAlpha vignetteAlpha:(CGFloat)vignetteAlpha
{
    self.lineWidth = lineWidth;
    self.lineColor = lineColor;
    self.lineAlpha = lineAlpha;
    self.vignetteAlpha = vignetteAlpha;
}

- (void)setGuideLandscapeFill:(CGFloat)landscapeFill portraitFill:(CGFloat)portraitFill aspectRatio:(CGFloat)guideAspectRatio cornerRadius:(CGFloat)cornerRadius
{
    self.landscapeFill = landscapeFill;
    self.portraitFill = portraitFill;
    self.guideAspectRatio = guideAspectRatio;
    self.cornerRadius = cornerRadius;
}

- (UIInterfaceOrientationMask)supportedInterfaceOrientations
{
    return UIInterfaceOrientationMaskAll;
}

- (BOOL)prefersStatusBarHidden
{
    return YES;
}

- (BOOL)shouldAutorotate
{
    return YES;
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
}

- (void)playShutterSoundAndVibrate:(BOOL)vibrate
{
    NSURL *soundFileURL = [[NSBundle bundleForClass:[self class]] URLForResource:@"barcode_photoshutter" withExtension:@"wav"];
    
    SystemSoundID sound1;
    AudioServicesCreateSystemSoundID((__bridge CFURLRef)soundFileURL, &sound1) ;
    
    if (vibrate)
    {
        AudioServicesPlayAlertSound(sound1);
    }
    else
    {
        AudioServicesPlaySystemSound(sound1);
    }
}

- (void)showFirstTimeTutorial
{
    dispatch_async(dispatch_get_main_queue(), ^{
        if (! self.tutorialViewController)
        {
            self.tutorialViewController = [MiSnapBarcodeScannerTutorialViewController instantiateFromStoryboard];
            self.tutorialViewController.delegate = self;
            self.tutorialViewController.guideMode = self.guideMode;
            self.tutorialViewController.modalTransitionStyle = UIModalTransitionStyleCrossDissolve;
            // For iOS 13, UIModalPresentationFullScreen is not the default, so be explicit
            self.tutorialViewController.modalPresentationStyle = UIModalPresentationFullScreen;
        }
        
        self.tutorialViewController.tutorialMode = MiSnapBarcodeScannerTutorialModeFirstTime;
        
        if (self.navigationController)
        {
            [self.navigationController pushViewController:self.tutorialViewController animated:YES];
        }
        else
        {
            [self presentViewController:self.tutorialViewController animated:YES completion:nil];
        }
    });
}

- (void)tutorialCancelButtonAction
{
    self.tutorialCancelled = YES;
    [self dismissTutorial];
    [self shutdownAnimated:NO];
}

- (void)tutorialRetryButtonAction {}

- (void)tutorialContinueButtonAction
{
    [self dismissTutorial];
}

- (void)dismissTutorial
{
    if (self.tutorialViewController != nil)
    {
        if (self.navigationController)
        {
            [self.navigationController popViewControllerAnimated:YES];
        }
        else
        {
            [self.tutorialViewController dismissViewControllerAnimated:YES completion:nil];
        }
        self.tutorialViewController.delegate = nil;
        self.tutorialViewController = nil;
    }
}

+ (NSString *)storyboardId
{
    return NSStringFromClass([self class]);
}

+ (MiSnapBarcodeScannerViewController *)instantiateFromStoryboard
{
    return [[UIStoryboard storyboardWithName:@"MiSnapBarcodeScanner" bundle:[NSBundle bundleForClass:self.class]] instantiateViewControllerWithIdentifier:[MiSnapBarcodeScannerViewController storyboardId]];
}

- (void)dealloc
{
    NSLog(@"MiSnapBarcodeScannerViewController is being deallocated");
}

@end
