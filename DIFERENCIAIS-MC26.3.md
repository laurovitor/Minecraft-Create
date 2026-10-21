# Diferenciais do Create 26.3 — edição de Lauro

Este documento reúne apenas customizações implementadas e verificadas para orientar uma futura publicação. A seção abaixo é texto voltado a jogadores; a evidência técnica fica separada.

## Texto para publicação

### Liga de andesito: duas unidades no crafting

Ao combinar **2 andesitos** com **2 pepitas de ferro ou de zinco** na bancada de trabalho, você recebe **2 ligas de andesito** em vez de 1. As receitas de mistura mecânica não mudaram: **1 andesito + 1 pepita de ferro ou de zinco → 1 liga**.

### Cofre de Itens Revestido

Um cofre novo, revestido com chapas resistentes. Receita na bancada, em coluna: **chapa resistente / cofre de itens / chapa resistente**.

- Forma estruturas maiores que o cofre comum: base de até **5×5** e comprimento de até **3× a largura**, chegando a **5×5×15**. O cofre comum continua com base máxima de 3×3 e comprimento máximo de 9.
- Cada bloco guarda **10% a mais** que um bloco do cofre comum. Na configuração padrão são **22 espaços por bloco** em vez de 20, o que dá até 8.250 espaços num 5×5×15.
- Tem a mesma textura do cofre comum, só que no tom escuro da chapa resistente.
- Funciona como o cofre comum: encaixe automático ao colocar blocos, separação com a chave inglesa, comparador, funis, packager e montagem em contraptions. Cofres comuns e revestidos não se juntam na mesma estrutura.

## Evidência técnica (não é copy de publicação)

- Implementação: commit `a3a37707ccbb1a440f6dc46372705af57b29a705` (`Double shaped andesite alloy recipe yield`) na branch `mc26.3/dev`. O gerador `src/main/java/com/simibubi/create/foundation/data/recipe/CreateStandardRecipeGen.java` passou a definir retorno de 2 para ambas as variantes de crafting.
- Receitas geradas: `src/generated/resources/data/create/recipe/crafting/materials/andesite_alloy.json` e `andesite_alloy_from_zinc.json` usam dois andesitos, duas pepitas e `result.count: 2`. As variantes em `src/generated/resources/data/create/recipe/mixing/` continuam com um andesito, uma pepita e uma liga resultante.
- Verificação registrada em `/home/openclaw/.openclaw/workspace/work-reports/2026-10-21_create-263-andesite-alloy/subsession-01.md` (registro operacional local): build offline aprovado e receitas conferidas no JAR. **Não houve teste in-game dessa mudança.**

### Cofre de Itens Revestido — evidência técnica

- Implementação: commit `REPLACE_COMMIT` na branch `mc26.3/dev`. Novo bloco `create:reinforced_item_vault` (`ReinforcedItemVaultBlock`) e block entity próprio (`ReinforcedItemVaultBlockEntity`). O block entity usa `getMaxWidth() = 5` e mantém o comprimento máximo como 3× a largura. Espaços por bloco: `floor(vaultCapacity × 11 / 10)`; com o padrão 20, dá 22. O cofre comum não mudou.
- O cofre comum e o revestido não se misturam porque `ConnectivityHandler.partAt` compara o tipo do block entity. O auto-place e o eixo de colocação também só seguem blocos do mesmo tipo.
- Registro de capacidade de itens, identificador de inventário do packager, mounted storage (`VAULT`, com a mesma proteção contra uso como combustível) e tags do Ponder foram estendidos ao bloco novo.
- Receita gerada: `src/generated/resources/data/create/recipe/crafting/kinetics/reinforced_item_vault.json`. Loot table, blockstate, modelo de item, tag de picareta e lang (en_us, en_ud e pt_br) foram escritos à mão, espelhando o cofre comum, porque o datagen client/Registrate desta port falha (`LootContextParamSets.REGISTRY`).
- Texturas: 12 sprites em `textures/block/reinforced_vault/`, recoloridos a partir das texturas do cofre com a paleta da `sturdy_sheet`, mais o modelo `models/block/reinforced_item_vault.json`.
- Verificação: `./gradlew --offline build` passou e os recursos foram conferidos dentro do JAR. **Não houve teste em jogo.** Ainda falta ver no jogo: formação 5×5×15, divisão com a chave, comparador, contraption e as texturas conectadas nas larguras 4 e 5. O Ponder não ganhou cena própria, e as cenas do cofre comum não foram associadas ao revestido porque citam o limite de 3.

## Critério para novos diferenciais

Adicionar uma funcionalidade à seção publicável somente depois de ela estar implementada no código/recursos da branch-alvo e de sua configuração ou comportamento relevante ter sido verificado com evidência rastreável (commit, arquivos e verificação executada). Ideias, pedidos e itens ainda não implementados não entram como funcionalidades; registrar limites de teste sem convertê-los em alegações de validação em jogo.
