# mitek-misnap-rn-bridge

mitek-misnap-rn-bridge

## Demo Examples
https://user-images.githubusercontent.com/2727930/136515785-ff1ea3ad-0eb1-42c5-a5e8-c559d0abbb87.MP4

https://user-images.githubusercontent.com/2727930/136515625-93f00717-db20-4c2b-a04d-b2e884b2d1e3.mp4


## Installation



```sh
npm install mitek-misnap-rn-bridge
```

## Usage



```js
import { startMiSnapWorkflow, MISNAPTYPE, setServerTypeAndVersion } from 'mitek-misnap-rn-bridge';

// ...

setServerTypeAndVersion('test', '0.0');
const snapResults = await startMiSnapWorkflow(MISNAPTYPE.DRIVER_LICENSE);
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT
