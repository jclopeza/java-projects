# <LIBRARY_NAME>

#[[
## Requisitos
]]#

* Node Package Manager v3.10.10 (`npm`)
* Webpack v1.13.3
* Webpack Development Server v1.16.2 (*sólo desarrollo*)

```
$ npm install -g webpack@1.13.3
$ npm install -g webpack-dev-server@1.16.2
```

> **Nota**: Se recomienda usar [*Node Version Manager*](https://github.com/creationix/nvm) (`nvm`).

#[[
## Instalación
]]#

```
$ git clone ${repo-git}
$ cd this-repo-folder
$ npm install
```

#[[
## Desarrollo
]]#

```
$ npm start
```

Y abrir en el navegador:

* [Server](http://localhost:8080/webpack-dev-server/<ENTRY_NAME>)
* [TDD](http://localhost:8080/webpack-dev-server/spec)

#[[
## Empaquetado
]]#

```
$ npm run build
```

Ficheros generados en la carpeta `./dist`.
