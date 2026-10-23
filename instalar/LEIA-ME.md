# Instalar ou atualizar o Wellness 1.2

Você não precisa programar nem instalar Android Studio para usar este APK.

## Se já tem o Wellness instalado

1. No aplicativo atual, entre em **Ajustes → Exportar backup** e guarde o arquivo.
2. Baixe `Wellness-teste.apk` desta pasta. No GitHub, toque em **Download raw** (seta de download).
3. Abra o APK no celular e escolha **Atualizar**. Não desinstale nem limpe os dados.

A identificação continua `br.com.wellness` e a chave de assinatura é a mesma do APK 1.1. O número interno aumentou de 2 para 3; a versão é 1.2. Cargas, repetições, aparelhos, progresso e histórico são preservados. A migração aceita os registros e backups antigos. Se o Android mostrar erro de assinatura, pare e informe o erro, mantendo o aplicativo instalado.

Esta atualização traz o cabeçalho centralizado, “Bem-vinda, Nanda”, aba Exercícios com busca, botões vermelhos para pesquisar execução no TikTok, dicas separadas das notas de cálculo e descansos ajustáveis por série e entre exercícios. O campo “Kg usados” saiu da tela, mas seus registros antigos continuam nos dados e backups.

Na primeira utilização do descanso, permita notificações se quiser receber o aviso ao bloquear a tela. Em **Ajustes**, configure som/vibração e permita alarmes precisos. O Android respeita seu volume, Não perturbe e as permissões. Sem alarmes precisos, o aviso pode atrasar na economia de bateria; ao reabrir, a contagem usa o horário correto.

## Primeira instalação ou pasta completa

No Android 8.0 ou superior, abra **Downloads**, toque no APK e autorize a instalação pelo navegador/gerenciador de arquivos quando solicitado. Para baixar todo o projeto, use **Code → Download ZIP** no GitHub, extraia e abra a pasta `instalar`; o APK está nela.

O APK foi compilado e assinado; cálculos, armazenamento, migração e navegação foram verificados por testes locais, incluindo Android simulado com Robolectric. Não houve instalação desta versão em celular físico nem teste real do aplicativo TikTok, som/vibração ou tela bloqueada. As fontes efetivamente consultadas para as dicas e o bloqueio às fontes institucionais estão em `docs/DICAS-E-FONTES.md`. Os resultados completos ficam em `docs/VALIDACAO.md`.
