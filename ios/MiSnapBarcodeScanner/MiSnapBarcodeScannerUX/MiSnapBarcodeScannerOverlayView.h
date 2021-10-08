//
//  MiSnapBarcodeScannerOverlayView.h
//  MiSnapBarcodeScanner
//
//  Created by Stas Tsuprenko on 4/16/17.
//  Copyright © 2017 MitekSystems. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <MiSnapBarcodeScanner/MiSnapBarcodeScanner.h>

@interface MiSnapBarcodeScannerOverlayView : UIView

@property (nonatomic, weak) IBOutlet UIButton *cancelButton;
@property (nonatomic, weak) IBOutlet UIButton *torchButton;
@property (nonatomic, weak) IBOutlet UIImageView *logoImageView;

- (void)initWithGuideMode:(MiSnapBarcodeGuideMode)guideMode;

- (void)showTorch:(BOOL)hasTorch;

- (void)torchEnabled:(BOOL)torchIsOn;

- (void)updateWithOrientation:(UIInterfaceOrientation)orientation;

- (void)setLineWidth:(CGFloat)lineWidth lineColor:(UIColor *)lineColor lineAlpha:(CGFloat)lineAlpha vignetteAlpha:(CGFloat)vignetteAlpha;

- (void)setGuideLandscapeFill:(CGFloat)landscapeFill portraitFill:(CGFloat)portraitFill aspectRatio:(CGFloat)guideAspectRatio cornerRadius:(CGFloat)cornerRadius;

- (void)hideAllUIElements;

@end
