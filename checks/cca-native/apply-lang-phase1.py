#!/usr/bin/env python3
"""Adds the en_us / pt_br strings of the electricity import (phase 1) to Create's lang files.

Registry names go to the generated en_us and to pt_br; tooltips and interface strings also go to the
lang/default sources the datagen reads, so a future datagen run keeps them. Idempotent: existing keys
are left alone, so re-running only adds what is missing.

Run from the repository root: python3 -I checks/cca-native/apply-lang-phase1.py
"""
import json
from pathlib import Path

EN = Path("src/generated/resources/assets/create/lang/en_us.json")
PT = Path("src/main/resources/assets/create/lang/pt_br.json")
DEFAULT_TOOLTIPS = Path("src/main/resources/assets/create/lang/default/tooltips.json")
DEFAULT_INTERFACE = Path("src/main/resources/assets/create/lang/default/interface.json")

# key: (en_us, pt_br)
NAMES = {
    "block.create.rolling_mill": ("Rolling Mill", "Laminadora"),
    "block.create.electric_motor": ("Electric Motor", "Motor Elétrico"),
    "block.create.alternator": ("Alternator", "Alternador"),
    "block.create.connector": ("Small Connector", "Conector Pequeno"),
    "block.create.small_light_connector": ("Small Connector with Light", "Conector Pequeno com Lâmpada"),
    "block.create.large_connector": ("Large Connector", "Conector Grande"),
    "block.create.modular_accumulator": ("Modular Accumulator", "Acumulador Modular"),
    "block.create.creative_energy": ("Creative Energy Source", "Fonte de Energia Criativa"),
    "block.create.electrum_block": ("Block of Electrum", "Bloco de Electrum"),
    "item.create.electrum_ingot": ("Electrum Ingot", "Barra de Electrum"),
    "item.create.electrum_nugget": ("Electrum Nugget", "Pepita de Electrum"),
    "item.create.electrum_sheet": ("Electrum Sheet", "Chapa de Electrum"),
    "item.create.zinc_sheet": ("Zinc Sheet", "Chapa de Zinco"),
    "item.create.copper_rod": ("Copper Rod", "Haste de Cobre"),
    "item.create.iron_rod": ("Iron Rod", "Haste de Ferro"),
    "item.create.gold_rod": ("Gold Rod", "Haste de Ouro"),
    "item.create.electrum_rod": ("Electrum Rod", "Haste de Electrum"),
    "item.create.brass_rod": ("Brass Rod", "Haste de Latão"),
    "item.create.copper_wire": ("Copper Wire", "Fio de Cobre"),
    "item.create.iron_wire": ("Iron Wire", "Fio de Ferro"),
    "item.create.gold_wire": ("Gold Wire", "Fio de Ouro"),
    "item.create.electrum_wire": ("Electrum Wire", "Fio de Electrum"),
    "item.create.diamond_grit": ("Diamond Grit", "Grãos de Diamante"),
    "item.create.spool": ("Empty Spool", "Carretel Vazio"),
    "item.create.copper_spool": ("Copper Spool", "Carretel de Cobre"),
    "item.create.gold_spool": ("Gold Spool", "Carretel de Ouro"),
    "item.create.electrum_spool": ("Electrum Spool", "Carretel de Electrum"),
    "item.create.festive_spool": ("Festive Spool", "Carretel Festivo"),
    "item.create.capacitor": ("Capacitor", "Capacitor"),
    "item.create.diamond_grit_sandpaper": ("Diamond Grit Sandpaper", "Lixa de Diamante"),
}

