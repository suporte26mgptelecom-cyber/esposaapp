# Fontes das dicas de execução — versão 1.2

As prescrições, exercícios, alternativas, séries e percentuais continuam exclusivamente na planilha oficial. `wellness.json` não foi alterado. As dicas complementares ficam em `app/src/main/assets/exercise-guides.json`; não definem cargas, amplitudes, ângulos ou regulagens universais de aparelhos.

## Material efetivamente consultado em 23/10/2026

- As observações de todas as ocorrências da planilha `docs/ACADEMIAWELLNESS.xlsx`. Frases úteis foram preservadas. Na biblioteca, são reunidas por exercício; no treino, são as daquela sessão. Frases sobre percentuais, PURGE, comparação de cargas e forma de registro ficam em “Sobre o cálculo”.
- [Free Exercise DB, conjunto JSON completo](https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/dist/exercises.json), seus [README e licença](https://github.com/yuhonas/free-exercise-db). Base pública comunitária de exercícios mantida por Yuhonas, derivada de [wrkout/exercises.json](https://github.com/wrkout/exercises.json), licença Unlicense. Foram lidas as instruções dos movimentos listados abaixo. É uma referência técnica comunitária, **não uma fonte clínica ou revisão individual por profissional**.

A redação em português resume posicionamento estável, movimento controlado e erros comuns. Não foi uma tradução automática integral: não foram adotadas indicações universais de ângulo de joelho, distância dos pés, impedir o joelho de passar dos dedos, rotação interna deliberada do ombro ou inclinação para trás ao terminar o terra. As variantes unilaterais e de polia receberam adaptação conservadora do padrão correspondente, identificada abaixo, mantendo as observações da planilha. Isso não equivale a validação presencial do aparelho ou da execução de Nanda.

| Exercício do plano | Entrada consultada na base | Adaptação |
|---|---|---|
| Cadeira abdutora | `Thigh_Abductor` | Mesma família de movimento; preservadas as particularidades do plano. |
| Cadeira flexora | `Seated_Leg_Curl` | Mesma família de movimento; preservadas as particularidades do plano. |
| Hack squat | `Hack_Squat` | Mesma família de movimento; preservadas as particularidades do plano. |
| Cadeira extensora | `Leg_Extensions` | Mesma família de movimento; preservadas as particularidades do plano. |
| Leg press unilateral | `Leg_Press` | Adaptação para execução unilateral; sem mudar séries/reps por lado. |
| Elevação pélvica no smith, máquina ou livre | `Barbell_Hip_Thrust` | Mesma família de movimento; preservadas as particularidades do plano. |
| Supino inclinado na máquina | `Incline_Dumbbell_Press` | Padrão de supino inclinado; adaptado para as alças da máquina. |
| Elevação lateral com halteres sentada | `Seated_Side_Lateral_Raise` | Mesma família de movimento; preservadas as particularidades do plano. |
| Remada articulada unilateral | `Leverage_High_Row` | Adaptação para execução unilateral; sem mudar séries/reps por lado. |
| Puxada alta com pegada neutra ou triângulo | `Close-Grip_Front_Lat_Pulldown` | Mesma família de movimento; preservadas as particularidades do plano. |
| Desenvolvimento na máquina articulada | `Leverage_Shoulder_Press` | Mesma família de movimento; preservadas as particularidades do plano. |
| Tríceps na polia alta com barra reta ou V | `Triceps_Pushdown` | Mesma família de movimento; preservadas as particularidades do plano. |
| Leg press 45º | `Leg_Press` | Mesma família de movimento; preservadas as particularidades do plano. |
| Stiff unilateral | `Stiff-Legged_Dumbbell_Deadlift` | Adaptação para execução unilateral; sem mudar séries/reps por lado. |
| Elevação lateral com halteres sentada (90º) | `Seated_Side_Lateral_Raise` | Mesma família de movimento; preservadas as particularidades do plano. |
| Remada baixa na polia com barra reta | `Seated_Cable_Rows` | Mesma família de movimento; preservadas as particularidades do plano. |
| Elevação lateral na polia | `Side_Lateral_Raise` | Padrão de elevação lateral; adaptado para resistência da polia. |
| Terra sumô | `Sumo_Deadlift` | Mesma família de movimento; preservadas as particularidades do plano. |
| Abdução de quadril na polia | `Thigh_Abductor` | Padrão de abdução; adaptado para polia em pé com apoio de equilíbrio. |

## Limitação da consulta de fontes primárias

Foram tentados ACE Exercise Library, Mayo Clinic, ACSM, NHS, MedlinePlus/NIH, HSS, Cleveland Clinic e ExRx. O proxy do ambiente respondeu **HTTP 403 (CONNECT bloqueado)** e não entregou os conteúdos. Portanto esses sites **não são apresentados como fontes lidas nem como validação das dicas**. O pedido de consultar fontes institucionais confiáveis permanece limitado por esse bloqueio: a documentação permite uma revisão posterior das dicas com fontes primárias ou com um profissional de educação física. Não houve reprodução de vídeos do TikTok nem verificação da qualidade dos resultados da busca.

O aplicativo indica interromper o movimento ao sentir dor e pedir orientação. As dicas úteis do arquivo (por exemplo, band nos joelhos e alternativas de banco) foram mantidas como prescrição específica do plano, sem promovê-las a regulagem válida para qualquer máquina.

Uma cópia das entradas consultadas está em `docs/fontes/exercicios-consultados.json` (Unlicense). SHA-256 do conjunto agregado lido: `5bb747e3fc658f095a60dcbf6d53c96627acdcc6ffb6fffde86f7e26995d40bf`.
