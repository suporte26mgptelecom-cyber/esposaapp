# Validação do aplicativo

Planilha oficial: `ACADEMIAWELLNESS.xlsx`, SHA-256 `59cd316986fdf8c8619ff1003961b192a65166a8bb3b9e20baed016e6725cb1a`.

## Atualização 1.4 — vídeos TikTok escolhidos (24/10/2026)

Resultado final: **40 testes aprovados, zero falhas/erros/ignorados**, `assembleDebug` e `assembleDebugAndroidTest` concluídos e lint **“No issues found”**. Comando executado:

```sh
source /workspace/toolchains/wellness-env.sh
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --console=plain
```

Distribuição: WorkoutTest 10, UpdateTest 7, RankingTest 9, PlatformTest 2, InterfaceTest 1, NavigationV13Test 1, TikTokVideosTest 8 e TikTokUiTest 2. Os testes de busca TikTok antigos foram substituídos pelos testes dos vídeos escolhidos, pois a solicitação mais recente substituiu a busca por links específicos. As 4.158 verificações das fórmulas continuam passando.

### Links: mapeamento versus acesso ao TikTok

- O teste confronta independentemente os **19 IDs/links exatos** fornecidos, incluindo maiúsculas dos códigos e barra final, verifica 18 URLs únicas e todos os **130 exercícios das 24 sessões**, além dos 19 itens do catálogo. As duas elevações sentadas permanecem registros separados com o mesmo vídeo TikTok e seus próprios links YouTube.
- Foram tentadas requisições GET com redirecionamentos aos **18 links únicos**. Todas falharam no proxy da nuvem: **CONNECT HTTP 403**, antes de uma resposta do TikTok. **Zero endereços completos obtidos; zero vídeos verificados como disponíveis/reproduzíveis.** Os originais foram preservados exatamente e `resolvedUrl=null` em todas as entradas. Isso não identifica links inválidos nem vídeos fora do ar. Evidências por URL/data/hora estão em [TIKTOK-RESOLUCAO.json](TIKTOK-RESOLUCAO.json), e a tabela legível em [TIKTOK-VIDEOS.md](TIKTOK-VIDEOS.md).
- O aplicativo está preparado para guardar um endereço completo verificado sem apagar o original. Um teste usa uma URL longa **sintética, apenas no código de testes**, para conferir prioridade do endereço completo e nova tentativa com o original. Essa URL de exemplo não integra os vídeos do APK e não é apresentada como resolução de qualquer link escolhido.

### Abertura: o que foi simulado

- A tentativa inicial envia **somente a URL do vídeo** por ACTION_VIEW com o pacote TikTok explicitamente definido. Não envia nome do exercício, Google, busca TikTok, link de página inicial ou intent implícito. Todos os 19 destinos são conferidos nos testes de transporte. O resultado é chamado `SENT_UNCONFIRMED`, sem afirmar que o vídeo foi reproduzido.
- Quando o TikTok é simulado como ausente/recusando o endereço, a tentativa termina sem abrir navegador, sem copiar e sem iniciar outro aplicativo. A presença de um navegador não muda esse comportamento.
- **Abrir no navegador** só inicia um navegador depois do toque específico, passando o mesmo vídeo (completo se disponível, senão original). Não faz pesquisa. Sem navegador disponível, retorna falha e mantém a opção de cópia.
- **Copiar link** só escreve no clipboard depois do toque, copia exatamente o endereço original e não abre aplicativo algum. Tentativas normais de TikTok e cancelamento preservam o clipboard anterior.
- Dois testes de interface executam as telas Compose com Robolectric/Android 14: botão na aba Exercícios filtrada por `sumo`, botão no treino correspondente, URL correta, opções de navegador/cópia, cópia apenas após toque, navegador apenas após toque, fechamento e comparação byte a byte do arquivo de registros antes/depois. A aceitação dos intents nesses testes é simulada; **não existe um TikTok real como receptor**.
- Os testes anteriores também passam: ranking sem duplicação, conclusões antigas, comemoração sem repetição, migração/importação dos formatos anteriores, perfis de equipamento, vazio versus zero, persistência, cronômetro, histórico imutável, navegação, fonte em 160%, finalização e YouTube.

### Atualização e dados

