# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## O que é

**Patinho Dançador** — joguinho mobile para crianças pequenas (<5 anos): um patinho papercraft 3D que dança, faz quack, bate asas, voa e mergulha num laguinho. Three.js + WebAudio. Todo o código de produto vive num único arquivo: `index.html` (~1200 linhas). UI e textos são em português (pt-BR). O jogo web não tem build; o **Capacitor** empacota esse mesmo HTML como app nativo — Android (WebView, para a Play Store) e iOS (WKWebView, para a App Store).

## Rodando localmente

Os áudios são carregados via `fetch`, então **precisa de um servidor HTTP** (não funciona abrindo o arquivo via `file://`):

```bash
npx serve .
# ou
python3 -m http.server 8080
```

Testar de preferência no celular (mesma rede) ou no modo device do DevTools — o jogo é feito para toque em tela pequena. Sem os MP3s o jogo ainda funciona: o quack cai num fallback sintetizado e a dança roda sem música. Não há lint ou testes.

## Empacotamento nativo (Capacitor: Android + iOS)

O app nativo é só um WebView/WKWebView carregando o `index.html`. **`index.html` + `assets/` na raiz são a fonte de verdade**; `npm run build` copia esses arquivos para `www/` (o `webDir` do Capacitor, gerado e gitignored — nunca edite `www/` à mão). Ícone e splash saem de `resources/` (`resources/icon-only.png`, `resources/splash.png`, `resources/splash-dark.png`) via `npx capacitor-assets generate --android|--ios --assetPath resources`. Fluxo:

```bash
npm run build          # raiz → www/  (rode SEMPRE após mexer no jogo)
npx cap sync android   # www/ + capacitor.config.json → projeto android/
npx cap sync ios       # idem para ios/
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

**iOS (só no macOS + Xcode):** a pasta `ios/` é um projeto Xcode padrão; Capacitor 8 usa **Swift Package Manager**, não CocoaPods (por isso não há `Podfile`/`pod install`). Rodar/buildar:

```bash
npx cap run ios     # build via xcodebuild + instala no simulador (--target <UDID> p/ escolher)
npx cap open ios    # abre no Xcode; publicar = Product → Archive (precisa de conta Apple Developer)
```

O `ios/.gitignore` gerado já exclui `App/build`, `App/App/public` (cópia do www), `DerivedData` e `xcuserdata`. **Peculiaridade do Simulator:** emojis renderizam como "?" (fonte de emoji reduzida do simulador) — no iPhone real aparecem normal; não é bug do app nem de encoding.

**Código nativo local:** o projeto é HTML único, mas o `android/` tem **um** plugin Java local (sem npm): `ScreenPinPlugin.java` (chama `startLockTask`/`stopLockTask` para a trava de criança), registrado em `MainActivity.onCreate` via `registerPlugin(...)` **antes** de `super.onCreate`. É a exceção deliberada ao "sem código nativo" — usar quando algo só é possível na camada Android. Chamado do JS por `Capacitor.Plugins.ScreenPin` (com guarda p/ rodar como no-op no navegador **e no iOS**, que não tem esse plugin — lá o equivalente é o Acesso Guiado, ativado à mão pelo usuário).

**Assinatura de release:** `app/build.gradle` lê `android/keystore.properties` (gitignored; modelo em `keystore.properties.example`). Sem esse arquivo o release sai **sem assinar**. O keystore de upload e suas senhas devem ser guardados para sempre — perdê-los impede atualizar o app publicado. Identidade do app: appId `io.github.jmarcos.patinhodancador` (permanente), `versionCode`/`versionName` em `app/build.gradle`. `targetSdk 36` (atende o mínimo da Play).

**Já feito:** ícone/splash próprios (patinho papercraft, gerados de `resources/`) e keystore de upload configurado (`android/keystore.properties` aponta p/ `~/keys/patinho-upload.jks`, fora do repo). **Ainda pendente para publicar:** o processo "Projetado para a Família" da Play e a categoria Kids da App Store (ver Roadmap no README).

## Arquitetura (tudo em `index.html`)

Three.js r128 via CDN. O `<script>` inline está organizado em seções comentadas, na ordem: cena/luzes → helpers papercraft → cenário estático → modelo do pato → vida do cenário → sons → sistema de ações → interação → loop.

- **Estética papercraft** — todo mesh passa por `mesh(geo, color, opts)`, que aplica `paperMat` (MeshStandardMaterial flat-shaded) e desenha arestas brancas via `addEdges`. A aparência "papel amassado" vem de `crumple(geo, amt)`, que desloca vértices por um hash determinístico baseado em `|x|` — vértices espelhados recebem o mesmo deslocamento, mantendo o pato simétrico e o casco fechado. `flattenTop` e `coxinha` deformam geometrias base (esfera/icosaedro) na forma do corpo e da cabeça.

- **Sistema de ações** — o núcleo do jogo. Existe uma única ação por vez em `let action = {name, t, dur}`. `startAction(name)` inicia; `updateDuck(dt, time)` no loop lê `action.name` e a fração de progresso `p = action.t/action.dur` para dirigir a pose do pato (posição, rotação, asas, bico, pernas, sombra). As cinco ações são `quack`, `dance`, `flap`, `fly`, `dive` — cada uma é um bloco `if(n==='...')` dentro de `updateDuck`. `dance` é uma coreografia em 4 partes fatiada por faixas de `p`, sincronizada com a música (~12s). Ao terminar (`p>=1`), `action` volta a `null`.

- **Áudio (contorna autoplay mobile)** — `S.*` são efeitos sintetizados on-the-fly via `tone`/`noise` (WebAudio). Os MP3s (`assets/dance.mp3`, `assets/quack.mp3`) são baixados e decodificados via `decodeAudioData` **no primeiro `pointerdown` em qualquer lugar da tela** (listener `once`), o que satisfaz a política de autoplay. Se DANÇAR for a primeira interação, a música ainda está decodificando: `dancePending` faz ela entrar sincronizada (`playDanceMusic(offset)`) assim que o buffer fica pronto. `danceAudio` (elemento `<Audio>`) é o último recurso se o WebAudio falhar.

- **Vida do cenário** — patinhos nadando/voando, familinha atravessando, borboletas, ondinhas, juncos etc. são atualizados a cada frame em `updateLife(dt, t)`, separado de `updateDuck`. Elementos que reaparecem (voadores, familinha) usam funções `reset*` para reposicionar fora da tela.

- **Interação** — `pointerdown/move/up` no canvas distinguem toque de arraste (limiar de 10px). Toque = ação; se acertar o pato no raycast, dispara ação **aleatória**. Arraste horizontal gira o pato via `userYaw` com inércia (`yawVel`). Botões `.btn[data-act]` chamam `startAction` diretamente e usam `stopPropagation` para não virar arraste.

## Convenções ao editar

- **Mobile-first, sempre.** O alvo real é o app Android (WebView do Capacitor) rodando num celular na mão de uma criança pequena — nunca o desktop. Toda feature deve ser pensada e validada para esse contexto: usar `pointer*`/`touch` (nunca depender de `hover`, mouse ou teclado), alvos de toque grandes, layout que cabe em tela pequena retrato com `env(safe-area-inset-*)`, e comportamento que faça sentido dentro de um WebView (ex.: prender o **botão Voltar** do Android via truque de histórico `pushState`/`popstate`, já que não há plugin `@capacitor/app`). Lembrar dos limites do WebView: **não dá para bloquear os botões Home/Recentes** só com web — isso exige "Fixar tela"/screen pinning do próprio Android. Testar no navegador serve de sanity check, mas o veredito é no celular/APK.
- **Não introduza build/framework/npm.** O projeto é intencionalmente um HTML único sem toolchain — assets locais funcionam direto no WebView (planejado empacotamento com Capacitor para a Play Store; ver README).
- Ajustes de comportamento do pato quase sempre são em `updateDuck`; ajustes de cenário em `updateLife`; novos sons em `S`.
- Público-alvo é criança pequena: alvos de toque grandes, poucos passos, muito feedback visual/sonoro.
- **Trava de criança (cadeado, canto sup. esquerdo)** — `#lock` alterna `locked`. Travar = um toque; destravar = **segurar ~1,6s** (barra de progresso `.fill`), para a criança não desativar sozinha. O joguinho segue 100% jogável travado. Ao travar, o app faz três coisas: (1) chama o plugin nativo **`ScreenPin`** (`screenPin(true)`) que dispara `Activity.startLockTask()` — "Fixar tela" do Android, bloqueia Home/Recentes; (2) prende o botão Voltar (guarda de histórico `pushState`/`popstate`); (3) bloqueia menu de contexto. Destravar chama `stopLockTask()`. Como o app **não é device owner**, o `startLockTask` equivale ao pinning manual (escapável pelo gesto de desafixar do sistema) — é o máximo possível sem provisionar o aparelho.
