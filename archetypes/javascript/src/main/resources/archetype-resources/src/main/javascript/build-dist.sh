#!/bin/bash

set -e

echo_msg() {
    local readonly msg=$1
    echo "~~~ build-dist: $msg"
}

exit_error() {
    local readonly msg=$1
    echo_msg "Terminating with error(s): $msg"
    exit 1
}

[ $# -eq 0 ] || exit_error "usage: build-dist.sh"

which npm > /dev/null || exit_error "No npm in PATH"

name=$(echo "console.log(require('./package.json').name)" | node)

echo_msg "Installing node packages ..."
npm install

echo_msg "Webpack build ..."
npm run build
sed -i -- 's/const /var /g' dist/$name.js

exit 0