O APK é **1.4 / versionCode 5**, antes 1.3 / 4, com `applicationId=br.com.wellness`. `apksigner verify --print-certs` compara os dois APKs e confirma o mesmo certificado SHA-256 `f45a0d175aed2396cba54737eee94ee8b96fda1190f9a8c5d95619687a7566d5`. O formato dos registros permanece **3** e o caminho `wellness-v1.json` continua igual. Não há nova migração ou restauração de valores iniciais.

`Models.kt`, `WorkoutEngine.kt`, `Storage.kt`, a planilha XLSX e `assets/wellness.json` foram comparados com a versão 1.3 e são idênticos. As URLs TikTok ficam somente no novo asset `tiktok-videos.json`, sem reescrever histórico, cargas, reps, equipamentos, tempos, ranking ou preferências. O JSON oficial embarcado no APK foi comparado com o anterior: continua idêntico, incluindo os links YouTube.

### Limitações

Não há dispositivo conectado (`adb devices` sem aparelhos) nem `/dev/kvm`; não foi iniciado o emulador de sistema completo que falhou nas entregas anteriores. Não houve instalação sobre um celular físico, abertura no TikTok real, confirmação da tela exibida por esse aplicativo ou reprodução dos vídeos. O APK de instrumentação foi compilado, mas não executado em aparelho. A rede bloqueou todos os links curtos. **Validação dos parâmetros enviados e das telas simuladas não equivale a validação de um link ao vivo nem de sua abertura no TikTok.**

A assinatura, a versão e a preservação dos dados foram conferidas; os dados pessoais do celular do usuário não foram acessados. Para atualizar, exporte um backup e instale o APK por cima escolhendo Atualizar, sem desinstalar nem limpar os dados. Checksum e metadados ficam em `instalar/SHA256SUMS.txt` e `instalar/versao.json`.

## Registros históricos das entregas anteriores

## Atualização 1.3 — resultados de 24/10/2026

**Resultado final: 32 testes aprovados, zero falhas/erros/ignorados; APK 1.3 compilado; lint “No issues found”.**

Comando executado com JDK 17 e SDK 35 já preparados:

```sh
source /workspace/toolchains/wellness-env.sh
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --console=plain
```

A execução final terminou **BUILD SUCCESSFUL**. Distribuição: `WorkoutTest` 10, `UpdateTest` 8, `RankingTest` 9, `PlatformTest` 3 e dois testes de interface (`InterfaceTest` e `NavigationV13Test`). Os testes originais continuam exercitando **4.158 casos das fórmulas** e a sincronização das ocorrências nas 24 sessões. As APIs simuladas do Robolectric geram advertências de depreciação na compilação dos testes; o lint do aplicativo terminou com zero problemas.

### O que foi verificado

