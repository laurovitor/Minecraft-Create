#!/usr/bin/env python3
"""Adds the en_us / pt_br strings of the electricity import (phase 2) to Create's lang files.

Same layout as apply-lang-phase1.py: names go to the generated en_us and pt_br; tooltips and interface
strings also go to lang/default. Ponder texts are read from ElectricityScenes2.java in call order, so
the keys always match what Ponder looks up; each English text needs a pt_br entry in PONDER_PT.
Re-running overwrites the keys of this phase with the values below (so corrections apply).

Run from the repository root: python3 -I checks/cca-native/apply-lang-phase2.py
"""
import json
import re
import sys
from pathlib import Path

EN = Path("src/generated/resources/assets/create/lang/en_us.json")
PT = Path("src/main/resources/assets/create/lang/pt_br.json")
DEFAULT_TOOLTIPS = Path("src/main/resources/assets/create/lang/default/tooltips.json")
DEFAULT_INTERFACE = Path("src/main/resources/assets/create/lang/default/interface.json")
SCENES = Path("src/main/java/com/simibubi/create/infrastructure/ponder/scenes/ElectricityScenes2.java")

# Leftovers of an earlier run: "<id>.tooltip" headers are not read by Create 6 tooltips.
REMOVE = ["block.create.barbed_wire.tooltip", "block.create.redstone_relay.tooltip",
          "block.create.digital_adapter.tooltip"]

NAMES = {
    "block.create.tesla_coil": ("Tesla Coil", "Bobina de Tesla"),
    "block.create.servo_motor": ("Servo Motor", "Servomotor"),
    "block.create.electric_pump": ("Electric Pump", "Bomba Elétrica"),
    "block.create.redstone_relay": ("Redstone Relay", "Relé de Redstone"),
    "block.create.digital_adapter": ("Digital Adapter", "Adaptador Digital"),
    "block.create.portable_energy_interface": ("Portable Energy Interface", "Interface de Energia Portátil"),
    "block.create.liquid_blaze_burner": ("Liquid Blaze Burner", "Queimador de Blaze a Líquido"),
    "block.create.barbed_wire": ("Barbed Wire", "Arame Farpado"),
    "block.create.copper_wire_casing": ("Copper Wire Casing", "Revestimento de Fios de Cobre"),
    "item.create.straw": ("Straw", "Canudo"),
    "effect.create.shocking": ("Shocked", "Eletrocutado"),
    "death.attack.create.barbed_wire": ("%1$s tried to cross barbed wire", "%1$s tentou atravessar arame farpado"),
    "death.attack.create.barbed_wire.player": ("%1$s tried to cross barbed wire while fighting %2$s",
                                               "%1$s tentou atravessar arame farpado enquanto lutava contra %2$s"),
    "death.attack.create.tesla_coil": ("%1$s was zapped by a Tesla Coil", "%1$s levou um choque de uma Bobina de Tesla"),
    "death.attack.create.tesla_coil.player": ("%1$s was zapped by a Tesla Coil while fighting %2$s",
                                              "%1$s levou um choque de uma Bobina de Tesla enquanto lutava contra %2$s"),
}