TOOLTIPS = {
    "block.create.rolling_mill.tooltip.summary": (
        "Uses _Rotational Force_ to roll _ingots_ into _rods_ and _rods_ into _wires_.",
        "Usa _Força Rotacional_ para laminar _barras_ em _hastes_ e _hastes_ em _fios_."),
    "block.create.rolling_mill.tooltip.condition1": ("When Powered", "Quando Ativada"),
    "block.create.rolling_mill.tooltip.behaviour1": (
        "Processes items _thrown_ or _inserted_ at its _top_. Right-click to take out the results.",
        "Processa itens _jogados_ ou _inseridos_ pelo _topo_. Clique com o botão direito para retirar o resultado."),
    "block.create.electric_motor.tooltip.summary": (
        "A compact and configurable source of _Rotational Force_ that runs on _electric energy_ (FE).",
        "Uma fonte compacta e configurável de _Força Rotacional_ movida a _energia elétrica_ (FE)."),
    "block.create.electric_motor.tooltip.condition1": ("When Powered", "Quando Energizado"),
    "block.create.electric_motor.tooltip.behaviour1": (
        "Spins at the speed set on its _back panel_. Energy use grows with the speed. A _redstone signal_ stops it.",
        "Gira na velocidade definida no _painel traseiro_. O consumo de energia cresce com a velocidade. Um _sinal de redstone_ o desliga."),
    "block.create.alternator.tooltip.summary": (
        "Turns _Rotational Force_ into _electric energy_ (FE).",
        "Transforma _Força Rotacional_ em _energia elétrica_ (FE)."),
    "block.create.alternator.tooltip.condition1": ("When Rotated", "Quando Girado"),
    "block.create.alternator.tooltip.behaviour1": (
        "The _faster_ it spins, the more energy it produces. Take the energy out with a _connector_ in _Pull_ mode.",
        "Quanto mais _rápido_ gira, mais energia produz. Retire a energia com um _conector_ no modo _Puxar_."),
    "block.create.connector.tooltip.summary": (
        "Connects blocks that use _FE_ with _wires_ to move _energy_ between them.",
        "Conecta blocos que usam _FE_ por meio de _fios_ para levar _energia_ de um para o outro."),
    "block.create.connector.tooltip.condition1": ("Low Current", "Baixa Corrente"),
    "block.create.connector.tooltip.behaviour1": (
        "Takes any wire (Copper, Gold, Electrum or Festive Spool), up to _4_ wires.",
        "Aceita qualquer fio (Carretel de Cobre, Ouro, Electrum ou Festivo), até _4_ fios."),
    "block.create.connector.tooltip.condition2": ("R-click with a Spool", "Clique direito com um Carretel"),
    "block.create.connector.tooltip.behaviour2": (
        "Click two connectors to run a _wire_ between them.",
        "Clique em dois conectores para passar um _fio_ entre eles."),
    "block.create.connector.tooltip.condition3": ("R-click with Wrench", "Clique direito com a Chave-inglesa"),
    "block.create.connector.tooltip.behaviour3": (
        "Cycles between _Push_, _Pull_ and _None_ modes.",
        "Alterna entre os modos _Empurrar_, _Puxar_ e _Nenhum_."),
    "block.create.small_light_connector.tooltip.summary": (
        "A _Small Connector_ that _lights up_ while energy runs through it, using a little of it.",
        "Um _Conector Pequeno_ que _acende_ enquanto a energia passa por ele, gastando um pouco dela."),
    "block.create.small_light_connector.tooltip.condition1": ("R-click with Wrench", "Clique direito com a Chave-inglesa"),
    "block.create.small_light_connector.tooltip.behaviour1": (
        "Cycles between _Push_, _Pull_ and _None_ modes.",
        "Alterna entre os modos _Empurrar_, _Puxar_ e _Nenhum_."),
    "block.create.large_connector.tooltip.summary": (
        "Connects blocks that use _FE_ with _high current wires_, for long lines and large flows of _energy_.",
        "Conecta blocos que usam _FE_ por _fios de alta corrente_, para linhas longas e grandes fluxos de _energia_."),
    "block.create.large_connector.tooltip.condition1": ("High Current", "Alta Corrente"),
    "block.create.large_connector.tooltip.behaviour1": (
        "Up to _6_ wires reaching twice as far. Two Large Connectors need a _Gold_ or _Electrum_ wire between them.",
        "Até _6_ fios com o dobro do alcance. Dois Conectores Grandes precisam de um fio de _Ouro_ ou _Electrum_ entre si."),
    "block.create.large_connector.tooltip.condition2": ("R-click with Wrench", "Clique direito com a Chave-inglesa"),
    "block.create.large_connector.tooltip.behaviour2": (
        "Cycles between _Push_, _Pull_ and _None_ modes.",
        "Alterna entre os modos _Empurrar_, _Puxar_ e _Nenhum_."),
    "block.create.modular_accumulator.tooltip.summary": (
        "Stores _electric energy_. Blocks placed together form one _multiblock_, up to 3x3 wide and 5 tall.",
        "Armazena _energia elétrica_. Blocos colocados juntos formam uma única _estrutura_, de até 3x3 de largura e 5 de altura."),
    "block.create.modular_accumulator.tooltip.condition1": ("With Connectors", "Com Conectores"),
    "block.create.modular_accumulator.tooltip.behaviour1": (
        "A connector in _Push_ mode fills it, one in _Pull_ mode drains it. It also feeds blocks on its _top_ and _bottom_.",
        "Um conector em modo _Empurrar_ o enche e um em modo _Puxar_ o esvazia. Ele também alimenta blocos no _topo_ e na _base_."),
    "block.create.creative_energy.tooltip.summary": (
        "Provides an endless supply of _electric energy_.",
        "Fornece _energia elétrica_ infinita."),
    "item.create.spool.tooltip.summary": ("Holds _wires_ for connectors.", "Guarda _fios_ para os conectores."),
    "item.create.spool.tooltip.condition1": ("R-clicking two linked Connectors", "Clique direito em dois Conectores ligados"),
    "item.create.spool.tooltip.behaviour1": (
        "Removes the wire between them and gives back a _filled Spool_.",
        "Remove o fio entre eles e devolve um _Carretel cheio_."),
    "item.create.copper_spool.tooltip.summary": (
        "A spool of _low current_ wire.", "Um carretel de fio de _baixa corrente_."),
    "item.create.copper_spool.tooltip.condition1": ("R-clicking two Connectors", "Clique direito em dois Conectores"),
    "item.create.copper_spool.tooltip.behaviour1": (
        "Runs a wire between them and keeps the _Empty Spool_. Cannot link two _Large Connectors_.",
        "Passa um fio entre eles e devolve o _Carretel Vazio_. Não liga dois _Conectores Grandes_ entre si."),
    "item.create.gold_spool.tooltip.summary": (
        "A spool of _high current_ wire.", "Um carretel de fio de _alta corrente_."),
    "item.create.gold_spool.tooltip.condition1": ("R-clicking two Connectors", "Clique direito em dois Conectores"),
    "item.create.gold_spool.tooltip.behaviour1": (
        "Runs a wire between them and keeps the _Empty Spool_. Fits _Small_ and _Large Connectors_.",
        "Passa um fio entre eles e devolve o _Carretel Vazio_. Serve para _Conectores Pequenos_ e _Grandes_."),
    "item.create.electrum_spool.tooltip.summary": (
        "A spool of _high current_ wire.", "Um carretel de fio de _alta corrente_."),
    "item.create.electrum_spool.tooltip.condition1": ("R-clicking two Connectors", "Clique direito em dois Conectores"),
    "item.create.electrum_spool.tooltip.behaviour1": (
        "Runs a wire between them and keeps the _Empty Spool_. Fits _Small_ and _Large Connectors_.",
        "Passa um fio entre eles e devolve o _Carretel Vazio_. Serve para _Conectores Pequenos_ e _Grandes_."),
    "item.create.festive_spool.tooltip.summary": (
        "A copper wire strung with _colourful lights_.", "Um fio de cobre enfeitado com _luzinhas coloridas_."),
    "item.create.festive_spool.tooltip.condition1": ("R-clicking two Connectors", "Clique direito em dois Conectores"),
    "item.create.festive_spool.tooltip.behaviour1": (
        "Runs a _festive_ wire between them and gives back an _Empty Spool_. Cannot link two _Large Connectors_.",
        "Passa um fio _festivo_ entre eles e devolve um _Carretel Vazio_. Não liga dois _Conectores Grandes_ entre si."),
    "item.create.capacitor.tooltip.summary": (
        "A small _energy_ cell. Charged by any _FE_ charger and used to build electric machines.",
        "Uma pequena célula de _energia_. Pode ser carregada por qualquer carregador de _FE_ e é usada para montar máquinas elétricas."),
    "item.create.diamond_grit_sandpaper.tooltip.summary": (
        "Can be used to _refine materials_ like regular sandpaper, but lasts far longer. The process can be automated with a Deployer.",
        "Serve para _refinar materiais_ como a lixa comum, mas dura muito mais. O processo pode ser automatizado com um Implantador."),
    "item.create.diamond_grit_sandpaper.tooltip.condition1": ("When Used", "Quando Usada"),
    "item.create.diamond_grit_sandpaper.tooltip.behaviour1": (
        "Applies polish to items held in the _offhand_ or lying on the _floor_ when _looking at them_",
        "Aplica um polimento aos itens segurados na _Mão secundária_ ou jogados no _chão_ quando _olhando em sua direção_"),
}

