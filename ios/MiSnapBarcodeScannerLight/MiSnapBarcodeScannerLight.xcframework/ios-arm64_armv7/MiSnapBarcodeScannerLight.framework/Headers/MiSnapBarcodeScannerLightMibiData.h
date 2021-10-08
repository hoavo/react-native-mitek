//
//  MiSnapBarcodeScannerLightMibiData.h
//  MiSnapBarcodeScannerLight
//
//  Created by Stas Tsuprenko on 6/18/18.
//  Copyright © 2018 miteksystems. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface MiSnapBarcodeScannerLightMibiData : NSObject

+ (NSMutableDictionary *)getMibiDataWithResult:(NSString *)resultCode
                               andTorchStatus:(NSString *)torchState;

@end
