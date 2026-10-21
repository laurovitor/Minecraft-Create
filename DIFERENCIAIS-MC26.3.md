# Diferenciais do Create 26.3 — edição de Lauro

Este documento reúne apenas customizações implementadas e verificadas para orientar uma futura publicação. A seção abaixo é texto voltado a jogadores; a evidência técnica fica separada.

## Texto para publicação

### Liga de andesito: duas unidades no crafting

Ao combinar **2 andesitos** com **2 pepitas de ferro ou de zinco** na bancada de trabalho, você recebe **2 ligas de andesito** em vez de 1. As receitas de mistura mecânica não mudaram: **1 andesito + 1 pepita de ferro ou de zinco → 1 liga**.

## Evidência técnica (não é copy de publicação)

- Implementação: commit `a3a37707ccbb1a440f6dc46372705af57b29a705` (`Double shaped andesite alloy recipe yield`) na branch `mc26.3/dev`. O gerador `src/main/java/com/simibubi/create/foundation/data/recipe/CreateStandardRecipeGen.java` passou a definir retorno de 2 para ambas as variantes de crafting.
- Receitas geradas: `src/generated/resources/data/create/recipe/crafting/materials/andesite_alloy.json` e `andesite_alloy_from_zinc.json` usam dois andesitos, duas pepitas e `result.count: 2`. As variantes em `src/generated/resources/data/create/recipe/mixing/` continuam com um andesito, uma pepita e uma liga resultante.
- Verificação registrada em `/home/openclaw/.openclaw/workspace/work-reports/2026-10-21_create-263-andesite-alloy/subsession-01.md` (registro operacional local): build offline aprovado e receitas conferidas no JAR. **Não houve teste in-game dessa mudança.**

## Critério para novos diferenciais

Adicionar uma funcionalidade à seção publicável somente depois de ela estar implementada no código/recursos da branch-alvo e de sua configuração ou comportamento relevante ter sido verificado com evidência rastreável (commit, arquivos e verificação executada). Ideias, pedidos e itens ainda não implementados não entram como funcionalidades; registrar limites de teste sem convertê-los em alegações de validação em jogo.
