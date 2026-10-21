# Diferenciais do Create 26.3 — edição de Lauro

Este documento reúne apenas customizações implementadas e verificadas para orientar uma futura publicação. A seção abaixo é texto voltado a jogadores; a evidência técnica fica separada.

## Texto para publicação

### Liga de andesito: duas unidades no crafting

Ao combinar **2 andesitos** com **2 pepitas de ferro ou de zinco** na bancada de trabalho, você recebe **2 ligas de andesito** em vez de 1. As receitas de mistura mecânica não mudaram: **1 andesito + 1 pepita de ferro ou de zinco → 1 liga**.

### Cofre de Itens Revestido

Um cofre novo, revestido com chapas resistentes. Receita na bancada, em coluna: **chapa resistente / cofre de itens / chapa resistente**.

- Forma estruturas maiores que o cofre comum. O comum cresce em passos de 3 (3, 6 e 9 de comprimento conforme a largura); o revestido cresce em passos de 5: **largura 1 → até 5 de comprimento, largura 2 → até 10, larguras 3 a 5 → até 15**. O maior é **5×5×15**. O cofre comum continua com máximo de 3×3×9.
- Cada bloco guarda **25% a mais** que um bloco do cofre comum. Na configuração padrão são **25 espaços por bloco** em vez de 20, o que dá até 9.375 espaços num 5×5×15.
- Tem o mesmo desenho do cofre comum, mas com o corpo bem escuro, no tom da chapa resistente, e as bordas prateadas. Assim dá pra diferenciar um do outro de longe.
- Segurando **Shift** sobre o item aparece a descrição dele, em inglês e em português.
- Funciona como o cofre comum: encaixe automático ao colocar blocos, separação com a chave inglesa, comparador, funis, packager e montagem em contraptions. Cofres comuns e revestidos não se juntam na mesma estrutura.

### Prancheta anota a ocupação do cofre

Com uma **prancheta em branco** na mão, clique com o botão direito num **Cofre de Itens** ou num **Cofre de Itens Revestido** e a prancheta anota as informações dele: tamanho (ex.: 5x5x15), capacidade total em espaços, espaços livres e porcentagem de ocupação. Os itens não são listados, só os totais, pra saber quando é hora de montar um cofre novo. A anotação é uma foto do momento do clique; pra atualizar, use outra prancheta em branco ou limpe a atual. Se a prancheta já tiver algo escrito, aparece um aviso e nada é sobrescrito.

### Golem de Andesito

Um ajudante novo para fazendas automáticas, com o corpo do golem de cobre na cor da liga de andesito.

- **Como criar:** coloque uma **abóbora esculpida** (ou lanterna de abóbora) sobre um **bloco de liga de andesito**. O golem nasce e o bloco vira o **Baú do Golem de Andesito**, a casa dele.
- **O que ele faz:** num raio de **15 blocos** do baú, em qualquer altura, ele **corta árvores inteiras** e **colhe plantações maduras** (trigo, cenoura, batata, beterraba e fungo do Nether). **Replanta quando há muda ou semente adequada** no que colheu ou no próprio baú; nunca troca a espécie da árvore.
- **Filtro:** a tampa do baú tem um **slot de filtro do Create**, igual ao dos funis. Sem filtro, ele trabalha em tudo e guarda tudo. Com filtro, só corta e colhe o que produz os itens do filtro e só guarda esses itens; o resto é descartado.
- **Árvores 2x2:** carvalho escuro, carvalho pálido, pinheiro gigante e selva gigante são reconhecidos pelo tronco 2x2 e recebem até **4 mudas da própria espécie** no lugar do tronco, se houver mudas na colheita ou no baú. Se faltar a muda certa, ele não planta outra espécie.
- **Folhas no caminho:** se as folhas de uma árvore que ele vai cortar chegam até o chão e bloqueiam o tronco, ele quebra as folhas à frente até alcançar o tronco. O que cair dessas folhas passa pelo filtro como o resto.
- **Abelhas em paz:** ele também não corta árvore que tenha colmeia ou ninho de abelha.
- **Levar o golem junto:** quebrando o baú com o golem vivo, o golem some e o baú vira o item "Baú do Golem de Andesito (Com Golem)". Ao colocar esse item de novo, o golem volta com a mesma vida, os itens do baú, o filtro e o que estava carregando.
- **Cura:** com **Shift + clique direito** usando **liga de andesito**, cada liga recupera **5%** da vida do golem.
- **Prancheta:** com uma prancheta em branco, clique direito no golem ou no baú dele para anotar a vida, a capacidade do baú, os espaços usados e os livres.
- **Descanso:** com o baú cheio ou sem nada maduro por perto, ele passeia num raio de **5 blocos** do baú.
- **Limite visível:** ao criar o golem (pela abóbora ou pelo ovo), a grama e a terra no limite dos 15 blocos e em volta do baú viram caminho de terra, marcando a área de trabalho. Recolher e recolocar o baú não repete isso.
- **Mangue:** árvores de mangue, que ficam sobre raízes, também são cortadas e replantadas com propágulo.
- **Dorme à noite:** se a área de trabalho não estiver iluminada, à noite ele vai para o lado do baú e dorme. A luz na área ou o retorno do dia o acorda. Dormindo, os monstros o ignoram. Uma pancada também acorda ele.
- **Chave inglesa:** Shift + chave inglesa no baú recolhe direto para o inventário, com o golem junto quando ele está vivo.
- **Perigos:** toma **dano de queda** e é **alvo de monstros hostis**. Mantenha a fazenda iluminada e protegida. Se cair num buraco, fica preso até o jogador ajudar.
- **Quando o golem morre**, ele deixa **2 ligas de andesito** e às vezes uma **semente de abóbora**, e o baú fica **rachado** e sem uso. Ele ainda guarda os itens e pode ir na **prensa mecânica**, que devolve **6 ligas de andesito**.
- **Slot de filtro:** um rebaixo discreto no meio da tampa marca onde o filtro vai, no estilo dos blocos do Create.
- **JEI e criativo:** existe o **Ovo Gerador de Golem de Andesito**, que coloca o golem já com o baú dele. O JEI tem uma página de informação para o ovo, o baú e o baú com golem, explicando como criar e usar.
- O baú abre como um baú comum (27 espaços) e funciona com funis, hoppers, empacotadora e comparador. Só o baú **sem golem** entra na prensa mecânica.