INTERFACE = {
    "create.recipe.rolling": ("Rolling", "Laminação"),
    "create.wire.connect": ("Wire connector selected", "Conector selecionado"),
    "create.wire.connect_in": ("Wire connector input selected", "Entrada do conector selecionada"),
    "create.wire.connect_out": ("Wire connector output selected", "Saída do conector selecionada"),
    "create.wire.count": ("This connector has no free wire slots", "Este conector não tem mais espaço para fios"),
    "create.wire.exists": ("These connectors are already linked", "Esses conectores já estão ligados"),
    "create.wire.invalid": ("Invalid connection", "Conexão inválida"),
    "create.wire.linked": ("Connectors linked", "Conectores ligados"),
    "create.wire.linked_in": ("Wire linked to input", "Fio ligado à entrada"),
    "create.wire.linked_out": ("Wire linked to output", "Fio ligado à saída"),
    "create.wire.long": ("Wire is too long", "Fio longo demais"),
    "create.wire.no_connection": ("No wire is connected here", "Não há fio ligado aqui"),
    "create.wire.obstructed": ("Wire is obstructed", "O fio está obstruído"),
    "create.wire.removed": ("Wire removed", "Fio removido"),
    "create.wire.requires_high_current": ("This connection needs a high current wire",
                                          "Esta ligação precisa de um fio de alta corrente"),
    "create.spool.connecting": ("Holding one end of a wire", "Segurando a ponta de um fio"),
    "create.tooltip.accumulator.info": ("Accumulator Info:", "Informações do Acumulador:"),
    "create.tooltip.alternator.info": ("Alternator Info:", "Informações do Alternador:"),
    "create.tooltip.connector.info": ("Connector Info:", "Informações do Conector:"),
    "create.tooltip.energy.capacity": ("Capacity:", "Capacidade:"),
    "create.tooltip.energy.consumption": ("Energy Usage:", "Consumo de Energia:"),
    "create.tooltip.energy.mode": ("Mode:", "Modo:"),
    "create.tooltip.energy.none": ("None", "Nenhum"),
    "create.tooltip.energy.passive": ("Passive", "Passivo"),
    "create.tooltip.energy.production": ("Energy Generated:", "Energia Gerada:"),
    "create.tooltip.energy.pull": ("Pull", "Puxar"),
    "create.tooltip.energy.push": ("Push", "Empurrar"),
    "create.tooltip.energy.stored": ("Stored Energy:", "Energia Armazenada:"),
    "create.tooltip.energy.usage": ("Network Usage:", "Uso da Rede:"),
    "create.display_source.modular_accumulator": ("Accumulator", "Acumulador"),
    "create.display_source.accumulator.current": ("Energy Stored in ⚡", "Energia Armazenada em ⚡"),
    "create.display_source.accumulator.display": ("Choose an Option:", "Escolha uma Opção:"),
    "create.display_source.accumulator.max": ("Total Capacity", "Capacidade Total"),
    "create.display_source.accumulator.percent": ("Percentage", "Porcentagem"),
    "create.display_source.accumulator.progress_bar": ("Progress Bar", "Barra de Progresso"),
    "create.display_source.accumulator.remaining": ("Remaining ⚡", "⚡ Restante"),
    "create.subtitle.electric_motor_buzz": ("Electric machine hums", "Máquina elétrica zumbe"),
    "create.subtitle.electric_charge": ("Electricity crackles", "Eletricidade estala"),
    "create.ponder.tag.electric": ("Electric Blocks", "Blocos Elétricos"),
    "create.ponder.tag.electric.description": (
        "Components which generate, store, carry or use electric energy",
        "Componentes que geram, armazenam, transportam ou usam energia elétrica"),
    "create.ponder.electric_motor.header": ("Generating Rotational Force using Electric Motors",
                                            "Gerando Força Rotacional com Motores Elétricos"),
    "create.ponder.electric_motor.text_1": ("Electric Motors are a compact and configurable source of Rotational Force",
                                            "Motores Elétricos são uma fonte compacta e configurável de Força Rotacional"),
    "create.ponder.electric_motor.text_2": ("Scrolling on the back panel changes the RPM of the motor's rotational output",
                                            "Rolar o mouse no painel traseiro muda o RPM da saída do motor"),
    "create.ponder.electric_motor.text_3": ("The Electric Motor requires a source of energy",
                                            "O Motor Elétrico precisa de uma fonte de energia"),
    "create.ponder.electric_motor.text_4": ("The motor's energy consumption is determined by the set RPM",
                                            "O consumo de energia do motor depende do RPM definido"),
    "create.ponder.alternator.header": ("Generating Electric energy using an Alternator",
                                        "Gerando energia elétrica com um Alternador"),
    "create.ponder.alternator.text_1": ("It generates electric energy from rotational force",
                                        "Ele gera energia elétrica a partir de força rotacional"),
    "create.ponder.alternator.text_2": ("Its energy production is determined by the input RPM",
                                        "A energia produzida depende do RPM de entrada"),
    "create.ponder.alternator.text_3": ("Use a connector in Pull mode to take the energy out",
                                        "Use um conector no modo Puxar para retirar a energia"),
    "create.ponder.rolling_mill.header": ("Rolling metals into Rods and Wires", "Laminando metais em Hastes e Fios"),
    "create.ponder.rolling_mill.text_1": ("The Rolling Mill uses rotational force to roll metals into Rods and Wires",
                                          "A Laminadora usa força rotacional para laminar metais em Hastes e Fios"),
    "create.ponder.rolling_mill.text_2": ("To manually input items, drop Ingots or Plates on the top of the Mill",
                                          "Para inserir itens à mão, jogue Barras ou Chapas no topo da Laminadora"),
    "create.ponder.rolling_mill.text_3": ("Right-click the Mill to manually retrieve the output items",
                                          "Clique com o botão direito na Laminadora para retirar o resultado"),
    "create.ponder.automated_rolling_mill.header": ("Automating the Rolling Mill", "Automatizando a Laminadora"),
    "create.ponder.automated_rolling_mill.text_1": ("The Rolling Mill can be automated using a Belt and two Funnels",
                                                    "A Laminadora pode ser automatizada com uma Esteira e dois Funis"),
    "create.ponder.automated_rolling_mill.text_2": ("Input Funnel", "Funil de Entrada"),
    "create.ponder.automated_rolling_mill.text_3": ("Output Funnel", "Funil de Saída"),
    "create.ponder.modular_accumulator.header": ("The Multi-block Accumulator", "O Acumulador Multibloco"),
    "create.ponder.modular_accumulator.text_1": ("The Accumulator is a multi-block structure",
                                                 "O Acumulador é uma estrutura de vários blocos"),
    "create.ponder.modular_accumulator.text_2": ("It can store large amounts of electric energy",
                                                 "Ele armazena grandes quantidades de energia elétrica"),
    "create.ponder.modular_accumulator.text_3": ("Configure an input connector", "Configure um conector de entrada"),
    "create.ponder.modular_accumulator.text_4": ("and an output connector.", "e um conector de saída."),
    "create.ponder.modular_accumulator.text_5": (
        "For compatibility with other mods, the Accumulator also pushes energy into blocks placed on its top or bottom",
        "Para compatibilidade com outros mods, o Acumulador também envia energia aos blocos no seu topo ou na sua base"),
    "create.ponder.connector.header": ("Transferring Energy using Connectors", "Transferindo Energia com Conectores"),
    "create.ponder.connector.text_1": ("The Alternator is a producer of energy...", "O Alternador produz energia..."),
    "create.ponder.connector.text_2": ("...while the Electric Motor is a consumer of energy",
                                       "...enquanto o Motor Elétrico consome energia"),
    "create.ponder.connector.text_3": (
        "Producers need a connector in Pull mode, which pulls energy out of the block it is attached to",
        "Produtores precisam de um conector no modo Puxar, que tira energia do bloco onde está preso"),
    "create.ponder.connector.text_4": (
        "Consumers need a connector in Push mode, which pushes energy into the block it is attached to",
        "Consumidores precisam de um conector no modo Empurrar, que envia energia para o bloco onde está preso"),
    "create.ponder.connector.text_5": ("Use a Wrench on a connector to cycle its mode",
                                       "Use a Chave-inglesa em um conector para trocar o modo"),
    "create.ponder.connector.text_6": ("Connectors in None mode do not interact with the block they are attached to...",
                                       "Conectores no modo Nenhum não interagem com o bloco onde estão presos..."),
    "create.ponder.connector.text_7": ("...they only relay energy through their wires, so they can be placed on any block",
                                       "...eles só repassam energia pelos fios, então podem ficar em qualquer bloco"),
    "create.ponder.connector.text_8": ("Connect two connectors with a wire by right-clicking both using a Copper Spool",
                                       "Ligue dois conectores com um fio clicando com o botão direito nos dois usando um Carretel de Cobre"),
    "create.ponder.connector.text_9": ("Energy now flows from the Alternator to the Electric Motor",
                                       "Agora a energia flui do Alternador para o Motor Elétrico"),
    "create.ponder.connector_remove.header": ("Removing Wires using an Empty Spool",
                                              "Removendo Fios com um Carretel Vazio"),
    "create.ponder.connector_remove.text_1": ("Right-click both ends of a wire with an Empty Spool to remove it",
                                              "Clique com o botão direito nas duas pontas de um fio com um Carretel Vazio para removê-lo"),
    "create.ponder.connector_remove.text_2": ("The wire is returned as a filled Spool", "O fio volta como um Carretel cheio"),
}


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