TOOLTIPS = {
    "block.create.tesla_coil.tooltip.summary": (
        "Charges _items_ on a _Belt_ or _Depot_ below it with _electric energy_ and runs _Charging_ recipes.",
        "Carrega com _energia elétrica_ os _itens_ numa _Esteira_ ou _Depósito_ abaixo dela e executa receitas de _Carregamento_."),
    "block.create.tesla_coil.tooltip.condition1": ("When Powered by Redstone", "Quando Ativada por Redstone"),
    "block.create.tesla_coil.tooltip.behaviour1": (
        "_Shocks_ players and mobs in front of it. A full set of _chainmail_ armor protects against it.",
        "Dá _choques_ em jogadores e criaturas à sua frente. Uma armadura completa de _cota de malha_ protege."),
    "block.create.servo_motor.tooltip.summary": (
        "An _electric bearing_ that turns attached structures to a _precise angle_ set by _redstone_.",
        "Um _rolamento elétrico_ que gira as estruturas presas a ele até um _ângulo exato_ definido por _redstone_."),
    "block.create.servo_motor.tooltip.condition1": ("Redstone on its sides", "Redstone nas laterais"),
    "block.create.servo_motor.tooltip.behaviour1": (
        "One side turns it toward the _Max Angle_ and the opposite side toward the _Min Angle_; the two signals are summed.",
        "Um lado o gira em direção ao _Ângulo Máximo_ e o lado oposto em direção ao _Ângulo Mínimo_; os dois sinais são somados."),
    "block.create.electric_pump.tooltip.summary": (
        "A _Mechanical Pump_ that runs on _electric energy_ (FE) instead of _Rotational Force_. Scroll on its side to set the speed.",
        "Uma _Bomba Mecânica_ movida a _energia elétrica_ (FE) em vez de _Força Rotacional_. Role o mouse na lateral para definir a velocidade."),
    "block.create.electric_pump.tooltip.condition1": ("When Powered by Redstone", "Quando Ativada por Redstone"),
    "block.create.electric_pump.tooltip.behaviour1": ("_Stops_ pumping.", "_Para_ de bombear."),
    "block.create.redstone_relay.tooltip.summary": (
        "Lets _energy_ pass from its _input connector_ to its _output connector_ while it receives a _redstone signal_.",
        "Deixa a _energia_ passar do _conector de entrada_ para o _conector de saída_ enquanto recebe um _sinal de redstone_."),
    "block.create.redstone_relay.tooltip.condition1": ("Low Current", "Baixa Corrente"),
    "block.create.redstone_relay.tooltip.behaviour1": ("Connects with _low current wires_.",
                                                       "Liga-se com _fios de baixa corrente_."),
    "block.create.redstone_relay.tooltip.condition2": ("Output", "Saída"),
    "block.create.redstone_relay.tooltip.behaviour2": (
        "Gives _energy_ to the _wires_ on the connector marked with an _upward arrow_.",
        "Fornece _energia_ aos _fios_ do conector marcado com uma _seta para cima_."),
    "block.create.redstone_relay.tooltip.condition3": ("Input", "Entrada"),
    "block.create.redstone_relay.tooltip.behaviour3": (
        "Takes _energy_ from the _wires_ on the connector marked with a _downward arrow_.",
        "Recebe _energia_ dos _fios_ do conector marcado com uma _seta para baixo_."),
    "block.create.digital_adapter.tooltip.summary": (
        "A _ComputerCraft peripheral_ for _Display Boards_, gauges, pulleys, pistons, bearings and speed controllers. _ComputerCraft support is not included yet_: on its own it is an empty _Display Link_ source.",
        "Um _periférico do ComputerCraft_ para _Painéis de Exibição_, medidores, polias, pistões, rolamentos e controladores de velocidade. _O suporte ao ComputerCraft ainda não foi incluído_: sozinho, é uma fonte vazia de _Conector de Exibição_."),
    "block.create.portable_energy_interface.tooltip.summary": (
        "Exchanges _electric energy_ with the _Modular Accumulators_ of a moving _contraption_ when it meets a second interface.",
        "Troca _energia elétrica_ com os _Acumuladores Modulares_ de uma _engenhoca_ em movimento quando encontra uma segunda interface."),
    "block.create.portable_energy_interface.tooltip.condition1": ("When Powered by Redstone", "Quando Ativada por Redstone"),
    "block.create.portable_energy_interface.tooltip.behaviour1": (
        "Stops the _stationary_ interface from engaging.",
        "Impede que a interface _parada_ se conecte."),
    "block.create.liquid_blaze_burner.tooltip.summary": (
        "A _Blaze Burner_ with a _Straw_. Burns _liquid fuels_ from buckets or pipes, and still accepts solid fuel.",
        "Um _Queimador de Blaze_ com um _Canudo_. Queima _combustíveis líquidos_ de baldes ou canos e continua aceitando combustível sólido."),
    "block.create.liquid_blaze_burner.tooltip.condition1": ("Breaking it", "Ao quebrá-lo"),
    "block.create.liquid_blaze_burner.tooltip.behaviour1": (
        "Gives back the _Blaze Burner_ and the _Straw_.",
        "Devolve o _Queimador de Blaze_ e o _Canudo_."),
    "block.create.barbed_wire.tooltip.summary": (
        "_Slows_ and _hurts_ anything that walks through it. Goes on floors or walls.",
        "_Desacelera_ e _fere_ tudo que passa por ele. Pode ficar no chão ou na parede."),
    "item.create.straw.tooltip.summary": (
        "Right-click a _Blaze Burner_ with it to turn it into a _Liquid Blaze Burner_.",
        "Clique com o botão direito num _Queimador de Blaze_ para transformá-lo num _Queimador de Blaze a Líquido_."),
}

