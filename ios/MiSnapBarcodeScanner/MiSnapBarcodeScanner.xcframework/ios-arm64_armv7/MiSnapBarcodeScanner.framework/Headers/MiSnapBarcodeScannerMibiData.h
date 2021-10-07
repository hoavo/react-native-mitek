//
//  MiSnapBarcodeScannerMibiData.h
//  MiSnapBarcodeScanner
//
//  Created by Runi Kovoor on 2/5/15.
//  Copyright (c) 2015 Runi Kovoor. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface MiSnapBarcodeScannerMibiData : NSObject

+ (NSMutableDictionary *)getMibiDataWithResult:(NSString *)resultCode
                               andTorchStatus:(NSString *)torchState;

@end