- **Cabeçalho e organização:** WELLNESS centralizado, imagem do W existente, ausência da frase retirada, boas-vindas mantida, inicial sem lista de semanas e botão fixo “Ver treinos”. A lista mantém oito semanas/24 sessões na sequência do plano. Títulos são convertidos para maiúsculas somente na apresentação, em português, com acentos; nenhum identificador, título armazenado ou plano oficial foi alterado.
- **Estados de sessão:** pendente, iniciada e concluída derivados dos registros, sem usar datas. A abertura de uma sessão passa a registrar seu início. Marcações e kg específicos antigos também reconhecem treino em andamento; cargas/reps compartilhadas não são usadas para inferir uma sessão. Conclusões antigas dão prioridade ao estado concluído; repetir/registrar novamente não apaga a conclusão anterior. Cards concluídos usam o verde centralizado `#1B5E20`, branco e ✓; os demais continuam no tema preto/branco/vermelho.
- **Ranking:** todos os valores de 0 a 24 foram verificados, cobrindo cada marco e os intervalos. Cinco conclusões geram a frase exata “2 de 3 treinos — falta 1 para desbloquear Voltei por Milagre”. A 24ª mostra o último rank e todos conquistados. Dez conclusões da mesma sessão contam uma vez; 24 sessões em ordem invertida e com a mesma data dão todos os ranks. Não há dependência de carga, velocidade ou descanso.
- **Comemoração:** o maior rank já anunciado é persistido na mesma gravação antes da exibição. Reinício/reabertura e backup preservam o reconhecimento; só um novo marco gera outra comemoração. Conclusões antigas da versão 1.2 são aproveitadas. A primeira abertura/importação pode anunciar uma vez o maior rank ainda não reconhecido; não reproduz uma fila de comemorações antigas.
- **Histórico:** o novo caminho Ranking → Ver histórico abre os registros antigos. As cópias de cargas, reps, zero explícito e timestamps permanecem iguais após alterações compartilhadas e novos descansos. Novas conclusões registram o descanso de transição realmente apresentado na última série; a série final do último exercício não registra um descanso fictício. Snapshots antigos não são recalculados.
- **Vídeos:** todas as 130 ocorrências tiveram seu link YouTube comparado aos dados oficiais. As 19 variações mantêm seus links; as elevações laterais sentada e sentada (90º) continuam distintas. Links vazios/inválidos não recebem um vídeo inventado. Testes Android simulados verificam preferência pelo aplicativo YouTube, fallback ao navegador com a URL original idêntica, nenhuma abertura sem link, TikTok com a busca correta e fallback com o mesmo texto codificado. As duas opções têm ícones, rótulos e descrição de acessibilidade específica por exercício. A origem dos ícones está em [ICONES.md](ICONES.md).
- **Descansos:** todos os 594 pontos de série foram conferidos: 464 controles de série intermediária, 106 transições entre exercícios e 24 ações finais. Não há descanso da última série duplicado com a transição, nem descanso para um exercício após o último. Os tempos de série antigos continuam na preferência mesmo quando o controle fica oculto. A transição reaproveita sua própria preferência salva, usando 3:00 somente se não existir. A faixa fixa, pausa/retomada, confirmação de substituição e prazo de término continuam testados.
- **Fonte maior:** a navegação foi executada com Android 14 simulado, tela de 393 × 852 dp e tamanho de fonte do sistema em **160%**. O teste verifica centro do título/logo, ranking com cinco conclusões, histórico, estados concluído/em andamento, botões de vídeo, rótulos de descanso sem overflow ou quebra no meio de palavras, disposição vertical quando necessária, inexistência de descanso após o exercício final, finalização da sexta sessão, card concluído, novo rank e reabertura por um ViewModel novo sem repetir a comemoração.
- **Interface padrão:** outro teste executa o app em uma tela menor, abre Exercícios, busca `sumo`, confere os dois botões, passa por Ranking/Histórico/Ajustes/Ver treinos, abre Aparelho e carga, separa dica/cálculo, ajusta/inicia/pausa/substitui/retoma/encerra o cronômetro e confere preservação dos dados. O diálogo de conclusão tem texto rolável para acomodar fontes maiores.
- **Migração/backup:** o mesmo arquivo `wellness-v1.json` permanece. Formatos 1 e 2 migram aditivamente para **3**, acrescentando `startedSessions` e `celebratedRank`. Foram verificados leitura sem sobrescrever o arquivo antigo, preservação de cargas/reps/equipamentos/tempos/marcas/zero/histórico/cronômetro, gravação/reabertura, restauração e rejeição de preferências inválidas sem substituir os dados anteriores. As novas preferências entram no backup.
- **Identidade e assinatura:** `aapt dump badging` confirmou `applicationId=br.com.wellness`, `versionCode=4` (antes 3), `versionName=1.3`. `apksigner verify --print-certs` confirmou a mesma assinatura do APK 1.2, SHA-256 **`f45a0d175aed2396cba54737eee94ee8b96fda1190f9a8c5d95619687a7566d5`**. A chave está disponível e não entra no Git/ZIP.
- **Fonte oficial:** a planilha XLSX, `assets/wellness.json` e o desenho `ic_wellness.xml` foram comparados byte a byte com o commit 1.2; são idênticos. O plano dentro do APK também permanece igual. Não foram alteradas fórmulas, equipamento, perfil de registro, arredondamento ou dados da prescrição.

### Limites desta entrega

A execução de interface foi com **Robolectric**, não com um Android de sistema completo nem celular físico. O ambiente continua sem `/dev/kvm`; o emulador instável da entrega inicial não foi reiniciado. O APK de instrumentação foi compilado, mas `connectedDebugAndroidTest` não foi executado nesta atualização. Não houve instalação por cima no celular do usuário nem acesso a seus registros pessoais; a compatibilidade foi conferida pela assinatura, identidade, versão e migração de dados.