INTERFACE = {
    "create.recipe.charging": ("Charging", "Carregamento"),
    "create.recipe.liquid_burning": ("Liquid Burning", "Queima de Líquidos"),
    "create.recipe.assembly.charging": ("Charge with %1$s⚡", "Carregar com %1$s⚡"),
    "create.servo.max_angle": ("Max Angle", "Ângulo Máximo"),
    "create.servo.min_angle": ("Min Angle", "Ângulo Mínimo"),
    "create.tooltip.relay.info": ("Relay", "Relé"),
    "create.subtitle.tesla_coil": ("Tesla Coil buzzes", "Bobina de Tesla zumbe"),
    "create.subtitle.loud_zap": ("Tesla Coil zaps", "Bobina de Tesla dispara"),
    "create.subtitle.little_zap": ("Electricity sparks", "Eletricidade faísca"),
}

PONDER_PT = {
    "Charging Items with the Tesla Coil": "Carregando Itens com a Bobina de Tesla",
    "A Tesla Coil charges items held on a Belt or Depot below it": "A Bobina de Tesla carrega os itens parados numa Esteira ou Depósito abaixo dela",
    "It fills items that store energy (FE) and processes Charging recipes": "Ela enche itens que guardam energia (FE) e processa receitas de Carregamento",
    "Dangerous Tesla Coils": "Bobinas de Tesla Perigosas",
    "The Tesla Coil can also shock nearby players and mobs": "A Bobina de Tesla também pode dar choques em jogadores e criaturas próximos",
    "This is triggered by a Redstone signal": "Isso é ativado por um sinal de Redstone",
    "A full set of chainmail armor protects against the shock": "Uma armadura completa de cota de malha protege contra o choque",
    "Burning Liquid Fuels": "Queimando Combustíveis Líquidos",
    "Giving a Blaze Burner a Straw": "Dar um Canudo a um Queimador de Blaze",
    "lets it accept liquid fuels from buckets...": "faz ele aceitar combustíveis líquidos de baldes...",
    "...or from pipes": "...ou de canos",
    "Contraption Energy Exchange": "Troca de Energia com Engenhocas",
    "Accumulators on moving contraptions cannot be wired up": "Acumuladores em engenhocas em movimento não podem ser ligados com fios",
    "This component can exchange energy without the need to stop the contraption": "Este componente troca energia sem precisar parar a engenhoca",
    "Place a second one with a gap of 1 or 2 blocks in between": "Coloque uma segunda com 1 ou 2 blocos de espaço entre elas",
    "Whenever they pass by each other, they will engage in a connection": "Sempre que passarem uma pela outra, elas se conectam",
    "While engaged, the stationary interface will represent ALL Accumulators on the contraption": "Enquanto conectada, a interface parada representa TODOS os Acumuladores da engenhoca",
    "Energy can now be pushed in or pulled out...": "Agora dá para empurrar ou puxar energia...",
    "After no energy has been exchanged for a while, the contraption will continue on its way": "Depois de um tempo sem troca de energia, a engenhoca segue seu caminho",
    "Redstone Control": "Controle por Redstone",
    "Redstone power will prevent the stationary interface from engaging": "Sinal de redstone impede que a interface parada se conecte",
    "Fluid Transportation using Electric Pumps": "Transportando Fluidos com Bombas Elétricas",
    "Electric Pumps govern the flow of their attached pipe networks": "Bombas Elétricas controlam o fluxo das redes de canos ligadas a elas",
    "They run on energy instead of Rotational Force; scroll on their side to set the speed": "Elas funcionam com energia em vez de Força Rotacional; role o mouse na lateral para definir a velocidade",
    "Their arrow indicates the direction of flow": "A seta indica a direção do fluxo",
    "The network behind is now pulling fluids...": "A rede de trás agora está puxando fluidos...",
    "...while the network in front is transferring it outward": "...enquanto a rede da frente os empurra para fora",
    "A Wrench can be used to reverse the direction": "Use a Chave-inglesa para inverter a direção",
    "Rotating Blocks to precise Angles using the Servo Motor": "Girando Blocos até Ângulos Exatos com o Servomotor",
    "The Servo Motor uses redstone to rotate attached blocks to a precise angle": "O Servomotor usa redstone para girar os blocos presos a ele até um ângulo exato",
    "It needs a source of electric energy (FE) to operate": "Ele precisa de uma fonte de energia elétrica (FE) para funcionar",
    "Redstone power on this side turns the Servo towards its Max Angle...": "Sinal de redstone deste lado gira o Servo em direção ao Ângulo Máximo...",
    "...while power on the opposite side turns it towards its Min Angle": "...enquanto sinal do lado oposto o gira em direção ao Ângulo Mínimo",
    "The signals from both sides are summed, with the Min side counting as negative, so equal signals cancel each other out": "Os sinais dos dois lados são somados, com o lado Mínimo contando como negativo, então sinais iguais se anulam",
    "How far the Servo turns depends on the strength of the summed signal": "O quanto o Servo gira depende da força do sinal somado",
    "Scroll on the sides of the Servo to adjust its Max and Min Angles between 0° and 180°": "Role o mouse nas laterais do Servo para ajustar os Ângulos Máximo e Mínimo entre 0° e 180°",
    "With the Max Angle raised to 180°, the same signal now turns it twice as far": "Com o Ângulo Máximo em 180°, o mesmo sinal agora o gira o dobro",
}