## Evidência técnica (não é copy de publicação)

- Implementação: commit `a3a37707ccbb1a440f6dc46372705af57b29a705` (`Double shaped andesite alloy recipe yield`) na branch `mc26.3/dev`. O gerador `src/main/java/com/simibubi/create/foundation/data/recipe/CreateStandardRecipeGen.java` passou a definir retorno de 2 para ambas as variantes de crafting.
- Receitas geradas: `src/generated/resources/data/create/recipe/crafting/materials/andesite_alloy.json` e `andesite_alloy_from_zinc.json` usam dois andesitos, duas pepitas e `result.count: 2`. As variantes em `src/generated/resources/data/create/recipe/mixing/` continuam com um andesito, uma pepita e uma liga resultante.
- Verificação registrada em `/home/openclaw/.openclaw/workspace/work-reports/2026-10-21_create-263-andesite-alloy/subsession-01.md` (registro operacional local): build offline aprovado e receitas conferidas no JAR. **Não houve teste in-game dessa mudança.**

### Cofre de Itens Revestido — evidência técnica

- Implementação: commit `ad5d932ea238655d3e61adae2b5c8e3851213ae7` na branch `mc26.3/dev`. Novo bloco `create:reinforced_item_vault` (`ReinforcedItemVaultBlock`) e block entity próprio (`ReinforcedItemVaultBlockEntity`). O block entity usa `getMaxWidth() = 5` e comprimento máximo `min(largura × 5, 15)`. Espaços por bloco: `floor(vaultCapacity × 5 / 4)`; com o padrão 20, dá 25. Comprimento e capacidade foram ajustados no commit `0bbafb2456fc9a128952feff657168109ef3ec31`, a pedido do Lauro, depois do primeiro teste em jogo. O cofre comum não mudou.
- O cofre comum e o revestido não se misturam porque `ConnectivityHandler.partAt` compara o tipo do block entity. O auto-place e o eixo de colocação também só seguem blocos do mesmo tipo.
- Registro de capacidade de itens, identificador de inventário do packager, mounted storage (`VAULT`, com a mesma proteção contra uso como combustível) e tags do Ponder foram estendidos ao bloco novo.
- Receita gerada: `src/generated/resources/data/create/recipe/crafting/kinetics/reinforced_item_vault.json`. Loot table, blockstate, modelo de item, tag de picareta e lang (en_us, en_ud e pt_br) foram escritos à mão, espelhando o cofre comum, porque o datagen client/Registrate desta port falha (`LootContextParamSets.REGISTRY`).
- Texturas: 12 sprites em `textures/block/reinforced_vault/`, recoloridos a partir das texturas do cofre. Pixels com luminância até 110 (o corpo azul) viram tons escuros da `sturdy_sheet`; acima de 110 (as bordas) viram prata. Modelo em `models/block/reinforced_item_vault.json`.
- Descrição no Shift: chave `block.create.reinforced_item_vault.tooltip.summary` em `lang/default/tooltips.json`, `en_us.json` (gerado, editado à mão) e `pt_br.json`. Ela é exibida pelo `ItemDescription.Modifier` padrão do Create.
- Verificação: `./gradlew --offline build` passou e os recursos foram conferidos dentro do JAR. Primeiro teste em jogo pelo Lauro: o 5×5 grande formou certo, mas o pequeno seguia os passos de 3, o que levou ao ajuste acima. Ainda falta confirmar no jogo: os passos de 5 nas larguras 1 e 2, divisão com a chave, comparador, contraption e as texturas conectadas nas larguras 4 e 5. O Ponder não ganhou cena própria, e as cenas do cofre comum não foram associadas ao revestido porque citam o limite de 3.