Capturas nativas por desenho direto do View foram inspecionadas para cabeçalho e opções de vídeo, mas algumas camadas de telas posteriores ficam ausentes nesse método. Elas não comprovam uma revisão visual completa em aparelho. Os testes verificaram semântica, visibilidade, acessibilidade, posição e layout dos controles com as fontes citadas. YouTube/TikTok reais, disponibilidade pública dos vídeos, som/vibração, doze e comportamento de fabricantes ao bloquear/minimizar continuam dependendo de conferência em aparelho. As limitações anteriores das fontes das dicas não mudaram: não houve nova redação das dicas nesta etapa.

O checksum atual está em `instalar/SHA256SUMS.txt`; `instalar/versao.json` registra identificação, assinatura e resultados. Para atualizar, faça backup e instale por cima, escolhendo Atualizar. Não desinstale nem limpe os dados.

## Registros históricos das versões anteriores

Os resultados seguintes referem-se às entregas anteriores.

## Atualização 1.2 — resultados desta entrega (23/10/2026)

- `applicationId=br.com.wellness` preservado; `versionCode` de 2 para **3**, `versionName=1.2`, Android mínimo API 26.
- Cabeçalho centralizado, boas-vindas, quatro abas, catálogo dos **19 exercícios**, busca sem distinção de acentos/maiúsculas, variantes preservadas, vídeos por busca e dicas separadas dos cálculos.
- O campo editável por série “Kg usados” foi retirado da interface. O modelo `SetRecord.actualLoad` e as cópias `usedLoad`/`explicitLoad` continuam no armazenamento e nos backups, incluindo valores zero. A carga de trabalho e as reps continuam compartilhadas por perfil e identidade semântica da série.
- Migração **aditiva de formato 1 para 2**, mantendo o caminho `wellness-v1.json`. Ler o arquivo antigo não o substitui; a próxima gravação atômica inclui os campos novos. Não existe limpeza automática nem migração destrutiva. Formatos 1 e 2 são aceitos na importação. Histórico antigo recebe somente campos opcionais ausentes, sem recalcular ou sobrescrever os valores já concluídos.
- Os padrões de descanso vieram do pedido desta atualização, não foram atribuídos à planilha. Preferências por perfil + tipo/número da série; tempo entre exercícios separado. O descanso por exercício da versão anterior é reaproveitado como descanso entre exercícios. Snapshot de novas conclusões guarda também os descansos daquele momento.
- `RestAlerts` usa AlarmManager e notificação por canal, sem serviço em primeiro plano. Pausa/encerramento cancelam o alarme; substituição troca o único alarme. A contagem usa prazo de término salvo em disco. O receiver não escreve no arquivo de treinos. Preferências próprias deduplicam o aviso. Notificação, som/vibração, Não perturbe e autorização de alarmes precisos ficam sob controle do Android. Sem alarme preciso, há fallback para alarme permitido em economia de bateria, sujeito a atraso. Sem notificação permitida, fica o aviso visual no treino.

### Comandos realmente executados

```sh
source /workspace/toolchains/wellness-env.sh
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --console=plain
```

Resultado final: **BUILD SUCCESSFUL**, **21 testes**, **zero falhas, erros ou ignorados**, e **lint sem problemas**. Distribuição: 10 testes originais, 8 testes da atualização, 2 testes de integração de plataforma com Robolectric e 1 teste de navegação Compose com Robolectric. As advertências de depreciação no compilador de testes referem-se a APIs de simulação do próprio Robolectric; não são erros de lint do aplicativo.

Verificações executadas:

