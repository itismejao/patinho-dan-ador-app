# 🐥 Patinho Dançador

Joguinho mobile para crianças pequenas: um patinho papercraft 3D que dança, faz quack, bate as asas, dá uma voadinha e mergulha no laguinho. Feito com Three.js, sons via WebAudio e visual low-poly de papel dobrado.

## ✨ Funcionalidades

- **Toque no patinho** → ele faz uma ação surpresa aleatória
- **Arraste pro lado** → gira o patinho em qualquer direção (com inércia)
- **Botões grandões** (pensados pra dedinhos pequenos): QUACK, DANÇAR, ASAS, VOAR e SPLASH
- **DANÇAR**: coreografia em 4 partes sincronizada com a música (~12s); apertar de novo para a música
- Quack de pato real (dobrado, com variação de tom); demais efeitos sintetizados via WebAudio
- Cenário vivo: lago com patinhos nadando e ondinhas, familinha de patos atravessando o gramado, patinhos voando no céu, borboletas, juncos, vitórias-régias, flores, cogumelos e arco-íris de papel
- Botão de mudo 🔊/🔇

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

## 🗺️ Roadmap → Play Store

- [ ] Empacotar com **Capacitor** (`@capacitor/android`) — os assets locais funcionam direto no WebView
- [ ] Ícone adaptativo + splash screen
- [ ] Orientação retrato travada
- [ ] Conta Google Play Developer + política **Projetado para a Família** (público-alvo < 5 anos, política de privacidade, formulário de classificação etária)
- [ ] Teste fechado (12+ testadores / 14 dias, exigência para contas pessoais novas) e publicação

## 🛠️ Stack

Three.js r128 (CDN) · WebAudio API · HTML/CSS/JS puro, sem build
