# mitek-misnap-rn-bridge

mitek-misnap-rn-bridge

## Demo Examples
| -------------------------------- | -------------------------------- |
| ![Android Mitek Demo](https://user-images.githubusercontent.com/2727930/136513356-61d3d44d-9963-4cad-b020-012ed62ebf9c.mp4) | ![iOS Demo](https://user-images.githubusercontent.com/2727930/136513380-6b41cbac-9352-4162-8503-52399f0da01d.MP4) |



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
