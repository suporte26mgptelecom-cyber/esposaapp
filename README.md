# Wellness — treinos no Android

Aplicativo em português para registrar os treinos da **ACADEMIAWELLNESS.xlsx**, sem conta e sem internet. Feito em Kotlin e Jetpack Compose. Requer Android 8.0 ou mais recente.

## Atualização 1.2 — exercícios, dicas e descansos

Cabeçalho centralizado **WELLNESS · Seu treino com segurança**, boas-vindas **Bem-vinda, Nanda** e nova aba **Exercícios**, com busca entre os 19 exercícios e variações do plano. O tema continua preto, branco e vermelho, independentemente do tema do celular. Progresso, próximo treino, semanas e cálculos foram preservados.

Os botões vermelhos **Buscar vídeo no TikTok** abrem a busca “como fazer [nome do exercício]”. Tentam o TikTok e, se ele não estiver disponível ou recusar o link, abrem a mesma pesquisa em um navegador. Não há vídeo fixo. As dicas aparecem antes das séries; as notas de cálculo ficam em **Sobre o cálculo**, expansível. As fontes efetivamente consultadas e a limitação de acesso às fontes institucionais estão em [docs/DICAS-E-FONTES.md](docs/DICAS-E-FONTES.md).

A carga de trabalho continua editável e cada série mostra a carga calculada, **Reps feitas** e a marcação de feita. O campo editável **Kg usados** saiu da tela: seus valores antigos, inclusive zero, continuam no arquivo, nos backups e nos registros de conclusão. Os descansos ajustados são salvos por exercício + perfil + tipo/número da série, em todas as semanas. O descanso entre exercícios tem preferência separada. O histórico já concluído permanece imutável.

Esta versão usa `applicationId=br.com.wellness`, `versionCode=3` e `versionName=1.2`. A mesma chave de assinatura do APK 1.1 está disponível e é usada na atualização. O arquivo local mantém o nome `wellness-v1.json`; a migração aditiva do conteúdo de formato 1 para 2 não apaga dados e aceita backups antigos. A importação por escolha do usuário continua sendo restauração completa, com confirmação.

Antes de atualizar, exporte um backup em **Ajustes** por precaução. Abra o novo APK e escolha **Atualizar**, instalando por cima. Não desinstale nem limpe os dados. Caso o Android recuse a assinatura, pare e use a chave da instalação anterior; não tente resolver desinstalando.

A paleta está centralizada em `app/src/main/res/values/colors.xml` e aplicada por `ui/theme/WellnessTheme.kt`. O teclado numérico é o teclado do próprio celular.

## Baixar e instalar sem programar

