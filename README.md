# PokéRoutine

Aplicativo Android em WebView, com dados existentes em `localStorage` (`pokeroutine-v1`). O formato foi mantido; a configuração `sound` é aditiva e usa o padrão ligado para instalações antigas.

## Estrutura

- `index.html`, `styles.css`, `app.js`: telas de agenda, tarefas, XP, companheiro, região, equipe, Pokédex, backup e preferências.
- `assets/fonts`: Pixelify Sans e Press Start 2P, com licenças SIL Open Font License para funcionar sem internet.
- `android/app/src/main/assets`: cópia offline dos arquivos da interface, fontes e 50 sprites PNG usados pelo app e verificados pelo workflow Android.
- `android/app/src/main/java`: Activity WebView, ponte JavaScript, agendamento/cancelamento nativo, restauração após reinicialização e backup pelo seletor Android.
- `android/app/src/main/res/mipmap-*`: ícone pixelado calendário + Poké Bola nas cinco densidades do launcher Android. `res/raw/pokeroutine_alert.wav` é uma melodia original curta de onda quadrada, sem áudio de jogos Pokémon.

O catálogo local tem os 12 iniciais das gerações 1–4, suas 36 formas evolutivas, Eevee e as oito evoluções, além das cinco espécies extras que já existiam. Os PNGs locais vêm do [repositório de sprites da PokéAPI](https://github.com/PokeAPI/sprites); o arquivo de licença e o aviso de direitos autorais do Pokémon foram preservados em `pokemon/LICENCE-PokeAPI.txt`. A disponibilidade do sprite no repositório não concede direitos sobre marcas e personagens Pokémon.

## Evoluções, encontros e dados antigos

Os níveis de treinador 5 e 10 evoluem os iniciais para a segunda e terceira forma, respectivamente. Eevee evolui no nível 5 para uma das oito formas com chances iguais. A forma, o histórico e a Pokédex são salvos no mesmo `localStorage` `pokeroutine-v1`; a migração acrescenta campos, mas mantém XP, tarefas, companheiro, Pokébolas e configurações antigas. As evoluções desbloqueadas não são revertidas ao trocar de companheiro ou reiniciar.

O primeiro uso oferece a escolha manual dos 12 iniciais ou um sorteio. Encontros podem revelar iniciais base, Eevee e espécies extras já existentes; uma recompensa surpresa de Pokébola tem chance de 8% na primeira conclusão registrada de cada tarefa. Reabrir e concluir de novo uma tarefa não duplica XP nem essa recompensa.

## Testar a versão web no computador ou no Motorola

O Node.js já instalado é suficiente; não é preciso instalar pacote npm. No computador, dê duplo clique em `start-pokeroutine.bat`, mantenha a janela aberta e abra `http://localhost:8765` no navegador. O servidor entrega apenas arquivos estáticos do projeto e para ao fechar a janela ou pressionar Ctrl+C.

Para abrir no Motorola pela mesma rede Wi-Fi, dê duplo clique em `start-pokeroutine-lan.bat`. Copie para o Chrome do celular um endereço `http://IP-DO-COMPUTADOR:8765` que aparecer no terminal. Se o Windows perguntar, permita o Node.js somente em redes privadas. O servidor é somente leitura e não sincroniza os dados: tarefas criadas no Chrome do celular ficam no armazenamento local desse navegador, separado dos dados do APK.

A página web/PWA não é um APK Android. No computador, `localhost` permite recursos de origem segura; no celular, o endereço IP pela rede local usa HTTP e pode não habilitar instalação PWA nem service worker. A versão web também não usa a ponte Android de alarmes: notificações do navegador dependem de permissões e timers da página podem atrasar ou parar quando ela fecha. Use o APK nativo para lembretes agendados confiáveis.

## Lembretes Android

Ative **Mais → Configurações → Notificações de tarefas** e aceite a permissão do Android 13+. Defina dia e horário na tarefa e marque **Lembrar no horário configurado**. O receptor Android agenda alarmes que chegam com o WebView fechado, atualiza o mesmo identificador quando reagendado, cancela quando concluído ou excluído e restaura alarmes futuros após reiniciar o aparelho.

Android 12+ pode exigir **Mais → Configurar horário exato** para aproximar o lembrete do minuto definido. Sem esse acesso, o app usa alarme compatível de menor precisão, que o modo Doze/economia de bateria pode atrasar. Os canais Android controlam som e importância; o canal sonoro é criado uma única vez. Se você mudar o som pelo sistema, a preferência do canal Android prevalece; ajuste em **Configurações → Apps → PokéRoutine → Notificações**. O navegador/PWA continua limitado: avisos em background não são garantidos pelo app aberto ou por timers JavaScript.

## Compilação

O projeto usa Gradle 8.9, Android Gradle Plugin 8.7.3, SDK 35 e `minSdk 23`. Build de depuração:

```powershell
$env:JAVA_HOME='D:\PokeroutineTools\jdk-17.0.20.1+1'
$env:ANDROID_HOME='D:\PokeroutineTools\android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
$env:GRADLE_USER_HOME='D:\PokeroutineTools\gradle-home'
Set-Location 'C:\Users\beano\Documents\Codex\2026-10-11\files-pasted-by-the-user-desenvolva\outputs\Pokeroutine\android'
& 'D:\PokeroutineTools\gradle-8.9\bin\gradle.bat' --no-daemon assembleDebug
```

Nesta alteração, `node --check` passou para `app.js`, `sw.js` e o arquivo de testes. `node --test tests/pokemon-system.test.cjs` passou em 11 testes cobrindo o catálogo, sprites de 96×96 nos assets web/Android e cache offline, migração legada, seleção manual/sorteada, barra de XP até o requisito, as 12 linhas iniciais, as oito evoluções de Eevee, persistência e recompensa única. O workflow Linux valida explicitamente os 50 sprites dentro do APK. Tentei abrir a página num browser local isolado, mas esse browser não alcançou o servidor; portanto, não confirmei a renderização visual nele nem num aparelho nesta sessão. Ainda não gerei um APK nesta alteração; a compilação Windows continua sujeita ao erro nativo AAPT2 0xC0000005 descrito abaixo e o workflow depende de o projeto ser enviado a um repositório GitHub privado pelo usuário.

### Gerar APK debug no GitHub Actions (Linux)

O workflow manual está em `.github/workflows/android-debug.yml`. Ele roda os testes Node, compila o módulo `:app` com Java 17, Gradle Wrapper 8.9, Android Gradle Plugin 8.7.3, SDK 35 e Build Tools 35.0.0. Antes de compilar, copia para os assets Android a interface, fontes e sprites atuais da versão web; depois verifica a assinatura e a presença dos 50 sprites dentro do APK. O `android/local.properties` da máquina local é ignorado e o workflow cria um arquivo temporário com o SDK instalado no runner Linux. A identidade de assinatura de depuração do runner é guardada no cache para builds seguintes. Como caches podem expirar, uma expiração pode fazer o Android exigir instalação com migração de dados novamente.

Para executar, crie no GitHub um repositório **privado** vazio chamado `Pokeroutine`. No PowerShell, entre na pasta do projeto `outputs\Pokeroutine` e execute `git init -b main`, `git add .`, `git status --short` e `git commit -m "Preparar build Android no GitHub Actions"`. Confira que `android/local.properties`, `work/` e `aapt-attempt.partial` não aparecem no status. Depois conecte o endereço do repositório privado com `git remote add origin https://github.com/SEU-USUARIO/Pokeroutine.git` (substitua `SEU-USUARIO` pelo seu usuário) e envie com `git push -u origin main`. Isso só envia quando você executar explicitamente o último comando; o projeto não foi enviado pelo Codex.

No GitHub, abra **Actions → PokéRoutine Android debug APK → Run workflow**. Aguarde o job verde e baixe `Pokeroutine-debug-apk` na seção **Artifacts** daquela execução. O arquivo dentro do ZIP será `app-debug.apk`. Se a etapa de verificação falhar, o job fica vermelho e não apresenta um artefato como build válido. O workflow ainda não foi executado: a pasta não está conectada a um repositório GitHub. Como o primeiro APK Linux terá uma chave de depuração diferente da compilação Windows já instalada, Android poderá recusar a atualização por assinatura incompatível. Antes de desinstalar a versão antiga nesse caso, exporte o backup dentro do app; depois instale o APK Linux e importe o arquivo de backup. Builds Linux seguintes reutilizam a chave enquanto o cache existir.

## Instalação no Motorola

Quando um build `assembleDebug` concluir, o APK estará em `android/app/build/outputs/apk/debug/app-debug.apk`. Copie para o celular e abra-o para instalar, permitindo instalação dessa origem quando o Android pedir. Para atualizar preservando o `localStorage`, instale por cima da versão anterior; não desinstale o app. O Android pode apresentar o pedido de permissão de notificações na primeira ativação.