def ponder_entries():
    """(key, en) pairs in the order Ponder numbers them: header, then text_1.. per scene."""
    source = SCENES.read_text(encoding="utf-8")
    entries = {}
    for method in re.split(r"\n\tpublic static void ", source)[1:]:
        title = re.search(r'scene\.title\("([a-z_]+)", "([^"]+)"\)', method)
        if not title:
            continue
        scene = title.group(1)
        entries[f"create.ponder.{scene}.header"] = title.group(2)
        for i, text in enumerate(re.findall(r'\.text\("([^"]+)"\)', method), 1):
            entries[f"create.ponder.{scene}.text_{i}"] = text
    return entries


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


def dump(path, data):
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def set_default(path, entries):
    """Adds or updates keys in a hand-written lang/default file, keeping its formatting."""
    text = path.read_text(encoding="utf-8")
    existing = json.loads(text)
    for k, v in entries.items():
        if k in existing and existing[k] != v:
            old = f"\t{json.dumps(k, ensure_ascii=False)}: {json.dumps(existing[k], ensure_ascii=False)}"
            assert old in text, f"cannot update {k} in {path}"
            text = text.replace(old, f"\t{json.dumps(k, ensure_ascii=False)}: {json.dumps(v, ensure_ascii=False)}")
    new = {k: v for k, v in entries.items() if k not in existing}
    if new:
        body = text.rstrip()
        assert body.endswith("}")
        body = body[:-1].rstrip()
        lines = ",\n".join(f"\t{json.dumps(k, ensure_ascii=False)}: {json.dumps(v, ensure_ascii=False)}"
                           for k, v in new.items())
        text = body + ",\n\n" + lines + "\n}\n"
    path.write_text(text, encoding="utf-8")
    json.loads(path.read_text(encoding="utf-8"))


ponder = ponder_entries()
missing = sorted(set(ponder.values()) - set(PONDER_PT))
if missing:
    sys.exit("apply-lang-phase2: no pt_br for ponder text(s): " + "; ".join(missing))

en, pt = load(EN), load(PT)
for key in REMOVE:
    en.pop(key, None)
    pt.pop(key, None)
for table in (NAMES, TOOLTIPS, INTERFACE):
    for key, (en_text, pt_text) in table.items():
        en[key] = en_text
        pt[key] = pt_text
for key, en_text in ponder.items():
    en[key] = en_text
    pt[key] = PONDER_PT[en_text]
dump(EN, en)
dump(PT, pt)
set_default(DEFAULT_TOOLTIPS, {k: v[0] for k, v in TOOLTIPS.items()})
set_default(DEFAULT_INTERFACE, {k: v[0] for k, v in INTERFACE.items()})
print(f"apply-lang-phase2: {len(NAMES) + len(TOOLTIPS) + len(INTERFACE) + len(ponder)} keys")
