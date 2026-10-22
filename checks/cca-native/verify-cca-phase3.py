#!/usr/bin/env python3
"""Check the native, active CC&A phase-3 content inside a built Create JAR.

The upstream Harmful Plant is intentionally excluded: its 1.21.1 registration,
spread event, texture and data are all disabled/absent. See phase-3 checkpoint.
"""
import json
import sys
from zipfile import ZipFile

jar = sys.argv[1]
items = ('cake_base', 'cake_base_baked', 'biomass', 'biomass_pellet', 'electrum_amulet')
blocks = ('chocolate_cake', 'honey_cake', 'biomass_pellet_block')
fluids = ('seed_oil', 'bioethanol')
recipes = {
    'crafting': ('biomass_pallet_block', 'biomass_pellet_from_biomass_pallet_block', 'electrum_amulet'),
    'smoking': ('cake_base_baked',),
    'mixing': ('bioethanol', 'biomass_from_crops', 'biomass_from_flowers', 'biomass_from_honeycomb',
               'biomass_from_leaves', 'biomass_from_plant_foods', 'biomass_from_plants',
               'biomass_from_saplings', 'biomass_from_stricks'),
    'filling': ('vanilla_cake_from_base', 'chocolate_cake', 'honey_cake'),
    'compacting': ('cca_cake_base', 'biomass_pellet', 'seed_oil'),
}
with ZipFile(jar) as z:
    names = set(z.namelist())
    def require(path):
        assert path in names, f'missing {path}'
        return z.read(path)
    for name in items + blocks:
        require(f'assets/create/items/{name}.json')
        require(f'assets/create/models/item/{name}.json')
    for name in blocks:
        require(f'assets/create/blockstates/{name}.json')
        require(f'data/create/loot_table/blocks/{name}.json')
    for name in fluids:
        require(f'assets/create/items/{name}_bucket.json')
        require(f'assets/create/textures/fluid/{name}_still.png')
        require(f'assets/create/textures/fluid/{name}_flow.png')
    for kind, entries in recipes.items():
        for name in entries:
            path = f'data/create/recipe/{kind}/{name}.json'
            data = json.loads(require(path))
            assert 'createaddition:' not in json.dumps(data), path
            for ingredient in data.get('ingredients', []):
                assert not isinstance(ingredient, dict) or set(ingredient) == {'amount', 'ingredient'}, path
    for tag in ('plantoil', 'biofuel'):
        data = json.loads(require(f'data/c/tags/fluid/{tag}.json'))
        assert data['values'], tag
    lang = json.loads(require('assets/create/lang/en_us.json'))
    assert all(f'item.create.{name}' in lang for name in items)
    assert all(f'block.create.{name}' in lang for name in blocks)
print(f'phase3 active content OK: {len(items)} items, {len(blocks)} blocks, '
      f'{len(fluids)} fluids, {sum(map(len, recipes.values()))} recipes')
