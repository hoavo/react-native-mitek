//
//  MiSnapBarcodeScannerLightTutorialViewController.m
//  MiSnapBarcodeScannerLightUX
//
//  Created by Stas Tsuprenko on 6/18/18.
//  Copyright © 2018 miteksystems. All rights reserved.
//

#import "MiSnapBarcodeScannerLightTutorialViewController.h"
#import "MiSnapBarcodeScannerLight+NSString.h"

@interface MiSnapBarcodeScannerLightTutorialViewController ()

@property (nonatomic) UILabel *dontShowLabel;
@property (nonatomic) UIImageView *checkboxImageView;
@property (nonatomic) UIView *tapView;
@property (nonatomic) BOOL shouldShowFirstTimeTutorial;
@property (nonatomic) UIInterfaceOrientation statusbarOrientation;

@end

@implementation MiSnapBarcodeScannerLightTutorialViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
    // Do any additional setup after loading the view from its nib.
}

- (void)viewWillAppear:(BOOL)animated
{
    [super viewWillAppear:animated];
    
    self.statusbarOrientation = [UIApplication sharedApplication].statusBarOrientation;
    
    [self.cancelButton setTitle:[NSString localizedBarcodeStringForKey:@"dialog_misnap_barcode_cancel"] forState:UIControlStateNormal];
    [self.cancelButton setAccessibilityLabel:[NSString localizedBarcodeStringForKey:@"dialog_misnap_barcode_cancel"]];
    
    if (self.tutorialMode == MiSnapBarcodeScannerLightTutorialModeFirstTime ||
        self.tutorialMode == MiSnapBarcodeScannerLightTutorialModeFailover ||
        self.tutorialMode == MiSnapBarcodeScannerLightTutorialModeHelp)
    {
        [self.continueButton setTitle:[NSString localizedBarcodeStringForKey:@"dialog_misnap_barcode_capture"] forState:UIControlStateNormal];
        [self.continueButton setAccessibilityLabel:[NSString localizedBarcodeStringForKey:@"dialog_misnap_barcode_capture"]];
        [self.retryButton setTitle:[NSString localizedBarcodeStringForKey:@"dialog_misnap_barcode_capture"] forState:UIControlStateNormal];
        [self.retryButton setAccessibilityLabel:[NSString localizedBarcodeStringForKey:@"dialog_misnap_barcode_capture"]];
    }
    
    self.cancelButton.titleLabel.textAlignment = NSTextAlignmentCenter;
    self.continueButton.titleLabel.textAlignment = NSTextAlignmentCenter;
    self.retryButton.titleLabel.textAlignment = NSTextAlignmentCenter;
    
    if (self.tutorialMode == MiSnapBarcodeScannerLightTutorialModeFirstTime)
    {
        self.backgroundImageName = @"barcode_light_tutorial_id_back_with_background";
    }
    
    if ((self.backgroundImageName != nil) && ([self.backgroundImageName isEqualToString:@""] == NO))
    {
        self.backgroundImageView.image = [self imageWithName:self.backgroundImageName orientation:self.statusbarOrientation guideMode:self.guideMode];
        [self.view bringSubviewToFront:self.backgroundImageView];
    }
    else
    {
        self.backgroundImageView.image = nil;
    }
    
    if (self.tutorialMode == MiSnapBarcodeScannerLightTutorialModeFirstTime)
    {
        [self addDontShowAgainCheckbox];
    }
    
    if (self.navigationController != nil)
    {
        [self.navigationController setNavigationBarHidden:YES];
    }
    
    if (self.timeoutDelay == 0.0)
    {
        [self showButtons];
    }
    else
    {
        self.cancelButton.alpha = 0.0;
        self.retryButton.alpha = 0.0;
        self.continueButton.alpha = 0.0;
        self.buttonBackgroundView.alpha = 0.0;
        
        if (self.numberOfButtons == 0)
        {
            [self performSelector:@selector(continueButtonAction:) withObject:nil afterDelay:self.timeoutDelay/1000.0];
        }
        else
        {
            [self performSelector:@selector(showButtons) withObject:nil afterDelay:self.timeoutDelay/1000.0];
        }
    }
}

