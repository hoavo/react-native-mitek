//
//  MiSnapBarcodeScannerTutorialViewController.h
//  MiSnap
//
//  Created by Greg Fisch on 7/30/14.
//  Copyright (c) 2014 mitek. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <MiSnapBarcodeScanner/MiSnapBarcodeScanner.h>

typedef NS_ENUM(NSInteger, MiSnapBarcodeScannerTutorialMode)
{
    MiSnapBarcodeScannerTutorialModeNone = 0,
    MiSnapBarcodeScannerTutorialModeFirstTime,
    MiSnapBarcodeScannerTutorialModeHelp,
    MiSnapBarcodeScannerTutorialModeFailover
};

@protocol MiSnapBarcodeScannerTutorialViewControllerDelegate <NSObject>

- (void)tutorialCancelButtonAction;
- (void)tutorialContinueButtonAction;
- (void)tutorialRetryButtonAction;

@end

@interface MiSnapBarcodeScannerTutorialViewController : UIViewController

+ (MiSnapBarcodeScannerTutorialViewController *)instantiateFromStoryboard;

/*! @abstract a pointer back to the method implementing the callback methods MiSnapBarcodeScannerTutorial will invoke
 upon transaction termination
 */
@property (weak, nonatomic) NSObject<MiSnapBarcodeScannerTutorialViewControllerDelegate>* delegate;

@property (weak, nonatomic) IBOutlet UIButton *cancelButton;
@property (weak, nonatomic) IBOutlet UIButton *continueButton;
@property (weak, nonatomic) IBOutlet UIButton *retryButton;
@property (weak, nonatomic) IBOutlet UIImageView *backgroundImageView;
@property (weak, nonatomic) IBOutlet UIView *buttonBackgroundView;

@property (nonatomic) NSString *backgroundImageName;
@property (nonatomic) NSString *speakableText;
@property (nonatomic, assign) MiSnapBarcodeScannerTutorialMode tutorialMode;

@property (nonatomic, assign) int numberOfButtons;
@property (nonatomic, assign) NSTimeInterval timeoutDelay;

@property (nonatomic, assign) MiSnapBarcodeGuideMode guideMode;

@end
