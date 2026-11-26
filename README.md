# Afrodite

Launcher (tela inicial) para Android, minimalista, preto com detalhes dourados.

- Fundo preto puro (`#000000`), que economiza bateria em telas AMOLED
- Relógio grande em fonte fina dourada (Montserrat), com a data embaixo
- Até 6 favoritos em texto, linha dourada fina como separador e dock opcional com 4 ícones
- Gaveta de apps em ordem alfabética, com busca e barra A–Z
- Ícones em dourado (ou os originais, se preferir) e três tons de dourado
- Gestos: deslizar para cima, para baixo e para os lados, toque duplo e toque longo

Feito em **Kotlin + Jetpack Compose**, arquitetura **MVVM**, `minSdk 26` (Android 8.0) e `targetSdk 35`.

---

## Instalar o APK no celular

Todo push na branch `main` gera os APKs pelo GitHub Actions. Existem **duas versões** do mesmo app:

| Versão | Arquivo | Como instalar | Toque duplo bloqueia a tela por… |
|---|---|---|---|
| **Padrão** (recomendada) | `Afrodite.apk` | Direto pelo celular | Administrador do dispositivo (o próximo desbloqueio pede o PIN) |
| **Completa** | `Afrodite-completo.apk` | Pelo computador (`adb install`) | Serviço de acessibilidade (a digital continua funcionando) |

> **Por que duas?** No Brasil, o Google Play Protect bloqueia qualquer APK baixado pelo navegador que tenha um serviço de acessibilidade, sem opção de "instalar mesmo assim". A versão padrão não tem esse serviço e instala normalmente. A completa tem, então só instala pelo computador.

### Versão padrão (pelo celular)

1. No celular, abra:
   **https://github.com/Netero-mac/Afrodite-/releases/download/ultima-versao/Afrodite.apk**
2. Toque no arquivo baixado.
3. Se o Android pedir, permita **"Instalar apps desconhecidos"** para o navegador ou o app de arquivos.
4. Toque em **Instalar**. Se o Play Protect sugerir uma verificação, pode deixar verificar.

### Versão completa (pelo computador)

1. Baixe **https://github.com/Netero-mac/Afrodite-/releases/download/ultima-versao/Afrodite-completo.apk** no computador.
2. No celular, ative as **Opções do desenvolvedor** (toque 7 vezes em *Sobre o telefone › Número da versão*) e ligue a **Depuração USB**.
3. Conecte o cabo e rode no terminal:
   ```bash
   adb install -r Afrodite-completo.apk
   ```
   (No Linux, o `adb` vem no pacote `android-tools-adb` ou `adb`.)

As duas versões têm o mesmo identificador e a mesma assinatura: dá para trocar de uma para a outra instalando por cima, sem perder as configurações.

### Atualizar

É só instalar o APK novo por cima. Todos os builds usam a mesma chave de assinatura (`app/afrodite-debug.keystore`), então o Android aceita a atualização e as suas configurações continuam.

