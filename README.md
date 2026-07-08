# 🐥 Patinho Dançador

Joguinho mobile para crianças pequenas: um patinho papercraft 3D que dança, faz quack, bate as asas, dá uma voadinha e mergulha no laguinho. Feito com Three.js, sons via WebAudio e visual low-poly de papel dobrado.

## ✨ Funcionalidades

- **Toque no patinho** → ele faz uma ação surpresa aleatória
- **Arraste pro lado** → gira o patinho em qualquer direção (com inércia)
- **Botões grandões** (pensados pra dedinhos pequenos): QUACK, DANÇAR, ASAS, VOAR, SPLASH e OVO
- **DANÇAR**: coreografia em 4 partes sincronizada com a música (~12s); apertar de novo para a música
- **OVO**: o patinho agacha e treme, o ovo racha com barulho e nasce um patinho que sai passeando pelo cenário
- Quack de pato real (dobrado, com variação de tom); demais efeitos sintetizados via WebAudio
- Cenário vivo: lago com patinhos nadando e ondinhas, familinha de patos atravessando o gramado, patinhos voando no céu, borboletas, juncos, vitórias-régias, flores, cogumelos e arco-íris de papel
- Botão de mudo 🔊/🔇
- **🔒 Trava de criança** (cadeado no canto superior esquerdo) — veja abaixo

## 🔒 Trava de criança (para os pais)

O **cadeado no canto superior esquerdo** deixa a criança brincar sem sair do app nem mexer em nada por acidente.

- **Travar:** um toque no cadeado (fica 🔒 amarelo). O joguinho continua **100% jogável** — só o "sair" é bloqueado.
- **Destravar:** **segure o cadeado por ~1,6s** (uma barrinha vai enchendo). Um toque rápido não destrava — assim a criança não desativa sozinha.

Ao travar, o app faz o máximo possível para prender a criança dentro do jogo:
- **Fixa a tela** (recurso "Fixar tela" do Android via `startLockTask`), bloqueando **Início** e **Recentes**;
- **Prende o botão Voltar** do Android.

> **Importante — limite do Android:** como o app não é "dispositivo dedicado", o fixar tela é o mesmo do pinning manual: dá para sair pelo **gesto de desafixar do sistema** (em geral segurar **Voltar + Recentes**, ou deslizar pra cima e segurar). Para reforçar, ative em **Configurações → Segurança → Fixar tela** a opção **"Pedir PIN/senha ao desafixar"** — aí nem o gesto solta sem o PIN. Um bloqueio 100% inescapável exigiria provisionar o aparelho como dispositivo dedicado (fora do escopo de um app de consumidor).

## 📁 Estrutura

```
index.html          → o jogo inteiro (cena, modelo do pato, animações, sons)
assets/
  dance.mp3         → música da dança (~12s, mono)
  quack.mp3         → som de quack real
```

## ▶️ Rodando localmente

Os áudios são carregados via `fetch`, então precisa de um servidor HTTP (não funciona com `file://`):

```bash
npx serve .
# ou
python3 -m http.server 8080
```

Abra `http://localhost:8080` (de preferência no celular, na mesma rede, ou use o modo device do DevTools).

> Sem os arquivos de áudio o jogo continua funcionando: o quack cai num fallback sintetizado e a dança roda sem música.

## 🔊 Detalhes de áudio

Os MP3 são decodificados via WebAudio (`decodeAudioData`) no **primeiro toque em qualquer lugar da tela**, o que contorna as políticas de autoplay dos navegadores mobile. Se o botão DANÇAR for a primeira interação, a música entra sincronizada assim que a decodificação termina.

## 📦 Apps nativos (Capacitor)

O app é o mesmo `index.html` rodando num WebView (Android) / WKWebView (iOS). `index.html` + `assets/` na raiz são a fonte; `npm run build` copia para `www/` (webDir do Capacitor). Ícone e splash saem de `resources/` via `@capacitor/assets`.

### Android

```bash
# uma vez / sempre que mudar o jogo
npm run build && npx cap sync android

# builds (o java do sistema não está no PATH; use o JDK do Android Studio)
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
cd android
./gradlew assembleDebug   # APK de teste  → app/build/outputs/apk/debug/app-debug.apk
./gradlew bundleRelease   # AAB p/ a Play → app/build/outputs/bundle/release/app-release.aab
```

**Assinatura:** copie `android/keystore.properties.example` → `android/keystore.properties` (gitignored) e gere o keystore com `keytool` (comando no próprio arquivo). Guarde o keystore e as senhas para sempre. appId: `io.github.jmarcos.patinhodancador` · targetSdk 36.

### iOS (macOS + Xcode)

```bash
npm run build && npx cap sync ios
npx cap run ios          # roda no simulador (pergunta o device)
npx cap open ios         # abre no Xcode (p/ publicar: Product → Archive)
```

Capacitor 8 usa **Swift Package Manager** (não CocoaPods). O plugin nativo `ScreenPin` é só Android — no iOS a trava vira no-op (o equivalente é o **Acesso Guiado**, ativado à mão pelo usuário). Emojis aparecem como "?" **só no simulador** (fonte de emoji reduzida do Simulator); no iPhone real renderizam normal.

## 🗺️ Roadmap → lojas

- [x] Empacotar com **Capacitor** (Android + iOS) — os assets locais funcionam direto no WebView
- [x] Ícone adaptativo + splash screen próprios (`@capacitor/assets` a partir de `resources/`)
- [x] Gerar keystore de upload e configurar `android/keystore.properties` (fora do repo; **guardar para sempre**)
- **Google Play:**
  - [ ] Conta Google Play Developer + política **Projetado para a Família** (público-alvo < 5 anos, política de privacidade, formulário de classificação etária)
  - [ ] Teste fechado (12+ testadores / 14 dias, exigência para contas pessoais novas) e publicação
- **App Store:**
  - [ ] Conta Apple Developer (US$ 99/ano) + assinatura de distribuição (Xcode)
  - [ ] Registro no App Store Connect + **categoria Kids** (parental gate, sem analytics/ads de terceiros) e publicação

## 🛠️ Stack

Three.js r128 (CDN) · WebAudio API · HTML/CSS/JS puro, sem build
