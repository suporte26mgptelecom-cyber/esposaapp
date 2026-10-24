# Vídeos TikTok escolhidos — atualização 1.4

Os 19 mapeamentos abaixo reproduzem os links do usuário. São **18 URLs únicas**; as duas elevações laterais sentadas têm o mesmo vídeo por escolha expressa, mas os cadastros e seus dados permanecem separados. O mapeamento usa o ID do exercício e vale em todas as 24 sessões e na aba Exercícios. O plano oficial e seus links YouTube permanecem iguais.

## Resolução e disponibilidade dos links

Em 24/10/2026 foram feitas requisições HTTPS GET aos 18 links únicos, com seguimento de redirecionamentos habilitado. **Todas foram bloqueadas pelo proxy do ambiente com `Tunnel connection failed: 403 Forbidden` no CONNECT**. Não houve resposta do servidor TikTok, código de redirecionamento, endereço completo obtido ou verificação do conteúdo do vídeo. Esse erro de rede não comprova que um link é inválido ou que o vídeo está indisponível.

Os resultados por URL, data/hora e cadeia de redirecionamentos estão em [TIKTOK-RESOLUCAO.json](TIKTOK-RESOLUCAO.json). Todas as cadeias estão vazias e `resolvedUrl` permanece `null`. O aplicativo usa os links curtos originais; o próprio TikTok ou o navegador escolhido pela pessoa fará a resolução quando tiver acesso. Um campo de endereço completo está preparado para uma futura resolução verificável, sem substituir o original.

| Exercício | Link original preservado | Endereço completo nesta nuvem |
|---|---|---|
| Cadeira abdutora | https://vt.tiktok.com/ZSguYVXWq/ | Não obtido: proxy HTTP 403 |
| Cadeira flexora | https://vt.tiktok.com/ZSguYpLt9/ | Não obtido: proxy HTTP 403 |
| Hack squat | https://vt.tiktok.com/ZSgu2rJfJ/ | Não obtido: proxy HTTP 403 |
| Cadeira extensora | https://vt.tiktok.com/ZSgu22FfE/ | Não obtido: proxy HTTP 403 |
| Leg press unilateral | https://vt.tiktok.com/ZSgu2Ypaq/ | Não obtido: proxy HTTP 403 |
| Elevação pélvica no smith, máquina ou livre | https://vt.tiktok.com/ZSgu2vKYY/ | Não obtido: proxy HTTP 403 |
| Leg press 45º | https://vt.tiktok.com/ZSgujNwML/ | Não obtido: proxy HTTP 403 |
| Stiff unilateral | https://vt.tiktok.com/ZSgu6Ff9c/ | Não obtido: proxy HTTP 403 |
| Terra sumô | https://vt.tiktok.com/ZSgu61uNB/ | Não obtido: proxy HTTP 403 |
| Abdução de quadril na polia | https://vt.tiktok.com/ZSgu6NrYG/ | Não obtido: proxy HTTP 403 |
| Supino inclinado na máquina | https://vt.tiktok.com/ZSgu6mvV5/ | Não obtido: proxy HTTP 403 |
| Elevação lateral com halteres sentada | https://vt.tiktok.com/ZSgu6D5Vo/ | Não obtido: proxy HTTP 403 |
| Remada articulada unilateral | https://vt.tiktok.com/ZSgu6M4wm/ | Não obtido: proxy HTTP 403 |
| Puxada alta com pegada neutra ou triângulo | https://vt.tiktok.com/ZSgu6U9pA/ | Não obtido: proxy HTTP 403 |
| Desenvolvimento na máquina articulada | https://vt.tiktok.com/ZSgu69kJx/ | Não obtido: proxy HTTP 403 |
| Tríceps na polia alta com barra reta ou V | https://vt.tiktok.com/ZSgurAVmj/ | Não obtido: proxy HTTP 403 |
| Elevação lateral com halteres sentada (90º) | https://vt.tiktok.com/ZSgu6D5Vo/ | Não obtido: proxy HTTP 403 |
| Remada baixa na polia com barra reta | https://vt.tiktok.com/ZSguM3Hea/ | Não obtido: proxy HTTP 403 |
| Elevação lateral na polia | https://vt.tiktok.com/ZSguM3vTX/ | Não obtido: proxy HTTP 403 |

## Abertura no aplicativo versus validação dos links

O Wellness tenta `ACTION_VIEW`, com a URL do vídeo e o pacote TikTok explicitamente definido (`com.zhiliaoapp.musically` e `com.ss.android.ugc.trill`). Havendo endereço completo verificado, ele é tentado antes do original. A implementação não usa pesquisa Google, pesquisa TikTok, URI inventada ou abertura da página inicial como alternativa. O sucesso do `startActivity` é chamado de **envio sem confirmação**, pois não permite observar o que o TikTok mostra.

Se os pacotes recusarem a URL, o Wellness oferece opções. **Abrir no navegador** resolve somente aplicativos navegadores e envia o mesmo vídeo, apenas após toque. **Copiar link** copia exatamente o original e não abre outro aplicativo. Se o TikTok aceitou o direcionamento mas exibiu outro conteúdo, a pessoa pode voltar ao Wellness e usar essas opções. Não há leitura de tela do TikTok, coleta de dados de outros aplicativos ou declaração automática de vídeo reproduzido.

Não há TikTok nem dispositivo Android conectado neste ambiente. Os testes com Robolectric simulam aceitação/recusa dos intents e verificam o endereço enviado, a interface, as ações manuais e a preservação dos dados. Eles **não validam o recebimento/reprodução dentro de um TikTok instalado real**. Os detalhes e resultados ficam em [VALIDACAO.md](VALIDACAO.md).
