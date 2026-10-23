# Wellness — treinos no Android

Aplicativo em português para registrar os treinos da **ACADEMIAWELLNESS.xlsx**, sem conta e sem internet. Feito em Kotlin e Jetpack Compose. Requer Android 8.0 ou mais recente.

## Baixar e instalar sem programar

[Baixar o APK Wellness-teste.apk](https://github.com/suporte26mgptelecom-cyber/esposaapp/raw/refs/heads/main/instalar/Wellness-teste.apk)

1. Baixe o APK pelo link acima. Na página do arquivo no GitHub, também pode usar **Download raw** (seta de download).
2. No celular Android 8 ou superior, abra a pasta **Downloads** e toque em `Wellness-teste.apk`.
3. Autorize a instalação pelo navegador ou gerenciador de arquivos quando o Android pedir.
4. Abra **Wellness** e toque em **Começar treino**.

Para baixar a pasta inteira pelo GitHub, use **Code → Download ZIP**. Extraia o ZIP e entre em **instalar**: o APK está nessa pasta. O restante das pastas contém o código do aplicativo, caso queira abrir no Android Studio.

## Entrega e resultado

O APK de teste foi compilado, assinado e verificado nesta nuvem. Os 10 testes locais passaram (incluem 4.158 casos de cálculo), e a análise Android terminou sem avisos. **O teste de interface ficou bloqueado pelo emulador sem aceleração da nuvem; o aplicativo ainda precisa ser aberto e conferido em um celular ou emulador do Android Studio.** Os detalhes estão em `docs/VALIDACAO.md`.

O projeto inclui o código completo e um APK pronto na pasta `instalar`. Para abrir em outro computador, baixe **Code → Download ZIP**, extraia e selecione a pasta extraída no Android Studio. SDK, caches, registros pessoais e chaves de assinatura não são incluídos.

## Usar no celular

1. Instale o APK de teste seguindo as instruções abaixo.
2. Toque em **Começar treino**. A tela inicial mostra o próximo treino e quantos dos 24 já foram concluídos.
3. Confira o aparelho em **Aparelho e carga**. Digite a **carga de trabalho**; as cargas de cada série serão calculadas conforme a fase da planilha.
4. Digite as **Reps feitas** e marque a caixa quando terminar a série. Se a carga usada for diferente da calculada, preencha **Kg usados** naquela série. Se deixar esse campo vazio, o registro utilizará a carga calculada.
5. Use **Definir descanso** para escolher um tempo. Depois, o botão inicia esse descanso. O temporizador permite pausar e continuar.
6. Toque em **Concluir sessão** para guardar uma cópia no histórico. É possível concluir com campos vazios ou séries não marcadas; a confirmação mostra quantas séries foram feitas. Os dados não preenchidos continuam vazios.

Os registros são salvos automaticamente. Espere a indicação **Salvando…** desaparecer antes de encerrar o aplicativo à força. Fechar e abrir normalmente preserva os dados. Desinstalar o aplicativo ou limpar seus dados apaga os registros: faça backup antes.

### Valores compartilhados e histórico

- Carga de trabalho e repetições são compartilhadas em todas as 24 sessões pelo mesmo **exercício + equipamento + forma de registrar + identificação da variação**.
- As repetições correspondem pelo **tipo e número**: `Working Set 1` só atualiza `Working Set 1`. Uma série que não existe naquela fase não é criada.
- Marcações de série feita e **Kg usados** pertencem somente à sessão atual. Não são compartilhadas.
- Para distinguir dois aparelhos ou variações, dê nomes diferentes em **Identificação / variação**. Trocar o perfil não transfere suas cargas. Voltar ao perfil anterior recupera os valores dele.
- Alterar o perfil vale nas ocorrências com a mesma configuração original do exercício. As configurações diferentes já presentes na planilha permanecem separadas.
- A tela de treino sempre mostra os valores compartilhados atuais. Para ver exatamente o que foi usado em uma conclusão, abra **Histórico**. Ele permanece intacto após edições posteriores.
- **Registrar novamente** limpa apenas as marcações e cargas específicas daquela sessão. Uma nova conclusão acrescenta outro registro; o progresso conta sessões distintas.

Em **Ajustes**, use **Exportar backup** para criar um arquivo JSON. Guarde-o fora do celular. **Importar backup** valida o arquivo e pede confirmação: a restauração substitui todos os registros atuais, sem misturar históricos. Um arquivo inválido não apaga seus dados. Limite por arquivo: 16 MB.

Os vídeos abrem no navegador ou YouTube e precisam de internet. O treino, os cálculos, o histórico e o backup funcionam offline. O temporizador preserva seu horário de término ao fechar a tela; o aviso sonoro ocorre com o aplicativo aberto. Não há alarme de segundo plano.

## Gerar o APK no Android Studio (caminho mais fácil)

1. Instale o **Android Studio** pelo site oficial: <https://developer.android.com/studio>.
2. Abra o Android Studio, escolha **Open** e selecione a pasta `esposaapp` (onde está `settings.gradle.kts`).
3. Deixe o Studio baixar as dependências. É preciso internet nessa primeira preparação.
4. Em **Settings → Build, Execution, Deployment → Build Tools → Gradle**, selecione **Gradle JDK 17**. O projeto usa Gradle 8.10.2, plugin Android 8.7.3 e Kotlin 2.0.21.
5. Em **SDK Manager**, instale **Android SDK Platform 35** e **Android SDK Build-Tools 35.0.0**. O Studio normalmente oferece isso automaticamente.
6. Para abrir diretamente no celular, ative a depuração USB nas opções de desenvolvedor do Android, conecte-o e clique no botão verde **Run ▶**. Também pode criar um aparelho virtual pelo **Device Manager**.
7. Para gerar um arquivo instalável, use **Build → Build App Bundle(s) / APK(s) → Build APK(s)** (o nome pode variar conforme a versão do Studio).
8. O arquivo estará em **`app/build/outputs/apk/debug/app-debug.apk`**.

### Instalar o arquivo APK

Transfira `app-debug.apk` para o celular, abra-o no gerenciador de arquivos e autorize a instalação por esse aplicativo quando o Android pedir. Instale apenas o arquivo que você gerou ou recebeu de uma fonte de confiança. Esse APK é de teste; não precisa de Play Store nem de conta.

Para atualizar mantendo os dados, instale por cima com a **mesma assinatura**. APKs gerados em computadores diferentes podem ter assinaturas de teste diferentes. Faça backup antes de desinstalar para trocar a assinatura. Para publicar na Play Store, será necessário configurar uma chave de assinatura de produção separadamente.

## Compilar pelo terminal

Com JDK 17 e SDK Android instalados, configure `ANDROID_HOME` ou deixe o Android Studio criar o arquivo local `local.properties`. Não envie esse arquivo para o Git.

Linux/macOS:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Windows (PowerShell):

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

O APK fica no mesmo caminho indicado acima. Com um aparelho conectado e autorizado, `adb install -r app/build/outputs/apk/debug/app-debug.apk` instala a versão de teste.

Na máquina Linux desta nuvem, a preparação reproduzível está em `tools/cloud_setup.sh`. Ela instala JDK e SDK em `/workspace`, confere os downloads e executa build, testes e análise Android. Execute:

```sh
bash tools/cloud_setup.sh
source /workspace/toolchains/wellness-env.sh
```

O script da nuvem é específico de Linux e não é necessário no Android Studio. Nenhuma chave ou credencial é necessária. Depois da instalação das dependências, as tarefas Gradle podem ser executadas com `--offline`.

## Planilha e cálculos

O arquivo original está em [docs/ACADEMIAWELLNESS.xlsx](docs/ACADEMIAWELLNESS.xlsx). A leitura detalhada de todas as sessões e configurações está em [docs/PLANILHA.md](docs/PLANILHA.md). O JSON embarcado em `app/src/main/assets/wellness.json` foi extraído automaticamente; não é um treino inventado.

São 8 semanas, 24 sessões, 130 ocorrências de exercícios e 594 séries. A1 é segunda, A2 quarta e A3 sexta. A sequência original alterna pernas/glúteos e superiores, inclusive na passagem entre semanas.

- Aquecimento: 30% da carga de trabalho, conforme a planilha.
- Feeder sets: os fatores de **cada linha** da planilha, incluindo 50%, 70% e 85%, conforme a quantidade prescrita.
- Working sets: 100%.
- Top sets: 110%, com a exceção de 108% no terra sumô da fase 3–4 (sessão S4 A2).
- Back offs: 75% da **carga de trabalho**, mesmo após top set.
- Incremento vazio ou zero: cálculo exato. Incremento positivo: arredondar para baixo por série. Não há divisão automática por lados nem desconto de barra.
- Nenhum aumento automático entre treinos. O valor zero é preservado; campo vazio não vira zero.
- A única carga inicial preenchida é 5 kg na cadeira abdutora. Nenhum descanso ou outra carga foi presumido.

As adaptações aprovadas já estavam no arquivo e foram preservadas: hack squat, leg press unilateral, flexora sentada, elevação pélvica e retirada do step up. As alternativas e observações da planilha também aparecem no aplicativo.

Há configurações incomuns **preservadas** na S1 A2: remada articulada unilateral como **Polia** e desenvolvimento na máquina articulada como **Halter**. As demais ocorrências usam **Máquina**. Por isso, esses valores não se misturam automaticamente. Ajuste o perfil no aplicativo se quiser refletir outro aparelho real.

## Estrutura e testes

- `domain/`: plano, cálculo decimal, sincronização por identidade de série e cópias do histórico.
- `data/`: JSON versionado e gravação local com sincronização em disco e troca atômica do arquivo. Importação validada antes de substituir os dados.
- `ui/`: telas Compose e fila de salvamento em segundo plano, sem rede ou conta.
- `tools/extract_workbook.py`: extrai todas as abas e rejeita fórmulas desconhecidas.
- `tools/generate_formula_cases.py`: gera 4.158 casos independentes a partir das 594 fórmulas originais.

Para refazer a extração, instale `tools/requirements.txt` em um ambiente Python e execute os dois scripts. Python não é necessário para usar nem compilar o aplicativo.

Testes locais: `./gradlew :app:testDebugUnitTest`. Exercitam todas as fórmulas, sincronização entre todas as ocorrências, variações e equipamentos separados, vazio versus zero, persistência em arquivo, histórico imutável, backups inválidos e temporizador.

Teste em dispositivo/emulador: `./gradlew :app:connectedDebugAndroidTest`. Ele abre a interface, altera uma carga, marca uma série, fecha e reabre a Activity, verifica a persistência e confere o compartilhamento em outra sessão. Use um dispositivo de teste: esse teste apaga os registros do aplicativo no dispositivo selecionado.

Consulte [docs/VALIDACAO.md](docs/VALIDACAO.md) para os resultados realmente executados nesta entrega e suas limitações.