### Prancheta anota a ocupação do cofre — evidência técnica

- Implementação: commit `654222384ffbb06b32f5c195316cd59c2885fa91`. Classe nova `content/logistics/vault/ItemVaultClipboardInfo.java`, chamada em `ClipboardValueSettingsHandler.interact` só no clique direito (não sobrescreve colar com clique esquerdo). Funciona com os dois tipos de cofre, porque checa `ItemVaultBlockEntity`.
- "Em branco" quer dizer nenhuma entrada escrita e nenhuma configuração copiada. O cálculo é feito no servidor, pela capability de itens do multibloco inteiro. A ocupação usa a mesma medida do comparador: média de quanto cada slot está cheio em relação ao limite do slot e da pilha.
- Lang: chaves `create.clipboard.vault_info.*` em `lang/default/interface.json`, `en_us.json` (gerado, editado à mão) e `pt_br.json`. As entradas ficam como texto traduzível, então aparecem no idioma de quem lê.
- Verificação: `./gradlew --offline build` passou. **Sem teste em jogo** até agora.

### Golem de Andesito — evidência técnica

- Implementação: commit `4e55aeca941578ffcebd13b1de9783512a20fa21`. Pacote `content/equipment/andesiteGolem/`:
  - `AndesiteGolem` (entidade, `AbstractGolem`, atributos iguais aos do golem de cobre: 12 de vida, velocidade 0.2, step 1);
  - `AndesiteGolemWorkGoal` (procura, anda até o alvo, trabalha 30 ticks, volta e guarda 20 ticks; desiste de alvos inalcançáveis por 60 s);
  - `AndesiteGolemStrollGoal`, `AndesiteGolemChestBlock`/`BlockEntity` (27 slots, `FilteringBehaviour` na tampa, estado `active`), `AndesiteGolemRenderer` (modelo e animações do golem de cobre, textura própria), `AndesiteGolemSpawning` e `AndesiteGolemEvents` (monstros não neutros ganham alvo no golem).
- Criação: mixin `CarvedPumpkinBlockMixin` em `trySpawnGolem` e `canSpawnGolem`. O `canSpawnGolem` deixa dispensers também colocarem a abóbora no bloco de liga.
- Corte de árvore: reaproveita o `TreeCutter` da serra mecânica. Ganhou `Tree.hasLeaves()` para o golem ignorar construções de tronco sem folhas. Colheita: segue a lógica da colheitadeira (tira uma semente do drop para replantar).
- Assets: textura do golem e do baú recoloridas a partir do golem de cobre e do baú de cobre do jogo base, com a paleta da liga de andesito. O baú inativo é mais escuro e tem rachaduras. Modelos em `models/block/andesite_golem_chest/`. Blockstate, item, loot, tag de picareta, receita de prensa (`recipe/pressing/andesite_alloy_from_golem_chest.json`) e lang (en_us, en_ud, pt_br e tooltip) foram escritos à mão, porque o datagen client desta port falha.
- Verificação: `./gradlew --offline build` passou. Também foi testado num **servidor dedicado de desenvolvimento**, por comandos via RCON:
  - o golem nasceu com abóbora e com lanterna de abóbora, e o bloco virou baú ativo ligado a ele;
  - trigo e cenoura foram colhidos e replantados, e o carvalho foi derrubado e replantado com muda;
  - os itens foram parar no baú;
  - com filtro `oak_log`, ele só guardou toras e deixou o trigo maduro em paz, e a muda de replantio saiu do baú;
  - um zumbi matou o golem, e o baú ficou inativo; o mesmo aconteceu com a morte por queda;
  - sem trabalho, ele passeou a até cerca de 5 blocos do baú.
