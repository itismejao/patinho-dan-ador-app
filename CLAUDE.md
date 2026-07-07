# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## O que é

**Patinho Dançador** — joguinho mobile para crianças pequenas (<5 anos): um patinho papercraft 3D que dança, faz quack, bate asas, voa e mergulha num laguinho. Three.js + WebAudio. Todo o código de produto vive num único arquivo: `index.html` (~1200 linhas). UI e textos são em português (pt-BR). O jogo web não tem build; o **Capacitor** empacota esse mesmo HTML como app Android nativo (WebView) para a Play Store.

## Rodando localmente

Os áudios são carregados via `fetch`, então **precisa de um servidor HTTP** (não funciona abrindo o arquivo via `file://`):

```bash
npx serve .
# ou
python3 -m http.server 8080
```

Testar de preferência no celular (mesma rede) ou no modo device do DevTools — o jogo é feito para toque em tela pequena. Sem os MP3s o jogo ainda funciona: o quack cai num fallback sintetizado e a dança roda sem música. Não há lint ou testes.

## Empacotamento Android (Capacitor)

O app Android é só um WebView carregando o `index.html`. **`index.html` + `assets/` na raiz são a fonte de verdade**; `npm run build` copia esses arquivos para `www/` (o `webDir` do Capacitor, gerado e gitignored — nunca edite `www/` à mão). Fluxo:

```bash
npm run build          # raiz → www/  (rode SEMPRE após mexer no jogo)
npx cap sync android   # www/ + capacitor.config.json → projeto android/
```

Pré-requisitos deste ambiente (o `java` do sistema não existe; use o JDK do Android Studio):

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"   # JDK 21
# Android SDK já em ~/Library/Android/sdk (apontado por android/local.properties, gitignored)
```

Builds (a partir de `android/`, com `JAVA_HOME` setado):

```bash
./gradlew assembleDebug    # APK de teste → app/build/outputs/apk/debug/app-debug.apk
./gradlew bundleRelease    # AAB p/ Play  → app/build/outputs/bundle/release/app-release.aab
```

**Assinatura de release:** `app/build.gradle` lê `android/keystore.properties` (gitignored; modelo em `keystore.properties.example`). Sem esse arquivo o release sai **sem assinar**. O keystore de upload e suas senhas devem ser guardados para sempre — perdê-los impede atualizar o app publicado. Identidade do app: appId `io.github.jmarcos.patinhodancador` (permanente), `versionCode`/`versionName` em `app/build.gradle`. `targetSdk 36` (atende o mínimo da Play).

**Ainda pendente para publicar:** ícone/splash próprios (hoje usa o padrão do Capacitor — gerar com `@capacitor/assets` a partir de um PNG fonte) e o processo "Projetado para a Família" da Play (ver Roadmap no README).

## Arquitetura (tudo em `index.html`)

Three.js r128 via CDN. O `<script>` inline está organizado em seções comentadas, na ordem: cena/luzes → helpers papercraft → cenário estático → modelo do pato → vida do cenário → sons → sistema de ações → interação → loop.

- **Estética papercraft** — todo mesh passa por `mesh(geo, color, opts)`, que aplica `paperMat` (MeshStandardMaterial flat-shaded) e desenha arestas brancas via `addEdges`. A aparência "papel amassado" vem de `crumple(geo, amt)`, que desloca vértices por um hash determinístico baseado em `|x|` — vértices espelhados recebem o mesmo deslocamento, mantendo o pato simétrico e o casco fechado. `flattenTop` e `coxinha` deformam geometrias base (esfera/icosaedro) na forma do corpo e da cabeça.

- **Sistema de ações** — o núcleo do jogo. Existe uma única ação por vez em `let action = {name, t, dur}`. `startAction(name)` inicia; `updateDuck(dt, time)` no loop lê `action.name` e a fração de progresso `p = action.t/action.dur` para dirigir a pose do pato (posição, rotação, asas, bico, pernas, sombra). As cinco ações são `quack`, `dance`, `flap`, `fly`, `dive` — cada uma é um bloco `if(n==='...')` dentro de `updateDuck`. `dance` é uma coreografia em 4 partes fatiada por faixas de `p`, sincronizada com a música (~12s). Ao terminar (`p>=1`), `action` volta a `null`.

- **Áudio (contorna autoplay mobile)** — `S.*` são efeitos sintetizados on-the-fly via `tone`/`noise` (WebAudio). Os MP3s (`assets/dance.mp3`, `assets/quack.mp3`) são baixados e decodificados via `decodeAudioData` **no primeiro `pointerdown` em qualquer lugar da tela** (listener `once`), o que satisfaz a política de autoplay. Se DANÇAR for a primeira interação, a música ainda está decodificando: `dancePending` faz ela entrar sincronizada (`playDanceMusic(offset)`) assim que o buffer fica pronto. `danceAudio` (elemento `<Audio>`) é o último recurso se o WebAudio falhar.

- **Vida do cenário** — patinhos nadando/voando, familinha atravessando, borboletas, ondinhas, juncos etc. são atualizados a cada frame em `updateLife(dt, t)`, separado de `updateDuck`. Elementos que reaparecem (voadores, familinha) usam funções `reset*` para reposicionar fora da tela.

- **Interação** — `pointerdown/move/up` no canvas distinguem toque de arraste (limiar de 10px). Toque = ação; se acertar o pato no raycast, dispara ação **aleatória**. Arraste horizontal gira o pato via `userYaw` com inércia (`yawVel`). Botões `.btn[data-act]` chamam `startAction` diretamente e usam `stopPropagation` para não virar arraste.

## Convenções ao editar

- **Não introduza build/framework/npm.** O projeto é intencionalmente um HTML único sem toolchain — assets locais funcionam direto no WebView (planejado empacotamento com Capacitor para a Play Store; ver README).
- Ajustes de comportamento do pato quase sempre são em `updateDuck`; ajustes de cenário em `updateLife`; novos sons em `S`.
- Público-alvo é criança pequena: alvos de toque grandes, poucos passos, muito feedback visual/sonoro.
