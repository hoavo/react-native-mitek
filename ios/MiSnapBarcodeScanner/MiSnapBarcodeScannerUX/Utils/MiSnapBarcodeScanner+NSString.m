//
//  MiSnapBarcodeScanner+NSString.m
//  MiSnapDevApp
//
//  Created by Stas Tsuprenko on 5/18/18.
//  Copyright © 2018 Mitek Systems. All rights reserved.
//

#import "MiSnapBarcodeScanner+NSString.h"

@implementation NSString (MiSnapBarcodeScanner)

+ (NSString *)localizedStringForKey:(NSString *)key
{
    return [[NSBundle mainBundle] localizedStringForKey:key value:key table:@"MiSnapBarcodeScannerLocalizable"];
}

@end