- (void)viewWillDisappear:(BOOL)animated
{
    [super viewWillDisappear:animated];
    
    [NSObject cancelPreviousPerformRequestsWithTarget:self];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (BOOL)shouldAutorotate
{
    return YES;
}

- (UIInterfaceOrientationMask)supportedInterfaceOrientations
{
    return UIInterfaceOrientationMaskAll;
}

- (BOOL)prefersStatusBarHidden
{
    return YES;
}

- (void)viewWillTransitionToSize:(CGSize)size withTransitionCoordinator:(id<UIViewControllerTransitionCoordinator>)coordinator
{
    [super viewWillTransitionToSize:size withTransitionCoordinator:coordinator];
    
    [coordinator animateAlongsideTransition:^(id<UIViewControllerTransitionCoordinatorContext> _Nonnull context)
    {
        self.statusbarOrientation = [UIApplication sharedApplication].statusBarOrientation;
        
        if ((self.backgroundImageName != nil) && ([self.backgroundImageName isEqualToString:@""] == NO))
        {
            self.backgroundImageView.image = [self imageWithName:self.backgroundImageName orientation:self.statusbarOrientation guideMode:self.guideMode];
            [self.view bringSubviewToFront:self.backgroundImageView];
        }
        else
        {
            self.backgroundImageView.image = nil;
        }
        
        CGFloat offset = 0;
        if (UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPhone && size.width / size.height > 1.8 && UIInterfaceOrientationIsLandscape(self.statusbarOrientation))
        {
            offset = 20;
        }
        else if (UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPhone && size.height / size.width > 1.8 && UIInterfaceOrientationIsPortrait(self.statusbarOrientation))
        {
            offset = 35;
        }
        else if (UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad)
        {
            offset = 30;
        }
        
        self.dontShowLabel.center = CGPointMake(size.width * 0.5, size.height - self.buttonBackgroundView.frame.size.height - self.dontShowLabel.frame.size.height - offset);
        self.checkboxImageView.center = CGPointMake(self.dontShowLabel.center.x - self.dontShowLabel.frame.size.width * 0.5 - self.checkboxImageView.frame.size.width * 0.5 - 5, self.dontShowLabel.center.y);
        self.tapView.center = CGPointMake(self.dontShowLabel.center.x - self.checkboxImageView.frame.size.width * 0.5, self.dontShowLabel.center.y);
        [self.view bringSubviewToFront:self.dontShowLabel];
        [self.view bringSubviewToFront:self.checkboxImageView];
        [self.view bringSubviewToFront:self.tapView];
    }
    completion:nil];
}

#pragma mark - Implementation

- (void)setTutorialMode:(MiSnapBarcodeScannerLightTutorialMode)tutorialMode
{
    if (tutorialMode < MiSnapBarcodeScannerLightTutorialModeNone || tutorialMode > MiSnapBarcodeScannerLightTutorialModeFailover)
    {
        _tutorialMode = MiSnapBarcodeScannerLightTutorialModeNone;
    }
    else
    {
        _tutorialMode = tutorialMode;
    }
    
    int numberOfButtons = 0;
    switch (_tutorialMode)
    {
        case MiSnapBarcodeScannerLightTutorialModeNone:
            break;
            
        case MiSnapBarcodeScannerLightTutorialModeFirstTime:
        case MiSnapBarcodeScannerLightTutorialModeFailover:
        case MiSnapBarcodeScannerLightTutorialModeHelp:
        default:
            numberOfButtons = 2;
            break;
    }
    self.numberOfButtons = numberOfButtons;
    self.timeoutDelay = 0;
}

- (void)setNumberOfButtons:(int)numberOfButtons
{
    if (numberOfButtons < 0)
    {
        _numberOfButtons = 0;
    }
    else if (numberOfButtons > 3)
    {
        _numberOfButtons = 3;
    }
    else
    {
        _numberOfButtons = numberOfButtons;
    }
}

- (UIImage *)imageWithName:(NSString *)imageName orientation:(UIInterfaceOrientation)orientation guideMode:(MiSnapBarcodeLightGuideMode)guideMode
{
    NSString *tutorialImageName;
    
    if (UIInterfaceOrientationIsLandscape(orientation))
    {
        tutorialImageName = [NSString stringWithFormat:@"%@.jpg", imageName];
    }
    else
    {
        
        if (guideMode == MiSnapBarcodeLightGuideModeDevicePortraitGuideLandscape)
        {
            tutorialImageName = [NSString stringWithFormat:@"%@_portrait_2.jpg", imageName];
        }
        else
        {
            tutorialImageName = [NSString stringWithFormat:@"%@_portrait.jpg", imageName];
        }
    }
    
    UIImage *tutorialImage = [UIImage imageNamed:tutorialImageName];
    return tutorialImage;
}

- (void)showButtons
{
    if ((self.speakableText != nil) && ([self.speakableText isEqualToString:@""] == NO))
    {
        NSString* localizedStr = [NSString localizedBarcodeStringForKey:self.speakableText];
        self.backgroundImageView.accessibilityLabel = localizedStr;
        UIAccessibilityPostNotification(UIAccessibilityAnnouncementNotification, localizedStr);
    }
    
    if (self.numberOfButtons == 0)
    {
        self.cancelButton.hidden = YES;
        self.retryButton.hidden = YES;
        self.continueButton.hidden = YES;
        
        self.cancelButton.enabled = NO;
        self.retryButton.enabled = NO;
        self.continueButton.enabled = NO;
        
        self.buttonBackgroundView.hidden = NO;
    }
    else if (self.numberOfButtons == 1)
    {
        self.cancelButton.hidden = YES;
        self.retryButton.hidden = NO;
        self.continueButton.hidden = YES;
        
        self.cancelButton.enabled = NO;
        self.retryButton.enabled = YES;
        self.continueButton.enabled = NO;
        
        self.buttonBackgroundView.hidden = NO;
    }
    else if (self.numberOfButtons == 2)
    {
        self.cancelButton.hidden = NO;
        self.retryButton.hidden = YES;
        self.continueButton.hidden = NO;
        
        self.cancelButton.enabled = YES;
        self.retryButton.enabled = NO;
        self.continueButton.enabled = YES;
        
        self.buttonBackgroundView.hidden = NO;
    }
    else if (self.numberOfButtons == 3)
    {
        self.cancelButton.hidden = NO;
        self.retryButton.hidden = NO;
        self.continueButton.hidden = NO;
        
        self.cancelButton.enabled = YES;
        self.retryButton.enabled = YES;
        self.continueButton.enabled = YES;
        
        self.buttonBackgroundView.hidden = NO;
    }
    
    [UIView animateWithDuration:0.5 animations:^{
        self.cancelButton.alpha = 1.0;
        self.retryButton.alpha = 1.0;
        self.continueButton.alpha = 1.0;
        self.buttonBackgroundView.alpha = 1.0;
    }];
}

- (IBAction)cancelButtonAction:(id)sender
{    
    if ([self.delegate respondsToSelector:@selector(tutorialCancelButtonAction)])
    {
        [self.delegate tutorialCancelButtonAction];
    }
}

- (IBAction)continueButtonAction:(id)sender
{
    if ([self.delegate respondsToSelector:@selector(tutorialContinueButtonAction)])
    {
        [self.delegate tutorialContinueButtonAction];
    }
}

- (IBAction)retryButtonAction:(id)sender
{
    if ([self.delegate respondsToSelector:@selector(tutorialRetryButtonAction)])
    {
        [self.delegate tutorialRetryButtonAction];
    }
}

- (void)addDontShowAgainCheckbox
{
    self.shouldShowFirstTimeTutorial = YES;
    
    [[NSUserDefaults standardUserDefaults] setBool:self.shouldShowFirstTimeTutorial forKey:kMiSnapBarcodeScannerLightShowTutorial];
    [[NSUserDefaults standardUserDefaults] synchronize];
    
    CGFloat screenWidth = [UIScreen mainScreen].bounds.size.width;
    CGFloat screenHeight = [UIScreen mainScreen].bounds.size.height;
    
    CGFloat offset = 0;
    if (UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPhone && screenWidth / screenHeight > 1.8 && UIInterfaceOrientationIsLandscape(self.statusbarOrientation))
    {
        offset = 20;
    }
    else if (UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPhone && screenHeight / screenWidth > 1.8 && UIInterfaceOrientationIsPortrait(self.statusbarOrientation))
    {
        offset = 35;
    }
    else if (UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad)
    {
        offset = 30;
    }
    
    NSString *dontShowString = @"Don't show this screen again";
    CGFloat dontShowAgainFontSize = 17.0;
    
    CGSize maximumSize = CGSizeMake(screenWidth * 0.9, 50);
    CGRect dontShowRect = [dontShowString boundingRectWithSize:maximumSize options:NSStringDrawingTruncatesLastVisibleLine attributes:@{ NSFontAttributeName : [UIFont systemFontOfSize:dontShowAgainFontSize] } context:nil];
    //NSLog(@"Don't show this again size: %@", NSStringFromCGRect(dontShowRect));
    
    self.dontShowLabel = [[UILabel alloc] initWithFrame:CGRectMake(0, 0, dontShowRect.size.width, dontShowRect.size.height)];
    self.dontShowLabel.center = CGPointMake(screenWidth * 0.5, screenHeight - self.buttonBackgroundView.frame.size.height - self.dontShowLabel.frame.size.height - offset);
    self.dontShowLabel.text = dontShowString;
    self.dontShowLabel.font = [UIFont systemFontOfSize:dontShowAgainFontSize];
    [self.dontShowLabel setTextColor:[UIColor blackColor]];

    self.checkboxImageView = [[UIImageView alloc] init];
    self.checkboxImageView.image = [UIImage imageNamed:@"barcode_light_checkbox_unchecked"];
    self.checkboxImageView.frame = CGRectMake(0, 0, dontShowRect.size.height - 5, dontShowRect.size.height - 5);
    self.checkboxImageView.center = CGPointMake(self.dontShowLabel.center.x - self.dontShowLabel.frame.size.width * 0.5 - self.checkboxImageView.frame.size.width * 0.5 - 5, self.dontShowLabel.center.y);
    
    self.tapView = [[UIView alloc] initWithFrame:CGRectMake(0, 0, self.checkboxImageView.frame.size.width + self.dontShowLabel.frame.size.width + 30, self.dontShowLabel.frame.size.height + 20)];
    self.tapView.center = CGPointMake(self.dontShowLabel.center.x - self.checkboxImageView.frame.size.width * 0.5, self.dontShowLabel.center.y);
    self.tapView.userInteractionEnabled = YES;
    self.tapView.accessibilityIdentifier = dontShowString;
    
    UITapGestureRecognizer *dontShowTap = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(dontShowTapped)];
    dontShowTap.numberOfTapsRequired = 1;
    [self.tapView addGestureRecognizer:dontShowTap];
    
    [self.view addSubview:self.dontShowLabel];
    [self.view addSubview:self.checkboxImageView];
    [self.view addSubview:self.tapView];
}

