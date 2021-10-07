import * as React from 'react';

import { TouchableOpacity, StyleSheet, View, Text } from 'react-native';
import {
  setDocumentType,
  setServerTypeAndVersion,
  doSnap,
  doCaptureFace,
} from 'react-native-mitek';

export default function App() {
  const snapDriverLicenseFront = async () => {
    try {
      setDocumentType('DRIVER_LICENSE');
      setServerTypeAndVersion('test', '0.0');
      let snapResults = await doSnap();
      console.log(snapResults);
    } catch (e) {
      console.error(e);
    }
  };

  const snapDriverLicenseBack = async () => {
    try {
      setDocumentType('PDF417');
      setServerTypeAndVersion('test', '0.0');
      let snapResults = await doSnap();
      console.log(snapResults);
    } catch (e) {
      console.error(e);
    }
  };

  const captureFace = async () => {
    try {
      let captureResults = await doCaptureFace();
      console.log(captureResults);
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <View style={styles.container}>
      <TouchableOpacity
        style={styles.button}
        onPress={() => snapDriverLicenseFront()}
      >
        <Text style={styles.text}>Driver's License Front</Text>
      </TouchableOpacity>
      <TouchableOpacity
        style={styles.button}
        onPress={() => snapDriverLicenseBack()}
      >
        <Text style={styles.text}>Driver's License Back</Text>
      </TouchableOpacity>
      <TouchableOpacity style={styles.button} onPress={() => captureFace()}>
        <Text style={styles.text}>Facial Capture</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  button: {
    alignItems: 'center',
    width: '75%',
    height: 60,
    backgroundColor: '#0F2B29',
    borderRadius: 30,
    paddingVertical: 15,
    paddingHorizontal: 36,
    marginVertical: 5,
  },
  text: {
    fontSize: 20,
    color: '#3FD899',
  },
});
