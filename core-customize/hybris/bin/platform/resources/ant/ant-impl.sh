#!/usr/bin/env bash
# ant の中身。Docker の builder コンテナの中で動く(ホストからは ./ant や ant.cmd が呼ぶ)。
# 本物の Commerce の ant ターゲットと同じ名前で受け付けて、Gradle と疑似 Commerce の起動に読み替える。
set -euo pipefail

props=()
targets=()
for a in "$@"; do
  case "$a" in
    -D*) props+=("-P${a#-D}") ;;
    *) targets+=("$a") ;;
  esac
done
[ ${#targets[@]} -eq 0 ] && targets=(all)

gradle_q() { gradle --console=plain "$@" "${props[@]+"${props[@]}"}"; }

run_jar() {
  [ -f build/libs/commerce.jar ] || { echo "build/libs/commerce.jar がありません。先に 'ant all' を実行してください。"; exit 1; }
  java -jar build/libs/commerce.jar "$@"
}

prop_value() {
  for p in "${props[@]+"${props[@]}"}"; do
    case "$p" in -P$1=*) echo "${p#-P$1=}"; return ;; esac
  done
}

echo "Buildfile: /workspace/core-customize/hybris/bin/platform/build.xml (platformlite)"
for t in "${targets[@]}"; do
  echo ""
  echo "${t}:"
  case "$t" in
    clean)              gradle_q clean ;;
    all|build|compile)  gradle_q bootJar ;;
    initialize)         run_jar --hybris.mode=initialize ;;
    updatesystem)       run_jar --hybris.mode=update ;;
    unittests)          gradle_q test ;;
    integrationtests)   gradle_q test -Pintegration ;;
    alltests)           gradle_q test -Pintegration ;;
    importImpex)
      res="$(prop_value resource)"
      [ -n "$res" ] || { echo "使い方: ant importImpex -Dresource=<ImpEx のパス(hybris/bin からの相対、または絶対)>"; exit 1; }
      run_jar --hybris.mode=import --hybris.impex="$res" ;;
    *)
      echo "BUILD FAILED"
      echo "Target \"$t\" does not exist in the project \"platform\"."
      echo "使えるターゲット: clean all build initialize updatesystem unittests integrationtests alltests importImpex"
      exit 1 ;;
  esac
done
echo ""
echo "BUILD SUCCESSFUL"
