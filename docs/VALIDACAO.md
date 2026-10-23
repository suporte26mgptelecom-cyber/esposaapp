# Validação da primeira versão

Planilha oficial: `ACADEMIAWELLNESS.xlsx`, SHA-256 `59cd316986fdf8c8619ff1003961b192a65166a8bb3b9e20baed016e6725cb1a`.

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
