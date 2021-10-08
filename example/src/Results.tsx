import * as React from 'react';

import {
  TouchableOpacity,
  StyleSheet,
  Text,
  Image,
  ScrollView,
  SafeAreaView,
  StatusBar,
} from 'react-native';

export default function Result({ data = {} as any, onClose = () => {} }) {
  return (
    <SafeAreaView style={styles.container}>
      <TouchableOpacity onPress={onClose}>
        <Text>Close</Text>
      </TouchableOpacity>
      <ScrollView style={styles.scrollView}>
        <Image
          style={{ height: 400, width: 400, backgroundColor: 'white' }}
          resizeMode="contain"
          source={{ uri: `data:image/jpeg;base64,${data.miSnapImage}` }}
        />
        <Text style={styles.text}>{JSON.stringify(data.miSnapMibiData)}</Text>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    paddingTop: StatusBar.currentHeight,
    marginHorizontal: 20,
  },
  scrollView: {
    flex: 1,
  },
  text: {
    marginTop: 20,
  },
});