- Complementos (commit `da507618c42a9584acceff883b4261a2d51de4b3`):
  - `Tree.hasBeeHome()` checa ninhos e colmeias nas anexações e ao redor de troncos e folhas; o golem pula essas árvores.
  - Item `packed_andesite_golem_chest`, que guarda vida, itens, filtro e o que o golem carregava, e recria o golem ao ser colocado. O drop normal do baú ativo quebrado por jogador é esse item; o loot table do bloco ficou vazio porque o drop é feito em `playerWillDestroy`.
  - Cura com liga em `AndesiteGolem.mobInteract`; prancheta no golem e no baú via `AndesiteGolemClipboardInfo`.
  - Baú registrado no `InventoryIdentifier` da empacotadora.
  - Correção da mensagem de erro no Shift (`ItemDescription.holdKeyParts`), que afetava as descrições com Shift, inclusive a do cofre revestido.
  - Verificado no servidor de desenvolvimento via RCON: árvore com ninho foi ignorada e, depois de remover o ninho, foi cortada e replantada; um hopper inseriu itens no baú.
  - Commit `af5cc63c0b86ffb98178d98072749ca05240e01e`: ovo gerador (`andesite_golem_spawn_egg`, via `spawnEgg()` do Registrate) e `AndesiteGolem.finalizeSpawn`, que coloca o baú quando o golem vem de ovo ou dispenser; páginas de info no JEI (`CreateJEI.registerRecipes`, chaves `create.jei.*`); moldura de latão na tampa das texturas `active`/`inactive`. Build passou; nada disso testado em jogo.
  - Commit `4b8c9c8198cb8d22a85268991fc103015bfa4795`: `AndesiteGolemWorkGoal.clearLeavesIfStuck` — a cada 20 ticks andando até uma árvore, se não chegou 0,3 bloco mais perto, quebra folhas na direção do tronco (altura dos pés e da cabeça). Build passou; não testado em jogo.
  - Commit `e29ea3a146c6f7b7c00cff810de173e271bc48f6`: replantio do tronco inteiro. Todas as toras da camada de baixo (`TreeCutter.Tree.getLogs()`) recebem muda do mesmo tipo, vinda dos drops e depois do baú. Build passou; não testado em jogo.
  - Commit `f3bbd7c6f0b29ad9b8c7c48ed56af5f4ddca48ac`:
    - raio de trabalho de 15;
    - `AndesiteGolemArea.markBoundary` (anel de caminho de terra só sobre `#minecraft:dirt` com ar em cima, mais os 8 blocos ao redor do baú), chamado só em `AndesiteGolemSpawning` e no `finalizeSpawn` do ovo;
    - `AndesiteGolemSleepGoal`: noite, com menos de 6 de 9 pontos da área com luz de bloco ≥ 8, faz o golem dormir ao lado do baú; `canBeSeenAsEnemy` é falso enquanto dorme, os olhos apagam e a cabeça abaixa;
    - base de tronco aceita raízes (`TreeCutter.isRoot`), e o replantio desce até o chão quando as raízes foram cortadas;
    - drops de morte: 2 ligas e 50% de chance de semente de abóbora;
    - `IWrenchable.onSneakWrenched` no baú;
    - textura do slot de filtro mais discreta.
    - Build passou; nada disso testado em jogo.
  - Correções da rodada de 2026-10-21 (commit `a88d882d26a81498b03e1e50ae901b64515fb23c`): `AndesiteGolemSleepGoal` reavalia luz e horário do Overworld durante o sono para soltar o goal ao clarear; `AndesiteGolemArea` procura solo pavimentável mesmo sob copa e marca os oito vizinhos na altura do bloco de liga ou um bloco abaixo; `AndesiteGolemWorkGoal` mascara apenas os outros três troncos de uma base 2x2 completa ao consultar `TreeCutter`, corta os quatro troncos e usa a espécie do tronco para escolher as mudas, sem substituir carvalho por bétula. As duas texturas do baú mudam só a área 8×8 do slot na tampa, para proeminência intermediária. Build offline aprovado; **sem teste em jogo, conforme pedido do Lauro**.
  - Não verificados em jogo: as correções desta rodada; quebrar e recolocar o baú com golem (precisa de jogador), cura e prancheta (precisam de clique de jogador), empacotadora e a correção do Shift (cliente).
- **Ainda não verificado:** o visual no cliente (modelo e texturas do golem e do baú, animações, slot de filtro na tampa) e a receita da prensa rodando numa prensa de verdade.

## Critério para novos diferenciais

Adicionar uma funcionalidade à seção publicável somente depois de ela estar implementada no código/recursos da branch-alvo e de sua configuração ou comportamento relevante ter sido verificado com evidência rastreável (commit, arquivos e verificação executada). Ideias, pedidos e itens ainda não implementados não entram como funcionalidades; registrar limites de teste sem convertê-los em alegações de validação em jogo.
