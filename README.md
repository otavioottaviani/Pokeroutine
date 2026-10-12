# PokéRoutine

PokéRoutine é um organizador de tarefas para Android com uma apresentação retrô inspirada em RPGs Pokémon. Concluir tarefas dá XP ao treinador e acompanha o crescimento do Pokémon companheiro. A interface e os recursos essenciais ficam no próprio aplicativo; a rotina não é enviada para um servidor.

> Projeto de fã, sem vínculo oficial com Nintendo, The Pokémon Company ou Pokémon. Pokémon e Poké Ball são marcas de seus respectivos titulares.

## O que o aplicativo faz

- Cadastra, edita, conclui e exclui tarefas; tarefas podem ter data, horário e lembrete.
- Mantém agenda semanal e progresso de XP do treinador.
- Permite escolher um inicial das gerações 1 a 4 ou sortear um no primeiro uso.
- Evolui as linhas dos iniciais no nível 5 e novamente no nível 10. Eevee evolui no nível 5 para uma das oito formas, sorteada uma única vez.
- Registra formas encontradas/evoluídas na Pokédex e oferece áreas de equipe e região.
- Pode conceder uma Poké Bola surpresa ao concluir uma tarefa pela primeira vez; repetir a conclusão não duplica XP ou prêmio.
- Agenda lembretes locais no Android e permite ativar/desativar o som nas configurações do app.
- Inclui sprites e fontes locais para que a interface principal não dependa de baixar esses recursos da internet.

## Dados, privacidade e backup

No Android, tarefas e progresso ficam no armazenamento local do aplicativo, usando `localStorage` (`pokeroutine-v1`). Não existe sincronização automática entre aparelhos ou entre o APK e a versão web. Cada navegador e cada instalação Android mantêm seus próprios dados.

Use a opção **Backup** dentro do aplicativo para exportar seus dados para um arquivo JSON. Guarde esse arquivo fora do app. Se reinstalar o app ou trocar de aparelho, instale a nova versão e use **Restaurar backup** para importar o JSON. A importação substitui os dados atuais do app pelos dados do arquivo; exporte um backup recente antes de importar.

## Abrir a versão web

É necessário ter Node.js instalado; não há pacote npm para instalar.

1. No Windows, dê duplo clique em `start-pokeroutine.bat`.
2. Mantenha a janela do servidor aberta.
3. Abra <http://localhost:8765> no navegador do computador.
4. Para parar o servidor, feche a janela ou pressione `Ctrl+C`.

Para testar no Motorola pela mesma rede Wi-Fi, execute `start-pokeroutine-lan.bat`, permita o Node.js somente em redes privadas se o Windows perguntar e abra no Chrome do celular o endereço de rede mostrado no terminal. Os dados criados nessa página ficam no armazenamento do navegador do celular; eles não são os dados do APK.

A versão web/PWA é útil para testar a interface, mas não é um APK. Notificações locais agendadas e o ícone do launcher Android devem ser conferidos na versão Android.

## Gerar e baixar o APK pelo GitHub Actions

O repositório é privado; é necessário entrar com uma conta autorizada para ver o código e os artefatos. O workflow manual está em `.github/workflows/android-debug.yml`.

1. Abra o repositório **Pokeroutine** no GitHub e selecione a aba **Actions**.
2. Escolha **PokéRoutine Android debug APK** na lista de workflows.
3. Clique em **Run workflow**, selecione a branch `main` e confirme a execução.
4. Aguarde o job terminar. Só considere válido quando a execução estiver verde (**Success**); se estiver vermelha, abra os logs para ver a etapa que falhou.
5. Na página dessa execução bem-sucedida, localize **Artifacts** e baixe **Pokeroutine-debug-apk**.
6. Extraia o ZIP. O APK dentro dele se chama `app-debug.apk`.

O artefato fica disponível por **30 dias** após a execução. O workflow roda testes, prepara os arquivos web e os sprites locais dentro do pacote Android, compila o módulo `:app` em Linux e verifica a assinatura e recursos esperados antes de publicar o artefato. Ele usa Java 17, Gradle Wrapper 8.9, Android Gradle Plugin 8.7.3, Android SDK 35 e Build Tools 35.0.0. A compilação não usa o AAPT do Windows.

## Instalar ou atualizar 