- Os **4.158 casos** de fórmulas originais continuam passando. Abrangem as 594 séries e diferentes entradas, incrementos, vazio e zero; exemplos reais permanecem na seção abaixo.
- A sincronização foi exercitada entre todas as ocorrências das 24 sessões. Working Set 1 é identificado por chave, mesmo quando muda de posição; aparelhos e variações mantêm valores separados.
- Fixture com o esquema antigo real (formato 1, sem campos novos): carga, reps zero, incremento, descanso anterior, marcas, carga específica zero, temporizador e histórico são lidos e preservados; gravação/reabertura e exportação/importação mantêm os dados. Editar os valores compartilhados e os novos descansos não reescreve o snapshot antigo.
- Todos os tipos de série existentes no arquivo recebem o descanso padrão pedido. Rest Pause tem padrão 20 s, mas não foi acrescentado onde não existe. Preferências por tipo/número propagam entre fases e sessões equivalentes sem misturar perfis. Entradas inválidas de minutos/segundos e overflow são rejeitadas.
- Substituição exige confirmação para cronômetro em andamento **ou pausado**. Marcar uma série não inicia descanso. Descanso não marca série nem conclui exercício/sessão. O horário de término, pausa e retomada são persistidos e conferidos usando tempos controlados.
- As 19 URLs são decodificadas nos testes para a frase exata `como fazer [nome]`, sem semana/carga/série. Teste Android simulado força a recusa dos dois pacotes TikTok e verifica o navegador recebendo **a mesma URL**. Outro caminho aceita o primeiro pacote e não abre navegador. Isso verifica o roteamento, não o comportamento de um TikTok real ou seus resultados de busca.
- AlarmManager e NotificationManager simulados: agendamento, deduplicação, cancelamento ao pausar, substituição e uma única notificação ao vencer o prazo. Não equivale a medir latência real do Android, doze, som ou vibração em hardware.
- **Navegação em execução com Robolectric/Android 14 simulado, gráficos nativos**: inicialização com registros anteriores, cabeçalho/boas-vindas, Exercícios, busca `sumo`, botão de vídeo, Histórico, Ajustes, retorno à sessão já concluída, seção de dica/cálculo, ausência do campo Kg usados, ajuste 1:05, faixa de contagem, pausa, confirmação/manutenção do timer, retomada, rolagem e encerramento. O teste verifica que a faixa continua visível na rolagem e que o arquivo preserva carga antiga, kg zero, histórico e preferência ajustada.
- Capturas pela captura de janela Compose não funcionaram no Robolectric (`forceRedraw` expirou). A alternativa de desenho direto do View produziu imagens parciais de camadas; elas foram inspecionadas, mas **não validam a aparência completa**. Os testes de semântica/visibilidade passaram. As capturas incompletas não são entregues como prints de um celular.
- `apksigner verify --print-certs`: APK novo e APK 1.1 têm certificado SHA-256 **`f45a0d175aed2396cba54737eee94ee8b96fda1190f9a8c5d95619687a7566d5`**. `aapt dump badging` confirma mesmo pacote e número de versão aumentado. O JSON oficial no APK novo é byte a byte igual ao arquivo embarcado no APK anterior; a planilha e seus cálculos não mudaram.

### Limitações atuais

Não houve teste em celular físico nem instalação por cima do aplicativo do usuário. Sem `/dev/kvm`, não foi repetido o emulador de sistema completo que morreu na entrega inicial; desta vez a navegação foi executada com Robolectric. O APK de instrumentação foi compilado, mas `connectedDebugAndroidTest` não foi executado nesta atualização. O navegador/TikTok reais, a concessão de permissões do fabricante e os avisos com tela bloqueada ainda precisam de conferência em aparelho. A identidade/assinatura e a migração permitem atualizar sem desinstalar; não se afirma acesso aos registros pessoais do usuário.

As fontes institucionais de execução ficaram bloqueadas pelo proxy com HTTP 403. Foram consultadas as observações da planilha e uma base técnica comunitária pública; fontes, correspondências, adaptações e cópia das entradas estão em [DICAS-E-FONTES.md](DICAS-E-FONTES.md). A consulta de fontes primárias confiáveis solicitada pelo usuário permanece limitada por esse bloqueio; não se atribui validação clínica às dicas. Não foram inventadas regulagens universais.

O APK 1.2 e seu checksum atualizado estão em `instalar/Wellness-teste.apk` e `instalar/SHA256SUMS.txt`.

## Registros das entregas anteriores

As seções seguintes descrevem verificações históricas das versões 1.0 e 1.1; as limitações específicas daquela época não substituem os resultados 1.2 acima.

## Atualização 1.1 — somente tema e identificação da versão

