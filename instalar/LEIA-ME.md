# Atualizar o Wellness para 1.3

Você não precisa programar nem instalar Android Studio.

1. No aplicativo atual, faça **Ajustes → Exportar backup** e guarde o arquivo.
2. Baixe `Wellness-teste.apk` desta pasta. No GitHub, toque em **Download raw** (seta de download).
3. No Android, abra o arquivo em **Downloads** e escolha **Atualizar**. Autorize a instalação por esse navegador/gerenciador quando solicitado.

Instale por cima: **não desinstale nem limpe os dados**. A identificação continua `br.com.wellness`; a mesma chave do APK 1.2 é usada. O número interno aumentou de 3 para **4**, versão **1.3**. Se aparecer erro de assinatura, pare e informe o erro mantendo o aplicativo instalado.

## O que mudou

- Cabeçalho com WELLNESS e o W já usado no ícone; títulos dos treinos em maiúsculas.
- **Ver treinos** abre as oito semanas. Cards mostram pendente, em andamento ou concluído em verde, sem depender de datas.
- Botões lado a lado para **YouTube** (vídeo original) e **TikTok** (busca do exercício).
- **Ajustar tempo**, botões adaptados a letras maiores e um único descanso na última série. O último exercício tem **Finalizar treino**.
- **Ranking** considera as sessões diferentes já concluídas e as próximas. Repetições da mesma sessão não dão pontos extras. O histórico completo continua em **Ranking → Ver histórico**.

Cargas, reps, equipamentos, tempos personalizados e histórico são preservados na migração. As novas preferências também entram nos backups. A primeira abertura pode comemorar uma conquista obtida com conclusões antigas; a comemoração não se repete ao reabrir normalmente.

Para baixar o projeto inteiro, use **Code → Download ZIP** no GitHub, extraia e abra `instalar`; o APK está nessa pasta. Requer Android 8.0 ou superior. Os resultados realmente executados e as limitações de testes estão em `docs/VALIDACAO.md`. O som/vibração e a entrega dos avisos continuam sujeitos às permissões e configurações do Android.