- (void)dontShowTapped
{
    //NSLog(@"Don't show was tapped");
    if (self.shouldShowFirstTimeTutorial)
    {
        self.checkboxImageView.image = [UIImage imageNamed:@"barcode_light_checkbox_checked"];
        self.shouldShowFirstTimeTutorial = NO;
    }
    else
    {
        self.checkboxImageView.image = [UIImage imageNamed:@"barcode_light_checkbox_unchecked"];
        self.shouldShowFirstTimeTutorial = YES;
    }
    
    [[NSUserDefaults standardUserDefaults] setBool:self.shouldShowFirstTimeTutorial forKey:kMiSnapBarcodeScannerLightShowTutorial];
    [[NSUserDefaults standardUserDefaults] synchronize];
}

+ (NSString *)storyboardId
{
    return NSStringFromClass([self class]);
}

+ (MiSnapBarcodeScannerLightTutorialViewController *)instantiateFromStoryboard
{
    return [[UIStoryboard storyboardWithName:@"MiSnapBarcodeScannerLight" bundle:[NSBundle bundleForClass:self.class]] instantiateViewControllerWithIdentifier:[MiSnapBarcodeScannerLightTutorialViewController storyboardId]];
}

- (void)dealloc
{
    //NSLog(@"MiSnapBarcodeScannerTutorialViewController is being deallocated");
}

@end