- Paleta fixa preto, branco e vermelho em `res/values/colors.xml`, aplicada pelo `WellnessTheme` a todas as telas. As barras nativas usam os mesmos recursos; o modo claro/escuro do celular não seleciona outra paleta.
- A organização das telas e os callbacks de entrada/ação foram comparados com a versão anterior e preservados. Não houve alteração em `domain/`, `data/`, `WellnessViewModel`, planilha, plano JSON ou testes de cálculo/persistência.
- Versão 1.1, `versionCode=2` (antes 1), `applicationId=br.com.wellness` inalterado, Android mínimo API 26 preservado.
- A chave anterior estava disponível. `apksigner verify --print-certs` confirmou que os certificados dos APKs antigo e novo têm o mesmo SHA-256: `f45a0d175aed2396cba54737eee94ee8b96fda1190f9a8c5d95619687a7566d5`.
- `:app:testDebugUnitTest`: 10 testes executados e aprovados, zero falhas/erros/ignorados; inclui os 4.158 casos das fórmulas e os testes de compartilhamento, histórico, backup e persistência.
- `:app:assembleDebug`: APK 1.1 gerado; assinatura validada e identificação/versão conferidas com `aapt`. O plano dentro do APK continua idêntico ao da versão anterior.
- `:app:lintDebug`: **No issues found**, zero erros e avisos. Os atributos de barras/contraste foram separados por versão do Android para manter suporte ao Android 8.0.
- Contraste calculado a partir da paleta: branco/preto **21:1**; branco/superfície **18,26:1**; branco/campo **16,29:1**; cinza claro/superfície **9,72:1**; branco/botão vermelho **5,62:1**; vermelho selecionado/superfície **4,85:1**. Seleção e conclusão também têm ✓; indisponibilidade ao salvar informa “Salvando…”.
- A atualização no celular e a aparência em execução não foram testadas nesta etapa. Não foi repetida a inicialização do emulador sem KVM que já apresentou as falhas documentadas abaixo. Não se afirma validação visual em aparelho.
- Para atualizar preservando os registros, instale por cima e escolha **Atualizar**. Não desinstale nem limpe os dados. A mesma identidade/assinatura e o formato dos dados preservado permitem a atualização; os registros no aparelho do usuário não foram acessados.
- APK 1.1 SHA-256: `62430e5201c9cd2c1855fe12cea7506ca231bc0e10960161338beb8056590685`.

## Ambiente utilizado

- Linux x86_64, JDK Temurin 17.0.21+9 (o Java inicial não tinha `javac`).
- Gradle Wrapper 8.10.2, Android Gradle Plugin 8.7.3, Kotlin/Compose Compiler 2.0.21.
- SDK Platform 35 e Build Tools 35.0.0. Android mínimo: API 26.
- Proxy da plataforma configurado para Gradle. Certificados do sistema utilizados pelo JDK; TLS permaneceu habilitado.
- Downloads do JDK e Gradle conferidos com SHA-256; ferramentas de linha de comando e emulador com os checksums oficiais do repositório Android.

## Verificações executadas

- Extração das **25 abas**, **24 sessões**, **130 ocorrências de exercícios** e **594 séries**. Todas as fórmulas de carga correspondem ao padrão verificado `IF/ISNUMBER/OR/ROUNDDOWN`; os fatores são extraídos linha a linha. Os 48 vínculos de agenda/conclusão foram conferidos. Configurações e links de vídeos foram extraídos e preservados; não foi feita reprodução dos vídeos.
- `:app:testDebugUnitTest`: **10 testes aprovados, zero falhas, zero ignorados**. Um dos testes executa **4.158 casos numéricos** gerados diretamente das fórmulas originais; outro percorre todas as ocorrências para verificar o compartilhamento por perfil e pelo tipo/número da série.
- Persistência em arquivo temporário real na JVM: gravação, reabertura por outra instância do armazenamento, restauração de backup, rejeição de JSON inválido/versão incompatível/outra planilha/valor negativo e preservação dos dados anteriores.
- Histórico: conclusão, alteração posterior de carga/reps, gravação e reabertura, mantendo a primeira cópia intacta. Nova conclusão preserva ambas; progresso conta sessões distintas.
- Equipamentos e variações: remada na Polia separada da Máquina; elevações laterais sentada e sentada (90º) separadas; troca de perfil não copia valores e recuperação do perfil anterior restaura seus valores.
- Marcas e cargas específicas das séries não se propagam para outra sessão. Vazio e zero permanecem distintos.
- Temporizador: restauração do horário de término, pausa, retomada e término.
- `:app:assembleDebug` e `:app:assembleDebugAndroidTest`: APKs gerados com sucesso.
- `:app:lintDebug --rerun-tasks`: concluído, **No issues found** (zero erros e zero avisos), após conferência das regras de backup Android.
- `bash tools/cloud_setup.sh`: preparação repetida com sucesso, incluindo compilação e validações. A extração e as fixtures também foram repetidas, com arquivos idênticos.
- `apksigner verify --verbose`: assinatura v2 do APK válida. O JSON dentro do APK foi comparado byte a byte com o plano extraído e é idêntico.

