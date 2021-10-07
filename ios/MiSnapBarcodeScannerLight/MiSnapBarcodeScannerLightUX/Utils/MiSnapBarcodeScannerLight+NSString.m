//
//  MiSnapBarcodeScannerLight+NSString.m
//  MiSnapBarcodeScannerLightUX
//
//  Created by Stas Tsuprenko on 6/18/18.
//  Copyright © 2018 miteksystems. All rights reserved.
//

#import "MiSnapBarcodeScannerLight+NSString.h"

@implementation NSString (MiSnapBarcodeScannerLight)

+ (NSString *)localizedBarcodeStringForKey:(NSString *)key
{
    return [[NSBundle mainBundle] localizedStringForKey:key value:key table:@"MiSnapBarcodeScannerLightLocalizable"];
}

@end