1. Transfira `app-debug.apk` para o telefone por USB ou baixe-o e extraia-o no próprio aparelho.
2. Abra o APK usando **Arquivos**. Se o Android pedir, permita temporariamente que o app usado para abrir o arquivo instale apps desconhecidos; o nome/caminho da opção varia conforme a versão do Android.
3. Confirme **Instalar** e abra o PokéRoutine.
4. Permita notificações quando o Android perguntar, se quiser receber lembretes.

Instalar por cima costuma preservar os dados quando o APK tem a mesma assinatura da versão já instalada. O primeiro APK assinado no GitHub Actions pode ter uma assinatura diferente da versão de depuração que foi compilada no Windows; nesse caso, o Android pode impedir a atualização. **Antes de desinstalar a versão antiga, exporte um backup JSON.** Desinstalar apaga os dados locais do app. Depois de instalar o APK novo, restaure o backup.

As execuções do workflow reutilizam uma chave de depuração em cache para permitir atualizações entre builds enquanto esse cache existir. Se o cache expirar ou for removido, a assinatura pode mudar; guarde um backup antes de atualizar caso haja dúvida.

## Lembretes e som no Android

O Android agenda os lembretes localmente, então eles não dependem de o app ficar aberto. No Android 13 ou superior, autorize notificações. Em versões recentes, o Android também pode pedir acesso a alarmes exatos; sem esse acesso, o comportamento depende das permissões e das restrições da versão do aparelho. O receptor do app restaura os lembretes salvos após a reinicialização do telefone.

O canal de notificações respeita o volume, o modo silencioso e as configurações do Android. Se uma notificação chegar sem som, verifique **Configurações do Android → Apps → PokéRoutine → Notificações** e confira o som do canal. A preferência de som do aplicativo também pode ser desligada nas configurações do próprio PokéRoutine. Economia de bateria e configurações do fabricante podem atrasar alarmes; isso varia por aparelho.

## Estrutura do projeto

- `index.html`, `styles.css`, `app.js`: versão web e interface embarcada no WebView Android.
- `assets/fonts`: Pixelify Sans e Press Start 2P, com licenças SIL OFL.
- `assets/pokemon`: sprites locais usados pelo app; a atribuição está em `pokemon/LICENCE-PokeAPI.txt`.
- `assets/pokeball-commons.svg` e `assets/ATTRIBUTION-pokeball.txt`: asset e atribuição/licença da Poké Ball do Wikimedia Commons. Os ícones PWA e Android usam uma adaptação pixel art documentada no mesmo arquivo.
- `android/app/src/main/assets`: cópia dos arquivos web e assets que o app Android carrega offline. O workflow sincroniza essa pasta com a versão raiz antes de compilar.
- `android/app/src/main/java`: Activity WebView, ponte JavaScript, notificações e backup Android.
- `android/app/src/main/res/mipmap-*`: ícones de fallback para o launcher. `mipmap-anydpi-v26` contém os ícones adaptativos.
- `android/app/src/main/res/raw/pokeroutine_alert.wav`: efeito curto original de 8-bit, sem áudio extraído dos jogos Pokémon.
- `tests/pokemon-system.test.cjs`: testes de catálogo, sprites, seleção, XP, evolução e persistência.

## Créditos dos assets

- Sprites de Pokémon: [PokeAPI/sprites](https://github.com/PokeAPI/sprites); consulte `pokemon/LICENCE-PokeAPI.txt` para o aviso e a atribuição mantidos no projeto. A disponibilidade desses sprites não concede direitos sobre Pokémon.
- Ícone da Poké Ball: [Andreuvv, Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Pok%C3%A9_Ball_icon.svg), sob [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/). O projeto adiciona fundo preto e adapta o desenho para pixel art; os detalhes estão em `assets/ATTRIBUTION-pokeball.txt`.
- Fontes: arquivos e textos de licença SIL OFL estão juntos em `assets/fonts`.

## Testes locais

Na pasta do projeto, execute:

```powershell
node --check .\app.js
node --check .\sw.js
node --test .\tests\pokemon-system.test.cjs
```

Para compilar manualmente, é preciso Java 17 e Android SDK 35. No PowerShell, entre em `android` e execute `..\gradlew.bat --no-daemon :app:assembleDebug`. O arquivo local, se a compilação terminar com sucesso, sai em `android\app\build\outputs\apk\debug\app-debug.apk`. A rota recomendada para este projeto é o workflow Linux descrito acima.