## Interface: tentativa bloqueada pelo emulador da nuvem

Foi instalado um emulador Android 11/API 30. A máquina não expõe `/dev/kvm`, portanto ele precisou iniciar por software (`-accel off`). O Android demorou a preparar seus serviços e ficou instável.

`connectedDebugAndroidTest` foi realmente executado em duas tentativas, mas ambas terminaram **antes de executar o teste**: 0 testes de interface executados. A primeira teve `ShellCommandUnresponsiveException` ao obter propriedades e identificou a API incorretamente como 1. A segunda, com `ddmlib.getprop.timeout.sec=60`, identificou a API 30, mas o instalador de testes continuou com informações inconsistentes de versão (`Cannot install split APKs with API level < 21`). Isso não foi uma falha de assertiva do aplicativo.

A instalação direta de `app-debug.apk`, com `adb install --no-streaming -r`, retornou **Success**. Depois, ao tentar abrir a Activity, o Android respondeu **`cmd: Can't find service: activity`**. A instalação do APK de testes também encontrou **`cmd: Can't find service: package`**, mesmo após esses serviços terem aparecido antes. `sys.boot_completed` não chegou a retornar 1 durante as verificações.

O buffer de falhas do próprio Android confirmou `DeadSystemException: The system died` nos processos `com.android.systemui` e `com.android.permissioncontroller`. O bloqueio observado pertence à inicialização do Android do emulador.

Assim, **a interface não foi validada em execução**. Não se afirma que o aplicativo abriu nesse emulador. O APK foi compilado e assinado; os testes locais de cálculo, sincronização e persistência passaram. Para executar o teste de interface, use um celular de teste ou um emulador do Android Studio com aceleração de hardware. O comando e o aviso sobre limpeza dos dados estão no README.

Os relatórios detalhados são gerados em `app/build/reports/tests/testDebugUnitTest/` e `app/build/reports/lint-results-debug.html`. Eles não são versionados, pois são saídas de compilação.

## Exemplos de cálculo

Dados reais preenchidos no arquivo: **S1 A1, cadeira abdutora, B9 = 5 kg**, incremento J9 vazio:

| Célula | Série | Fórmula da planilha | Resultado validado |
|---|---|---|---|
| D6 | Aquecimento | 5 × 0,30 | 1,5 kg |
| D7 | Feeder Set 1 | 5 × 0,70 | 3,5 kg |
| D8 | Feeder Set 2 | 5 × 0,85 | 4,25 kg |
| D9 | Working Set 1 | 5 × 1 | 5 kg |

Outros fatores reais, com **entradas controladas de teste** (não são cargas iniciais prescritas):

| Origem | Entrada de teste | Resultado |
|---|---|---|
| S4 A2, terra sumô, Top Set | WS 100 kg, incremento vazio, fator 1,08 | 108 kg |
| S1 A2, supino, Back Off | WS 100 kg, incremento vazio, fator 0,75 | 75 kg, calculados da WS |
| Série com fator 0,85 | WS 10 kg, incremento 2,5 kg | 7,5 kg, arredondado para baixo |
| Série com fator 0,85 | WS vazia | Carga vazia |
| Série com fator 0,85 | WS zero | 0 kg |

O aplicativo usa números decimais (`BigDecimal`) para evitar erros de arredondamento binário nas divisões pelos incrementos.

## Limites

A validação em um celular físico, a instalação por gerenciador de arquivos e a escolha de arquivos de backup dependem de um aparelho do usuário. Não foram testados em celular físico. O teste automatizado de interface usa um aparelho dedicado e limpa os dados dele antes de começar.

Os campos `install_script` e `start_skill` foram salvos no rascunho da configuração da nuvem. Isso não publica o ambiente: revise e salve nas configurações e publique para reutilizá-lo. Nenhuma validação em uma tarefa nova foi realizada.

O APK é de depuração, assinado para testes. Assinatura de produção, publicação na Play Store e restauração em uma nova tarefa de nuvem não fazem parte das verificações acima.