[Baixar o APK Wellness-teste.apk](https://github.com/suporte26mgptelecom-cyber/esposaapp/raw/refs/heads/main/instalar/Wellness-teste.apk)

1. Baixe o APK pelo link acima. Na página do arquivo no GitHub, também pode usar **Download raw** (seta de download).
2. No celular Android 8 ou superior, abra a pasta **Downloads** e toque em `Wellness-teste.apk`.
3. Autorize a instalação pelo navegador ou gerenciador de arquivos quando o Android pedir.
4. Abra **Wellness** e toque em **Começar treino**.

Para baixar a pasta inteira pelo GitHub, use **Code → Download ZIP**. Extraia o ZIP e entre em **instalar**: o APK está nessa pasta. O restante das pastas contém o código do aplicativo, caso queira abrir no Android Studio.

## Entrega e resultado

O projeto completo e o APK ficam no GitHub, com o instalador na pasta `instalar`. SDK, caches, registros pessoais e chaves de assinatura não entram no repositório. Os resultados da compilação, dos testes e suas limitações estão em [docs/VALIDACAO.md](docs/VALIDACAO.md).

## Usar no celular

1. Instale o APK de teste seguindo as instruções abaixo.
2. Toque em **Começar treino**. A tela inicial mostra o próximo treino e quantos dos 24 já foram concluídos.
3. Confira o aparelho em **Aparelho e carga**. Digite a **carga de trabalho**; as cargas de cada série serão calculadas conforme a fase da planilha.
4. Digite as **Reps feitas** e marque a caixa quando terminar a série. Confira a carga calculada. A marcação não inicia descanso automaticamente.
5. Toque em **Descansar** abaixo da série, ou em **Ajustar descanso** para editar minutos e segundos e salvar/iniciar. A faixa fixa no topo permite pausar, continuar e encerrar. Outro descanso exige confirmação antes de substituir o atual. Ao final do exercício há um descanso próprio para o próximo exercício; ele não avança o treino.
6. Toque em **Concluir sessão** para guardar uma cópia no histórico. É possível concluir com campos vazios ou séries não marcadas; a confirmação mostra quantas séries foram feitas. Os dados não preenchidos continuam vazios.

Os registros são salvos automaticamente. Espere a indicação **Salvando…** desaparecer antes de encerrar o aplicativo à força. Fechar e abrir normalmente preserva os dados. Desinstalar o aplicativo ou limpar seus dados apaga os registros: faça backup antes.

### Valores compartilhados e histórico

- Carga de trabalho e repetições são compartilhadas em todas as 24 sessões pelo mesmo **exercício + equipamento + forma de registrar + identificação da variação**.
- As repetições correspondem pelo **tipo e número**: `Working Set 1` só atualiza `Working Set 1`. Uma série que não existe naquela fase não é criada.
- Marcações de série feita e registros antigos de **Kg usados** pertencem somente à sessão atual. Não são compartilhados. O campo de kg por série não é mais editável na interface.
- Para distinguir dois aparelhos ou variações, dê nomes diferentes em **Identificação / variação**. Trocar o perfil não transfere suas cargas. Voltar ao perfil anterior recupera os valores dele.
- Alterar o perfil vale nas ocorrências com a mesma configuração original do exercício. As configurações diferentes já presentes na planilha permanecem separadas.
- A tela de treino sempre mostra os valores compartilhados atuais. Para ver exatamente o que foi usado em uma conclusão, abra **Histórico**. Ele permanece intacto após edições posteriores.
- **Registrar novamente** limpa apenas as marcações e cargas específicas daquela sessão. Uma nova conclusão acrescenta outro registro; o progresso conta sessões distintas.

Em **Ajustes**, use **Exportar backup** para criar um arquivo JSON. Guarde-o fora do celular. **Importar backup** valida o arquivo e pede confirmação: a restauração substitui todos os registros atuais, sem misturar históricos. Um arquivo inválido não apaga seus dados. Limite por arquivo: 16 MB.

Os vídeos precisam de internet. O treino, os cálculos, o histórico e o backup funcionam offline.

### Descanso e avisos

Os tempos padrão foram solicitados nesta atualização: aquecimento **0:40**, feeder **1:30**, working/top/back off **3:00**, rest pause **0:20** (caso esse tipo exista no plano). Nenhuma série rest pause foi acrescentada à planilha. Entre exercícios: **3:00**. Um descanso entre exercícios já configurado na versão antiga é reaproveitado; valores legados fracionários permanecem armazenados, mas a nova escolha de minutos/segundos usa segundos inteiros.

A contagem fica acima da lista do treino e usa o horário de término, mesmo ao minimizar, bloquear a tela ou encerrar o processo normalmente. O alarme nativo permite avisar com o app em segundo plano. O Android pede permissão de notificações a partir da versão 13. Em **Ajustes**, **Configurar notificações, som e vibração** abre o canal do aparelho: volume, som, vibração, Não perturbe e bloqueios do sistema são respeitados. **Permitir alarmes precisos** abre o ajuste Android 12 ou superior. Sem essa autorização, o aviso pode atrasar na economia de bateria; a contagem ao reabrir continua correta. Se notificações estiverem bloqueadas, haverá aviso visual no treino. Não há retomada automática do alarme após reiniciar o celular ou forçar a parada do aplicativo pelas configurações do Android; ao abrir o app, o horário salvo volta a ser conferido.

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

Para atualizar mantendo os dados, instale por cima com a **mesma assinatura**. APKs gerados em computadores diferentes podem ter assinaturas de teste diferentes. Se o Android avisar que a assinatura não coincide, **não desinstale nem limpe os dados**: será necessário gerar o APK com a chave anterior. A chave usada nesta nuvem está em `/workspace/.android/debug.keystore`, fora do Git; preserve-a para futuras atualizações. Para publicar na Play Store, será necessário planejar a assinatura de produção separadamente.

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
- A única carga inicial preenchida é 5 kg na cadeira abdutora. Nenhuma outra carga foi presumida. A planilha não traz descanso; os padrões da versão 1.2 são a adaptação explicitamente solicitada pelo usuário.

As adaptações aprovadas já estavam no arquivo e foram preservadas: hack squat, leg press unilateral, flexora sentada, elevação pélvica e retirada do step up. As alternativas e observações da planilha também aparecem no aplicativo.

Há configurações incomuns **preservadas** na S1 A2: remada articulada unilateral como **Polia** e desenvolvimento na máquina articulada como **Halter**. As demais ocorrências usam **Máquina**. Por isso, esses valores não se misturam automaticamente. Ajuste o perfil no aplicativo se quiser refletir outro aparelho real.

## Estrutura e testes

- `domain/`: plano, cálculo decimal, sincronização por identidade de série e cópias do histórico.
- `data/`: JSON versionado e gravação local com sincronização em disco e troca atômica do arquivo. Importação validada antes de substituir os dados.
- `ui/`: telas Compose e fila de salvamento em segundo plano, sem rede ou conta.
- `tools/extract_workbook.py`: extrai todas as abas e rejeita fórmulas desconhecidas.
- `tools/generate_formula_cases.py`: gera 4.158 casos independentes a partir das 594 fórmulas originais.

Para refazer a extração, instale `tools/requirements.txt` em um ambiente Python e execute os dois scripts. Python não é necessário para usar nem compilar o aplicativo.

Testes locais: `./gradlew :app:testDebugUnitTest`. Incluem fórmulas, sincronização, perfis separados, vazio versus zero, persistência, histórico imutável, migração do formato antigo, backups, tempos de descanso e rotas de vídeo. Há testes de interface e plataforma com Robolectric (Android simulado na JVM, sem KVM); a primeira execução baixa também esse Android de teste do Maven Central.

Teste em dispositivo/emulador: `./gradlew :app:connectedDebugAndroidTest`. Ele abre a interface, altera uma carga, marca uma série, fecha e reabre a Activity, verifica a persistência e confere o compartilhamento em outra sessão. Use um dispositivo de teste: esse teste apaga os registros do aplicativo no dispositivo selecionado.

Consulte [docs/VALIDACAO.md](docs/VALIDACAO.md) para os resultados realmente executados nesta entrega e suas limitações.
