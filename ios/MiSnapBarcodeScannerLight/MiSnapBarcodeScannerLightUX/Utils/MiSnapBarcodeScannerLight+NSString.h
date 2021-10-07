//
//  MiSnapBarcodeScannerLight+NSString.h
//  MiSnapBarcodeScannerLightUX
//
//  Created by Stas Tsuprenko on 6/18/18.
//  Copyright © 2018 miteksystems. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface NSString (MiSnapBarcodeScannerLight)

/**
 Returns the localized string from the FacialCaptureLocalizable string table.
 If the key is not found, it's returned as the default value.
 
 @param key The lookup key associated with a translated value
 @return The localized string value
 @see FacialCaptureLocalizable.strings
 */
+ (NSString *)localizedBarcodeStringForKey:(NSString *)key;

@end