def dump(path, data):
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def append_default(path, entries):
    """lang/default files are hand-formatted with tabs; append new lines before the closing brace."""
    text = path.read_text(encoding="utf-8")
    existing = json.loads(text)
    new = {k: v for k, v in entries.items() if k not in existing}
    if not new:
        return
    body = text.rstrip()
    assert body.endswith("}")
    body = body[:-1].rstrip()
    lines = ",\n".join(f"\t{json.dumps(k, ensure_ascii=False)}: {json.dumps(v, ensure_ascii=False)}"
                       for k, v in new.items())
    path.write_text(body + ",\n\n" + lines + "\n}\n", encoding="utf-8")
    json.loads(path.read_text(encoding="utf-8"))


en, pt = load(EN), load(PT)
for table in (NAMES, TOOLTIPS, INTERFACE):
    for key, (en_text, pt_text) in table.items():
        en.setdefault(key, en_text)
        pt.setdefault(key, pt_text)
dump(EN, en)
dump(PT, pt)
append_default(DEFAULT_TOOLTIPS, {k: v[0] for k, v in TOOLTIPS.items()})
append_default(DEFAULT_INTERFACE, {k: v[0] for k, v in INTERFACE.items()})
print(f"apply-lang-phase1: {len(NAMES) + len(TOOLTIPS) + len(INTERFACE)} keys checked")
