# mitek-misnap-rn-bridge

mitek-misnap-rn-bridge

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
