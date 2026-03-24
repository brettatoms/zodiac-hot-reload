# Zodiac Hot Reload

[![Clojars Project](https://img.shields.io/clojars/v/com.github.brettatoms/zodiac-hot-reload.svg)](https://clojars.org/com.github.brettatoms/zodiac-hot-reload)

A [Zodiac](https://github.com/brettatoms/zodiac) extension for hot reload of
server-rendered applications using
[ring-hot-reload](https://github.com/brettatoms/ring-hot-reload).

When server-side code changes, the browser automatically re-fetches the page and
morphs the DOM in place using [idiomorph](https://github.com/bigskysoftware/idiomorph),
preserving scroll position, focus, and form state.

### Getting Started

```clojure
(ns myapp
  (:require [zodiac.core :as z]
            [zodiac.ext.hot-reload :as z.hot-reload]))

(defn handler [_request]
  [:html
   [:body
    [:h1 "Hello world"]]])

(defn routes []
  ["/" {:get #'handler}])

(z/start {:routes routes
          :extensions [(z.hot-reload/init)]})
```

That's it — save a file or eval a form in your editor and the browser updates
automatically.

### Options

`zodiac.ext.hot-reload/init` accepts an optional map of options, passed directly
to `ring.hot-reload.core/hot-reloader`:

- `:watch-paths` — directories to watch (default `["src"]`)
- `:watch-extensions` — file extensions that trigger reload
  (default `#{".clj" ".cljc" ".edn" ".html" ".css"}`)
- `:uri-prefix` — WebSocket endpoint path (default `"/__hot-reload"`)
- `:inject?` — predicate `(fn [request response])` controlling script injection
  (default: always inject into full HTML pages)
- `:debounce-ms` — debounce window in ms (default `100`)
- `:bust-css-cache?` — append cache-busting param to stylesheet URLs on reload
  (default `false`)

Example with custom options:

```clojure
(z.hot-reload/init {:watch-paths ["src" "resources/templates"]
                    :debounce-ms 200})
```

### With Zodiac Assets

For a full development experience with Vite asset management and hot reload,
use together with [zodiac-assets](https://github.com/brettatoms/zodiac-assets):

```clojure
(require '[zodiac.ext.assets :as z.assets]
         '[zodiac.ext.hot-reload :as z.hot-reload])

(z/start {:routes routes
          :extensions [(z.assets/init {:manifest-path "myapp/.vite/manifest.json"
                                       :asset-resource-path "myapp/assets"
                                       :vite {:mode :dev-server}})
                       (z.hot-reload/init {:watch-extensions #{".clj" ".cljc" ".edn" ".html"}})]})
```

In this setup:
- **CSS/JS changes** are handled instantly by Vite's native HMR (no page reload)
- **Clojure/template changes** trigger a re-fetch and DOM morph via ring-hot-reload

When using with a Vite dev server, narrow `:watch-extensions` to server-side
files only (exclude `.css`) since Vite handles CSS/JS HMR natively.

### nREPL Integration

For the best development experience, add the ring-hot-reload nREPL middleware
so that evaluating code in your editor triggers a browser reload automatically —
no file save needed.

Add `.nrepl.edn` to your project:

```clojure
{:middleware [ring.hot-reload.nrepl/wrap-hot-reload-nrepl]}
```

Or configure it in CIDER via `.dir-locals.el`:

```elisp
((clojure-mode
  (eval . (progn
            (make-local-variable 'cider-jack-in-nrepl-middlewares)
            (add-to-list 'cider-jack-in-nrepl-middlewares "ring.hot-reload.nrepl/wrap-hot-reload-nrepl")))))
```

See the [ring-hot-reload README](https://github.com/brettatoms/ring-hot-reload)
for more details.

### License

Copyright © 2025 Brett Adams

Distributed under the MIT License. See [LICENSE](LICENSE) for details.
