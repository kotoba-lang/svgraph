# svgraph

Portable CLJC semantic graph contract for SVG presentation sources.

This repository no longer treats the TypeScript/browser package as the source of
truth. The migration authority is plain Clojure data:

- `src/svgraph/core.cljc` builds SVGraph nodes, relation edges, a presentation
  view, and an Office Causal payload.
- `test/svgraph/core_test.clj` fixes the portable data contract.
- `examples/` keeps representative SVG/DrawingML fixtures for future adapter
  layers.
- `docs/index.html` is a static project page and does not require a JS build.

## Verify

```sh
clojure -M:test
```

## Scope

SVGraph is the stable handoff layer between SVG-like presentation sources and
downstream renderers, Office Causal projections, or browser/package adapters.
Adapters may implement richer visual conversion, but the shared contract remains
CLJC data under `src/`.

## License

MIT