A página de todas as versões fica em [Releases](https://github.com/Netero-mac/Afrodite-/releases). Os APKs também ficam na aba **Actions**, em **Artifacts › Afrodite-apks**.

---

## Primeiros passos

1. Abra o **Afrodite**. Na primeira vez, ele pede para ser definido como tela inicial padrão: toque em **Definir como padrão** e escolha o Afrodite.
   Se quiser trocar depois, vá em *Configurações do Android › Apps › Apps padrão › App de início*.
2. **Toque longo** em qualquer lugar da tela inicial abre as configurações. Lá você escolhe os favoritos, o dock, os gestos e a aparência.
3. Para o **toque duplo bloquear a tela**, ative a opção em *Configurações › Gestos*. O Afrodite explica o que vai pedir e abre a tela do Android certa:
   - **versão padrão:** autorizar o Afrodite como *app de administração do dispositivo*, só com a permissão de bloquear a tela. Depois de bloquear assim, a maioria dos celulares pede o PIN no desbloqueio seguinte. **Para desinstalar**, desligue a opção antes, porque o Android não desinstala um app que é administrador;
   - **versão completa (Android 9+):** ligar o serviço **"Afrodite · bloqueio de tela"** em Acessibilidade. A digital continua funcionando.

## Gestos

| Gesto | O que faz |
|---|---|
| Deslizar para cima | Abre a gaveta de apps |
| Deslizar para baixo | Abre a barra de notificações |
| Deslizar para a direita | Abre o app escolhido nas configurações |
| Deslizar para a esquerda | Abre o app escolhido nas configurações |
| Toque duplo | Bloqueia a tela (se ativado) |
| Toque longo | Abre as configurações |
| Tocar no relógio / na data | Abre os alarmes / a agenda |

Na gaveta:

- A busca ignora acentos. Se **só um app** corresponder ao que você digitou, ele abre direto.
- Arraste o dedo na **barra A–Z** para pular de letra.
- **Toque longo** num app mostra: informações do app, desinstalar, adicionar/remover dos favoritos, adicionar ao dock e ocultar.
- Puxar a lista para baixo (quando ela já está no topo) fecha a gaveta. O botão Voltar também fecha.

O botão **Voltar** nunca sai do launcher. O botão **Início** sempre volta para a tela inicial.

---

## Compilar no computador

Requisitos: **JDK 17** e **Android SDK 35** (o Android Studio já instala os dois).

```bash
git clone https://github.com/Netero-mac/Afrodite-.git
cd Afrodite-
./gradlew assemblePadraoDebug      # gera app/build/outputs/apk/padrao/debug/app-padrao-debug.apk
./gradlew assembleCompletoDebug    # versão com acessibilidade
./gradlew testPadraoDebugUnitTest  # roda os testes de unidade
```

Com o celular conectado por USB (depuração USB ligada):

```bash
./gradlew installPadraoDebug     # ou installCompletoDebug
```

Ou abra a pasta no Android Studio e clique em ▶ Run.

---

## Estrutura do código

```
app/src/main/java/com/netero/afrodite/
├── AfroditeApp.kt              # Application: cria os repositórios
├── MainActivity.kt             # Activity HOME; botão Voltar e Início
├── data/
│   ├── AppInfo.kt              # modelo de app + ícones
│   ├── AppsRepository.kt       # lista de apps (LauncherApps), cache em disco, ícones
│   ├── LauncherSettings.kt     # modelo das configurações + tons de dourado
│   └── SettingsRepository.kt   # leitura e gravação com DataStore
├── system/
│   ├── SystemActions.kt        # abrir apps, notificações, telas do Android
│   ├── DeviceLock.kt           # bloqueio pelo administrador do dispositivo
│   └── AccessibilityBridge.kt  # ponte para o serviço de acessibilidade (se existir)
├── ui/
│   ├── LauncherViewModel.kt    # ViewModel único (MVVM)
│   ├── AfroditeRoot.kt         # empilha as telas e as animações
│   ├── home/                   # tela inicial, relógio, dock
│   ├── drawer/                 # gaveta, barra A–Z, menu do toque longo
│   ├── settings/               # configurações
│   ├── onboarding/             # boas-vindas (definir como padrão)
│   ├── components/             # ícone, seletor de apps, campo de busca etc.
│   └── theme/                  # cores, fonte e tema
└── util/                       # busca sem acentos, toast

app/src/completo/               # só na versão completa: serviço de acessibilidade
```

### Como ficou rápido e leve

- A lista de apps fica salva em disco. Ao abrir, ela aparece na hora, e a versão atualizada é carregada em segundo plano.
- Os ícones são convertidos fora da thread principal e guardados num cache em memória com limite de tamanho. Depois que a lista carrega, eles são pré-carregados aos poucos.
- O launcher escuta o sistema (`LauncherApps.Callback`) e atualiza a lista sozinho quando um app é instalado, removido ou atualizado.

### Ícones dourados

- Se o app tem ícone **monocromático** (o mesmo dos "ícones temáticos" do Android 13+), ele é pintado de dourado.
- Se não tem, o ícone colorido vira **duotone**: o brilho de cada pixel é multiplicado pelo dourado, e o desenho continua reconhecível.

---

## Permissões

| Permissão | Para quê |
|---|---|
| `EXPAND_STATUS_BAR` | Abrir as notificações com o gesto de deslizar para baixo |
| `REQUEST_DELETE_PACKAGES` | Abrir a tela de desinstalação do Android |
| `<queries>` com `LAUNCHER` | Enxergar os apps que têm ícone (Android 11+) |
| Administrador do dispositivo (opcional) | Só a política `force-lock`: bloquear a tela no toque duplo |
| Serviço de acessibilidade (opcional, só na versão completa) | Só bloqueia a tela e abre as notificações. Não lê a tela e não coleta dados |

## Observações

- A chave `app/afrodite-debug.keystore` é uma **chave de debug** (senha `android`), pública de propósito, para os APKs do Actions instalarem por cima uns dos outros. Para publicar na Play Store, crie uma chave de release própria e não coloque essa chave no repositório.

## Licença

O código é distribuído sob a licença MIT. Veja [LICENSE](LICENSE).
A fonte Montserrat segue a SIL Open Font License (veja `licenses/Montserrat-OFL.txt`).
