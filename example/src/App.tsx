import * as React from 'react';

import { TouchableOpacity, StyleSheet, View, Text } from 'react-native';
import { startMiSnapWorkflow } from 'react-native-mitek';
import Result from './Results';

export default function App() {
  const [result, setResult] = React.useState(null);
  const snapDriverLicenseFront = async () => {
    try {
      const snapResults = await startMiSnapWorkflow('DRIVER_LICENSE');
      setResult(snapResults as any);
    } catch (e) {
      console.error(e);
    }
  };

  const snapDriverLicenseBack = async () => {
    try {
      const snapResults = await startMiSnapWorkflow('PDF417');
      setResult(snapResults as any);
    } catch (e) {
      console.error(e);
    }
  };
  const onClose = () => {
    setResult(null);
  };
  if (result) {
    return <Result onClose={onClose} data={result} />;
  }

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
