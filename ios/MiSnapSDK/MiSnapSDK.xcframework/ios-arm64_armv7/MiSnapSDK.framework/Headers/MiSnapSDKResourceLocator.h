//
//  MiSnapSDKResourceLocator.h
//  MiSnap
//
//  Created by Greg Fisch on 11/4/14.
//  Copyright (c) 2014 mitek. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <MiSnapSDK/MiSnapSDKParameters.h>

@interface MiSnapSDKResourceLocator : NSObject

@property (nonatomic, weak) NSString* languageKey;

- (UIImage *)getLocalizedImage:(NSString *)imageName;
- (UIImage *)getLocalizedImage:(NSString *)imageName extension:(NSString *)extension;

- (UIImage *)getLocalizedImage:(NSString *)imageName withOrientation:(UIInterfaceOrientation)orientation withOrientationMode:(MiSnapOrientationMode)orientationMode;
- (UIImage *)getLocalizedImage:(NSString *)imageName extension:(NSString *)extension withOrientation:(UIInterfaceOrientation)orientation withOrientationMode:(MiSnapOrientationMode)orientationMode;

- (UIImage *)getLocalizedTutorialImage:(NSString *)imageName withOrientation:(UIInterfaceOrientation)orientation withOrientationMode:(MiSnapOrientationMode)orientationMode;
- (UIImage *)getLocalizedTutorialImage:(NSString *)imageName extension:(NSString *)extension withOrientation:(UIInterfaceOrientation)orientation withOrientationMode:(MiSnapOrientationMode)orientationMode;

- (NSString*)getLocalizedString:(NSString*)key;

+ (MiSnapSDKResourceLocator *)initWithLanguageKey:(NSString*)key bundle:(NSBundle *)bundle localizableStringsName:(NSString *)localizableStringsName;

@end
