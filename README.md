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

Todo push na branch `main` gera um APK novo pelo GitHub Actions.

### Opção 1: link direto (mais fácil)

1. No celular, abra:
   **https://github.com/Netero-mac/Afrodite-/releases/download/ultima-versao/Afrodite-debug.apk**
2. Toque no arquivo baixado.
3. Se o Android pedir, permita **"Instalar apps desconhecidos"** para o navegador ou o app de arquivos (ele leva você direto à opção).
4. Toque em **Instalar**.

A página de todas as versões fica em [Releases](https://github.com/Netero-mac/Afrodite-/releases).

### Opção 2: pelo artefato do Actions

1. Abra a aba **Actions** do repositório e entre na execução mais recente de **Gerar APK**.
2. Em **Artifacts**, baixe **Afrodite-debug** (vem em um `.zip`).
3. Extraia o `.zip` e instale o `Afrodite-debug.apk` como na opção 1.

### Atualizar

É só instalar o APK novo por cima. Todos os builds usam a mesma chave de assinatura (`app/afrodite-debug.keystore`), então o Android aceita a atualização e as suas configurações continuam.

---

## Primeiros passos

1. Abra o **Afrodite**. Na primeira vez, ele pede para ser definido como tela inicial padrão: toque em **Definir como padrão** e escolha o Afrodite.
   Se quiser trocar depois, vá em *Configurações do Android › Apps › Apps padrão › App de início*.
2. **Toque longo** em qualquer lugar da tela inicial abre as configurações. Lá você escolhe os favoritos, o dock, os gestos e a aparência.
3. Para o **toque duplo bloquear a tela**, ative a opção nas configurações. O Afrodite explica o motivo e abre a tela de Acessibilidade, onde você liga o serviço **"Afrodite · bloqueio de tela"**. Esse recurso precisa do Android 9 ou mais novo.

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
./gradlew assembleDebug          # gera app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # roda os testes de unidade
```

Com o celular conectado por USB (depuração USB ligada):

```bash
./gradlew installDebug
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
│   └── LockAccessibilityService.kt  # bloqueio de tela por toque duplo
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
| Serviço de acessibilidade (opcional) | Só bloqueia a tela e abre as notificações. Não lê a tela e não coleta dados |

## Observações

- A chave `app/afrodite-debug.keystore` é uma **chave de debug** (senha `android`), pública de propósito, para os APKs do Actions instalarem por cima uns dos outros. Para publicar na Play Store, crie uma chave de release própria e não coloque essa chave no repositório.
- A fonte Montserrat é distribuída sob a licença SIL Open Font License (veja `licenses/Montserrat-OFL.txt`).
