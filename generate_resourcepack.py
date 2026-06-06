#!/usr/bin/env python3
"""
NuclearCraft Resource Pack Generator
Generates a complete Minecraft resource pack with custom textures for all NuclearCraft items.
"""

from PIL import Image
import os, json, zipfile, shutil

# ─────────────────────────────────────────────
#  COLOR PALETTE
# ─────────────────────────────────────────────
_ = (0,   0,   0,   0)    # transparent
A = (0,   255, 65,  255)  # bright plutonium green
B = (0,   200, 50,  255)  # mid green
C = (0,   120, 20,  255)  # dark green
D = (0,   50,  5,   255)  # very dark green
E = (180, 255, 180, 255)  # green-white glow
F = (255, 215, 0,   255)  # bright hazmat yellow
G = (230, 170, 0,   255)  # mid yellow
H = (180, 110, 0,   255)  # dark yellow
I = (90,  55,  0,   255)  # very dark yellow/shadow
J = (210, 210, 210, 255)  # light metal
K = (155, 155, 155, 255)  # mid metal
L = (95,  95,  95,  255)  # dark metal
M = (45,  45,  45,  255)  # very dark metal
N = (255, 60,  60,  255)  # bright red
O = (160, 20,  20,  255)  # dark red
P = (140, 210, 255, 255)  # light blue
Q = (60,  110, 200, 255)  # mid blue
R = (255, 180, 210, 255)  # light pink
S = (210, 90,  140, 255)  # mid pink
T = (255, 255, 255, 255)  # white
U = (220, 220, 220, 255)  # light grey
V = (255, 130, 0,   255)  # orange
W = (190, 75,  0,   255)  # dark orange
X = (190, 110, 255, 255)  # light purple
Y = (110, 45,  195, 255)  # mid purple
Z = (40,  15,  80,  255)  # dark purple
br= (160, 90,  30,  255)  # brown (handle)
dk= (90,  50,  10,  255)  # dark brown (handle shadow)
cy= (0,   230, 220, 255)  # cyan
dc= (0,   140, 130, 255)  # dark cyan
gw= (200, 255, 200, 255)  # soft green glow

# ─────────────────────────────────────────────
#  TEXTURE DEFINITIONS  (16×16 pixel art)
#  Each row = one list of 16 color tuples
# ─────────────────────────────────────────────

def px(rows):
    """Convert a list-of-lists pixel definition into a 16x16 RGBA image."""
    img = Image.new("RGBA", (16, 16), (0,0,0,0))
    pixels = img.load()
    for y, row in enumerate(rows):
        for x, col in enumerate(row):
            pixels[x, y] = col
    return img

TEXTURES = {}

# ── RADIOACTIVE CORE ──────────────────────────────
TEXTURES["radioactive_core"] = px([
    [_,_,_,_,_,C,C,C,C,C,_,_,_,_,_,_],
    [_,_,_,C,B,B,A,A,B,B,C,_,_,_,_,_],
    [_,_,C,B,A,E,A,A,E,A,B,C,_,_,_,_],
    [_,C,B,A,A,A,A,A,A,A,A,B,C,_,_,_],
    [_,B,A,A,A,C,A,A,C,A,A,A,B,_,_,_],
    [C,B,A,A,C,C,B,B,C,C,A,A,B,C,_,_],
    [C,A,A,A,B,B,C,C,B,B,A,A,A,C,_,_],
    [C,A,A,A,C,C,B,B,C,C,A,A,A,C,_,_],
    [C,B,A,A,A,C,A,A,C,A,A,A,B,C,_,_],
    [_,B,A,A,A,A,A,A,A,A,A,A,B,_,_,_],
    [_,C,B,A,E,A,A,A,A,E,A,B,C,_,_,_],
    [_,_,C,B,B,A,A,A,A,B,B,C,_,_,_,_],
    [_,_,_,C,B,B,B,B,B,B,C,_,_,_,_,_],
    [_,_,_,_,C,C,C,C,C,C,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
])

# ── RAW PLUTONIUM FRAGMENT ─────────────────────────
TEXTURES["raw_plutonium_fragment"] = px([
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,C,C,B,_,_,_,_,_,_,_,_,_],
    [_,_,_,C,B,A,A,B,C,_,_,_,_,_,_,_],
    [_,_,C,B,A,A,B,A,A,B,_,_,_,_,_,_],
    [_,C,B,A,B,A,A,B,A,A,C,_,_,_,_,_],
    [_,C,A,L,A,B,A,A,B,A,B,C,_,_,_,_],
    [_,B,A,A,K,A,B,A,A,B,A,C,_,_,_,_],
    [_,C,A,B,A,A,L,A,B,A,A,C,_,_,_,_],
    [_,_,C,B,A,A,A,K,A,B,A,B,_,_,_,_],
    [_,_,_,C,B,A,A,A,A,A,B,C,_,_,_,_],
    [_,_,_,_,C,B,A,B,A,B,C,_,_,_,_,_],
    [_,_,_,_,_,C,B,A,B,C,_,_,_,_,_,_],
    [_,_,_,_,_,_,C,C,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
])

# ── REFINED PLUTONIUM INGOT ───────────────────────
TEXTURES["refined_plutonium_ingot"] = px([
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,C,C,C,C,C,C,C,C,_,_,_,_,_],
    [_,_,C,B,A,E,A,A,E,A,B,C,_,_,_,_],
    [_,_,B,A,A,A,A,A,A,A,A,B,_,_,_,_],
    [_,_,B,A,A,A,A,A,A,A,A,B,_,_,_,_],
    [_,_,B,A,A,A,A,A,A,A,A,B,_,_,_,_],
    [_,_,B,B,A,A,A,A,A,A,B,B,_,_,_,_],
    [_,_,_,B,B,B,A,A,B,B,B,_,_,_,_,_],
    [_,_,_,_,C,B,B,B,B,C,_,_,_,_,_,_],
    [_,_,_,_,D,C,C,C,C,D,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
])

# ── MUTATED SEED ──────────────────────────────────
TEXTURES["mutated_seed"] = px([
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,A,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,A,B,A,_,_,_,_,_,_,_],
    [_,_,_,_,_,A,B,A,B,A,_,_,_,_,_,_],
    [_,_,_,_,_,_,A,C,A,_,_,A,_,_,_,_],
    [_,_,_,_,_,_,_,C,_,_,A,B,A,_,_,_],
    [_,_,_,_,_,_,_,B,_,A,B,A,B,A,_,_],
    [_,_,_,_,_,_,_,B,_,_,A,C,A,_,_,_],
    [_,_,_,_,_,_,_,B,_,_,_,C,_,_,_,_],
    [_,_,_,_,_,_,C,B,C,_,_,B,_,_,_,_],
    [_,_,_,_,_,C,B,A,B,C,_,B,_,_,_,_],
    [_,_,_,_,C,B,A,A,A,B,C,C,_,_,_,_],
    [_,_,_,_,_,C,B,B,B,C,_,_,_,_,_,_],
    [_,_,_,_,_,_,C,C,C,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
])

# ── HEALING PETAL ─────────────────────────────────
TEXTURES["healing_petal"] = px([
    [_,_,_,_,_,_,R,R,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,R,S,S,R,_,_,_,_,_,_,_],
    [_,_,_,_,R,S,R,R,S,R,_,_,_,_,_,_],
    [_,_,_,R,S,R,R,R,R,S,R,_,_,_,_,_],
    [_,_,_,S,R,R,A,A,R,R,S,_,_,_,_,_],
    [_,_,R,S,R,A,B,B,A,R,S,R,_,_,_,_],
    [_,_,R,R,R,A,B,B,A,R,R,R,_,_,_,_],
    [_,_,R,S,R,A,B,B,A,R,S,R,_,_,_,_],
    [_,_,_,S,R,R,A,A,R,R,S,_,_,_,_,_],
    [_,_,_,R,S,R,R,R,R,S,R,_,_,_,_,_],
    [_,_,_,_,R,S,R,R,S,R,_,_,_,_,_,_],
    [_,_,_,_,_,R,S,S,R,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,R,R,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
])

# ── IRRADIATED HEART ─────────────────────────────
TEXTURES["irradiated_heart"] = px([
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,O,O,_,_,_,O,O,_,_,_,_,_,_,_],
    [_,O,N,N,O,_,O,N,N,O,_,_,_,_,_,_],
    [O,N,N,N,N,O,N,N,N,N,O,_,_,_,_,_],
    [O,N,A,N,N,N,N,A,N,N,O,_,_,_,_,_],
    [O,N,N,N,N,N,N,N,N,N,O,_,_,_,_,_],
    [_,O,N,N,N,N,N,N,N,O,_,_,_,_,_,_],
    [_,_,O,N,N,N,N,N,O,_,_,_,_,_,_,_],
    [_,_,_,O,N,N,N,O,_,_,_,_,_,_,_,_],
    [_,_,_,_,O,N,O,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,O,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
])

# ── TITAN CORE ────────────────────────────────────
TEXTURES["titan_core"] = px([
    [_,_,_,_,_,Y,X,X,Y,_,_,_,_,_,_,_],
    [_,_,_,Y,X,X,A,A,X,X,Y,_,_,_,_,_],
    [_,_,Y,X,A,E,A,A,E,A,X,Y,_,_,_,_],
    [_,Y,X,A,A,A,X,X,A,A,A,X,Y,_,_,_],
    [_,X,A,A,X,Y,Z,Z,Y,X,A,A,X,_,_,_],
    [Y,X,A,A,X,Z,A,A,Z,X,A,A,X,Y,_,_],
    [X,A,E,A,Z,A,E,E,A,Z,A,E,A,X,_,_],
    [X,A,E,A,Z,A,E,E,A,Z,A,E,A,X,_,_],
    [Y,X,A,A,X,Z,A,A,Z,X,A,A,X,Y,_,_],
    [_,X,A,A,X,Y,Z,Z,Y,X,A,A,X,_,_,_],
    [_,Y,X,A,A,A,X,X,A,A,A,X,Y,_,_,_],
    [_,_,Y,X,A,E,A,A,E,A,X,Y,_,_,_,_],
    [_,_,_,Y,X,X,A,A,X,X,Y,_,_,_,_,_],
    [_,_,_,_,_,Y,Y,Y,Y,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
])

# ── RADIATION DRILL (PICKAXE SHAPE) ───────────────
TEXTURES["radiation_drill"] = px([
    [_,_,_,_,_,_,_,_,A,B,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,A,C,B,A,_,_,_,_,_],
    [_,_,_,_,_,_,A,B,A,C,B,A,_,_,_,_],
    [_,_,_,_,_,A,B,A,B,A,C,B,E,_,_,_],
    [_,_,_,_,_,_,C,B,A,B,A,E,_,_,_,_],
    [_,_,_,_,_,_,_,C,B,A,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,J,K,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,J,K,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,J,K,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,J,K,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,J,br,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,J,br,dk,_,_,_,_,_,_,_,_,_,_,_],
    [_,J,br,dk,_,_,_,_,_,_,_,_,_,_,_,_],
    [J,br,dk,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [br,dk,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [dk,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
])

# ── PLUTONIUM SWORD ───────────────────────────────
TEXTURES["plutonium_sword"] = px([
    [_,_,_,_,_,_,_,_,_,_,_,_,_,E,A,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,E,A,B,_],
    [_,_,_,_,_,_,_,_,_,_,_,E,A,B,C,_],
    [_,_,_,_,_,_,_,_,_,_,E,A,B,C,_,_],
    [_,_,_,_,_,_,_,_,_,E,A,B,C,_,_,_],
    [_,_,_,_,_,_,_,_,E,A,B,C,_,_,_,_],
    [_,_,_,_,_,_,_,A,A,B,C,_,_,_,_,_],
    [_,_,_,_,_,_,A,J,J,C,_,_,_,_,_,_],
    [_,_,_,_,_,A,J,br,_,_,_,_,_,_,_,_],
    [_,_,_,_,J,J,br,_,_,_,_,_,_,_,_,_],
    [_,_,_,J,br,br,_,_,_,_,_,_,_,_,_,_],
    [_,_,J,br,dk,_,_,_,_,_,_,_,_,_,_,_],
    [_,J,br,dk,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,dk,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
])

# ── PLUTONIUM AXE ─────────────────────────────────
TEXTURES["plutonium_axe"] = px([
    [_,_,_,_,_,_,A,B,C,C,_,_,_,_,_,_],
    [_,_,_,_,_,A,B,A,B,B,C,_,_,_,_,_],
    [_,_,_,_,_,A,A,B,A,A,B,C,_,_,_,_],
    [_,_,_,_,A,B,A,B,A,B,A,B,_,_,_,_],
    [_,_,_,A,B,A,B,A,B,A,B,C,_,_,_,_],
    [_,_,_,_,A,B,A,B,A,B,C,_,_,_,_,_],
    [_,_,_,_,_,C,B,A,B,C,_,_,_,_,_,_],
    [_,_,_,_,_,_,J,K,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,J,K,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,J,K,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,J,br,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,J,br,dk,_,_,_,_,_,_,_,_,_,_,_],
    [_,J,br,dk,_,_,_,_,_,_,_,_,_,_,_,_],
    [J,br,dk,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [br,dk,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [dk,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
])

# ── PLUTONIUM PICKAXE ─────────────────────────────
TEXTURES["plutonium_pickaxe"] = px([
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,A,B,C,_,_,A,B,A,_,_,C,B,A,_,_],
    [_,B,A,B,A,A,B,A,B,A,A,B,A,B,_,_],
    [_,C,B,A,B,A,B,A,B,A,B,A,B,C,_,_],
    [_,_,C,B,A,B,A,A,A,B,A,B,C,_,_,_],
    [_,_,_,_,_,J,K,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,J,K,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,J,K,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,J,br,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,J,br,dk,_,_,_,_,_,_,_,_,_,_,_,_],
    [J,br,dk,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [br,dk,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [dk,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
])

# ── PLUTONIUM SHOVEL ──────────────────────────────
TEXTURES["plutonium_shovel"] = px([
    [_,_,_,_,_,_,_,C,C,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,C,B,B,C,_,_,_,_,_,_],
    [_,_,_,_,_,_,B,A,A,B,_,_,_,_,_,_],
    [_,_,_,_,_,_,B,A,A,B,_,_,_,_,_,_],
    [_,_,_,_,_,_,B,A,A,B,_,_,_,_,_,_],
    [_,_,_,_,_,_,C,B,B,C,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,J,K,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,J,K,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,J,K,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,J,K,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,J,br,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,J,br,dk,_,_,_,_,_,_,_,_,_,_,_],
    [_,J,br,dk,_,_,_,_,_,_,_,_,_,_,_,_],
    [J,br,dk,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [br,dk,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [dk,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
])

# ── PLUTONIUM HOE ─────────────────────────────────
TEXTURES["plutonium_hoe"] = px([
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,C,B,A,_,_,_,_,_,_],
    [_,_,_,_,_,_,C,B,A,B,C,_,_,_,_,_],
    [_,_,_,_,_,_,B,A,B,A,B,_,_,_,_,_],
    [_,_,_,_,_,_,C,B,C,_,B,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,C,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,J,K,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,J,K,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,J,K,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,J,K,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,J,br,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,J,br,dk,_,_,_,_,_,_,_,_,_,_],
    [_,_,J,br,dk,_,_,_,_,_,_,_,_,_,_,_],
    [_,J,br,dk,_,_,_,_,_,_,_,_,_,_,_,_],
    [J,br,dk,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [dk,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
])

# ── HAZMAT HELMET (item) ─────────────────────────
TEXTURES["hazmat_helmet"] = px([
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,I,H,H,H,H,H,H,I,_,_,_,_,_],
    [_,_,I,H,G,G,G,G,G,G,H,I,_,_,_,_],
    [_,_,H,G,F,F,F,F,F,F,G,H,_,_,_,_],
    [_,_,H,G,F,P,P,P,P,F,G,H,_,_,_,_],
    [_,_,H,G,F,P,Q,Q,P,F,G,H,_,_,_,_],
    [_,_,H,G,F,P,P,P,P,F,G,H,_,_,_,_],
    [_,_,H,G,F,F,F,F,F,F,G,H,_,_,_,_],
    [_,_,I,H,G,G,G,G,G,G,H,I,_,_,_,_],
    [_,_,_,I,H,H,H,H,H,H,I,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
])

# ── HAZMAT CHESTPLATE (item) ──────────────────────
TEXTURES["hazmat_chestplate"] = px([
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,I,H,I,_,_,_,_,I,H,I,_,_,_,_],
    [_,I,H,G,H,I,_,_,I,H,G,H,I,_,_,_],
    [_,H,G,F,G,H,I,I,H,G,F,G,H,_,_,_],
    [_,I,H,G,H,G,F,F,G,H,G,H,I,_,_,_],
    [_,_,I,H,G,F,F,F,F,G,H,I,_,_,_,_],
    [_,_,_,I,G,F,G,G,F,G,I,_,_,_,_,_],
    [_,_,_,I,G,F,G,G,F,G,I,_,_,_,_,_],
    [_,_,_,I,G,F,G,G,F,G,I,_,_,_,_,_],
    [_,_,_,I,G,F,G,G,F,G,I,_,_,_,_,_],
    [_,_,_,I,H,G,H,H,G,H,I,_,_,_,_,_],
    [_,_,_,_,I,H,I,I,H,I,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
])

# ── HAZMAT LEGGINGS (item) ────────────────────────
TEXTURES["hazmat_leggings"] = px([
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,I,H,G,F,F,G,H,I,_,_,_,_,_],
    [_,_,I,H,G,F,G,G,F,G,H,I,_,_,_,_],
    [_,_,H,G,F,G,H,H,G,F,G,H,_,_,_,_],
    [_,_,I,G,F,H,I,I,H,F,G,I,_,_,_,_],
    [_,_,_,I,H,I,_,_,I,H,I,_,_,_,_,_],
    [_,_,_,H,G,_,_,_,_,G,H,_,_,_,_,_],
    [_,_,_,G,F,_,_,_,_,F,G,_,_,_,_,_],
    [_,_,_,G,F,_,_,_,_,F,G,_,_,_,_,_],
    [_,_,_,H,G,_,_,_,_,G,H,_,_,_,_,_],
    [_,_,_,I,H,_,_,_,_,H,I,_,_,_,_,_],
    [_,_,_,_,I,_,_,_,_,I,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
])

# ── HAZMAT BOOTS (item) ───────────────────────────
TEXTURES["hazmat_boots"] = px([
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,I,H,I,_,_,_,I,H,I,_,_,_,_,_],
    [_,_,H,G,H,_,_,_,H,G,H,_,_,_,_,_],
    [_,_,G,F,G,_,_,_,G,F,G,_,_,_,_,_],
    [_,_,G,F,G,_,_,_,G,F,G,_,_,_,_,_],
    [_,_,G,F,G,_,_,_,G,F,G,_,_,_,_,_],
    [_,_,H,G,H,H,H,H,H,G,H,_,_,_,_,_],
    [_,_,I,H,G,G,G,G,G,H,I,_,_,_,_,_],
    [_,_,_,I,H,H,H,H,H,I,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
])

# ── PLUTONIUM HELMET (item) ──────────────────────
TEXTURES["plutonium_helmet"] = px([
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,D,C,C,C,C,C,C,D,_,_,_,_,_],
    [_,_,D,C,B,B,B,B,B,B,C,D,_,_,_,_],
    [_,_,C,B,A,A,A,A,A,A,B,C,_,_,_,_],
    [_,_,C,B,A,E,A,A,E,A,B,C,_,_,_,_],
    [_,_,C,B,A,A,A,A,A,A,B,C,_,_,_,_],
    [_,_,C,B,A,A,A,A,A,A,B,C,_,_,_,_],
    [_,_,C,B,A,A,A,A,A,A,B,C,_,_,_,_],
    [_,_,D,C,B,B,B,B,B,B,C,D,_,_,_,_],
    [_,_,_,D,C,C,C,C,C,C,D,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
])

# ── PLUTONIUM CHESTPLATE (item) ───────────────────
TEXTURES["plutonium_chestplate"] = px([
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,D,C,D,_,_,_,_,D,C,D,_,_,_,_],
    [_,D,C,B,C,D,_,_,D,C,B,C,D,_,_,_],
    [_,C,B,A,B,C,D,D,C,B,A,B,C,_,_,_],
    [_,D,C,B,C,B,A,A,B,C,B,C,D,_,_,_],
    [_,_,D,C,B,A,E,E,A,B,C,D,_,_,_,_],
    [_,_,_,D,B,A,B,B,A,B,D,_,_,_,_,_],
    [_,_,_,D,B,A,B,B,A,B,D,_,_,_,_,_],
    [_,_,_,D,B,A,B,B,A,B,D,_,_,_,_,_],
    [_,_,_,D,B,A,B,B,A,B,D,_,_,_,_,_],
    [_,_,_,D,C,B,C,C,B,C,D,_,_,_,_,_],
    [_,_,_,_,D,C,D,D,C,D,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
])

# ── PLUTONIUM LEGGINGS (item) ─────────────────────
TEXTURES["plutonium_leggings"] = px([
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,D,C,B,A,A,B,C,D,_,_,_,_,_],
    [_,_,D,C,B,A,B,B,A,B,C,D,_,_,_,_],
    [_,_,C,B,A,B,C,C,B,A,B,C,_,_,_,_],
    [_,_,D,B,A,C,D,D,C,A,B,D,_,_,_,_],
    [_,_,_,D,C,D,_,_,D,C,D,_,_,_,_,_],
    [_,_,_,C,B,_,_,_,_,B,C,_,_,_,_,_],
    [_,_,_,B,A,_,_,_,_,A,B,_,_,_,_,_],
    [_,_,_,B,A,_,_,_,_,A,B,_,_,_,_,_],
    [_,_,_,C,B,_,_,_,_,B,C,_,_,_,_,_],
    [_,_,_,D,C,_,_,_,_,C,D,_,_,_,_,_],
    [_,_,_,_,D,_,_,_,_,D,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
])

# ── PLUTONIUM BOOTS (item) ────────────────────────
TEXTURES["plutonium_boots"] = px([
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,D,C,D,_,_,_,D,C,D,_,_,_,_,_],
    [_,_,C,B,C,_,_,_,C,B,C,_,_,_,_,_],
    [_,_,B,A,B,_,_,_,B,A,B,_,_,_,_,_],
    [_,_,B,A,B,_,_,_,B,A,B,_,_,_,_,_],
    [_,_,B,A,B,_,_,_,B,A,B,_,_,_,_,_],
    [_,_,C,B,C,C,C,C,C,B,C,_,_,_,_,_],
    [_,_,D,C,B,B,B,B,B,C,D,_,_,_,_,_],
    [_,_,_,D,C,C,C,C,C,D,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
])

# ── PLUTONIUM ARROW ───────────────────────────────
TEXTURES["plutonium_arrow"] = px([
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,A,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,A,B,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,A,B,C,_],
    [_,_,_,_,_,_,_,_,_,_,_,A,C,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,J,K,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,J,K,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,J,K,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,J,K,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,J,K,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,J,K,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,J,K,J,_,_,_,_,_,_,_,_,_],
    [_,_,_,J,K,J,K,_,_,_,_,_,_,_,_,_],
    [_,_,J,K,_,K,_,_,_,_,_,_,_,_,_,_],
    [_,J,K,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [J,K,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [K,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
])

# ── INDUSTRIAL FABRIC ─────────────────────────────
TEXTURES["industrial_fabric"] = px([
    [I,H,I,H,I,H,I,H,I,H,I,H,I,H,I,H],
    [H,F,G,F,G,F,G,F,G,F,G,F,G,F,G,F],
    [I,G,F,G,F,G,F,G,F,G,F,G,F,G,F,I],
    [H,F,G,F,G,F,G,F,G,F,G,F,G,F,G,H],
    [I,H,I,H,I,H,I,H,I,H,I,H,I,H,I,H],
    [H,F,G,F,G,F,G,F,G,F,G,F,G,F,G,F],
    [I,G,F,G,F,G,F,G,F,G,F,G,F,G,F,I],
    [H,F,G,F,G,F,G,F,G,F,G,F,G,F,G,H],
    [I,H,I,H,I,H,I,H,I,H,I,H,I,H,I,H],
    [H,F,G,F,G,F,G,F,G,F,G,F,G,F,G,F],
    [I,G,F,G,F,G,F,G,F,G,F,G,F,G,F,I],
    [H,F,G,F,G,F,G,F,G,F,G,F,G,F,G,H],
    [I,H,I,H,I,H,I,H,I,H,I,H,I,H,I,H],
    [H,F,G,F,G,F,G,F,G,F,G,F,G,F,G,F],
    [I,G,F,G,F,G,F,G,F,G,F,G,F,G,F,I],
    [H,F,G,F,G,F,G,F,G,F,G,F,G,F,G,H],
])

# ── RADIATION ANTIDOTE ────────────────────────────
TEXTURES["radiation_antidote"] = px([
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,K,K,K,K,_,_,_,_,_,_,_],
    [_,_,_,_,K,L,L,L,L,K,_,_,_,_,_,_],
    [_,_,_,_,_,A,B,A,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,B,C,B,_,_,_,_,_,_,_,_],
    [_,_,_,_,C,B,A,B,C,_,_,_,_,_,_,_],
    [_,_,_,C,B,A,E,A,B,C,_,_,_,_,_,_],
    [_,_,_,B,A,E,A,E,A,B,_,_,_,_,_,_],
    [_,_,_,B,A,A,A,A,A,B,_,_,_,_,_,_],
    [_,_,_,B,A,A,A,A,A,B,_,_,_,_,_,_],
    [_,_,_,C,B,A,A,A,B,C,_,_,_,_,_,_],
    [_,_,_,_,C,B,B,B,C,_,_,_,_,_,_,_],
    [_,_,_,_,_,C,C,C,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
])

# ── RADIATION SERUM ───────────────────────────────
TEXTURES["radiation_serum"] = px([
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,K,K,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,L,K,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,B,A,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,A,B,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,C,B,A,C,_,_,_,_,_,_,_,_],
    [_,_,_,C,B,A,E,B,C,_,_,_,_,_,_,_],
    [_,_,_,B,A,E,A,A,B,_,_,_,_,_,_,_],
    [_,_,_,B,A,A,A,A,B,_,_,_,_,_,_,_],
    [_,_,_,B,A,A,A,A,B,_,_,_,_,_,_,_],
    [_,_,_,C,B,A,A,B,C,_,_,_,_,_,_,_],
    [_,_,_,_,C,B,B,C,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,C,C,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
    [_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_],
])

# ─────────────────────────────────────────────
#  ARMOR LAYER TEXTURES (64×32)
#  Simplified solid-colour suits with subtle detail
# ─────────────────────────────────────────────

def make_armor_texture(body_color, dark_color, shadow_color, accent_color, size=(64,32)):
    """Generate a Minecraft-layout armor layer texture."""
    img = Image.new("RGBA", size, (0,0,0,0))
    px2 = img.load()
    w, h = size
    # Fill entire image with body color (where Minecraft samples)
    for y in range(h):
        for x in range(w):
            px2[x,y] = body_color
    # Add accent lines to simulate texture
    for y in range(0, h, 4):
        for x in range(w):
            if px2[x,y] == body_color:
                px2[x,y] = dark_color
    for y in range(1, h, 8):
        for x in range(w):
            px2[x,y] = accent_color
    # Shadow edge
    for y in range(h):
        px2[0,y] = shadow_color
        px2[w-1,y] = shadow_color
    for x in range(w):
        px2[x,0] = shadow_color
        px2[x,h-1] = shadow_color
    return img

# Hazmat suit: yellow (layer 1 = helmet+chest+boots, layer 2 = legs)
hazmat_layer1 = make_armor_texture(
    body_color=(230,170,0,255),
    dark_color=(180,110,0,255),
    shadow_color=(90,55,0,255),
    accent_color=(255,215,0,255)
)
hazmat_layer2 = make_armor_texture(
    body_color=(200,140,0,255),
    dark_color=(150,90,0,255),
    shadow_color=(80,50,0,255),
    accent_color=(240,190,0,255)
)

# Plutonium armor: glowing green
plutonium_layer1 = make_armor_texture(
    body_color=(0,150,30,255),
    dark_color=(0,90,15,255),
    shadow_color=(0,40,5,255),
    accent_color=(0,220,50,255)
)
plutonium_layer2 = make_armor_texture(
    body_color=(0,130,25,255),
    dark_color=(0,80,12,255),
    shadow_color=(0,35,5,255),
    accent_color=(0,200,45,255)
)

# ─────────────────────────────────────────────
#  MODEL JSON DEFINITIONS
# ─────────────────────────────────────────────

def handheld_model(texture_path):
    return {"parent": "minecraft:item/handheld", "textures": {"layer0": texture_path}}

def flat_model(texture_path):
    return {"parent": "minecraft:item/generated", "textures": {"layer0": texture_path}}

def armor_model(texture_path):
    return {"parent": "minecraft:item/generated", "textures": {"layer0": texture_path}}

# Custom item models (the actual item visuals)
CUSTOM_MODELS = {
    # Materials
    "radioactive_core":          flat_model("minecraft:item/nuclearcraft/radioactive_core"),
    "raw_plutonium_fragment":    flat_model("minecraft:item/nuclearcraft/raw_plutonium_fragment"),
    "refined_plutonium_ingot":   flat_model("minecraft:item/nuclearcraft/refined_plutonium_ingot"),
    "mutated_seed":              flat_model("minecraft:item/nuclearcraft/mutated_seed"),
    "healing_petal":             flat_model("minecraft:item/nuclearcraft/healing_petal"),
    "irradiated_heart":          flat_model("minecraft:item/nuclearcraft/irradiated_heart"),
    "titan_core":                flat_model("minecraft:item/nuclearcraft/titan_core"),
    "industrial_fabric":         flat_model("minecraft:item/nuclearcraft/industrial_fabric"),
    # Tools
    "radiation_drill":           handheld_model("minecraft:item/nuclearcraft/radiation_drill"),
    "plutonium_sword":           handheld_model("minecraft:item/nuclearcraft/plutonium_sword"),
    "plutonium_axe":             handheld_model("minecraft:item/nuclearcraft/plutonium_axe"),
    "plutonium_pickaxe":         handheld_model("minecraft:item/nuclearcraft/plutonium_pickaxe"),
    "plutonium_shovel":          handheld_model("minecraft:item/nuclearcraft/plutonium_shovel"),
    "plutonium_hoe":             handheld_model("minecraft:item/nuclearcraft/plutonium_hoe"),
    # Armor items (inventory look)
    "hazmat_helmet":             armor_model("minecraft:item/nuclearcraft/hazmat_helmet"),
    "hazmat_chestplate":         armor_model("minecraft:item/nuclearcraft/hazmat_chestplate"),
    "hazmat_leggings":           armor_model("minecraft:item/nuclearcraft/hazmat_leggings"),
    "hazmat_boots":              armor_model("minecraft:item/nuclearcraft/hazmat_boots"),
    "plutonium_helmet":          armor_model("minecraft:item/nuclearcraft/plutonium_helmet"),
    "plutonium_chestplate":      armor_model("minecraft:item/nuclearcraft/plutonium_chestplate"),
    "plutonium_leggings":        armor_model("minecraft:item/nuclearcraft/plutonium_leggings"),
    "plutonium_boots":           armor_model("minecraft:item/nuclearcraft/plutonium_boots"),
    # Projectile / consumables
    "plutonium_arrow":           flat_model("minecraft:item/nuclearcraft/plutonium_arrow"),
    "radiation_antidote":        flat_model("minecraft:item/nuclearcraft/radiation_antidote"),
    "radiation_serum":           flat_model("minecraft:item/nuclearcraft/radiation_serum"),
}

# Base vanilla item overrides — maps base item → list of (CMD, model_id)
OVERRIDES = {
    "magma_cream":         [(1101, "radioactive_core")],
    "prismarine_crystals": [(1102, "raw_plutonium_fragment")],
    "echo_shard":          [(1103, "refined_plutonium_ingot")],
    "wheat_seeds":         [(1104, "mutated_seed")],
    "pink_petals":         [(1105, "healing_petal")],
    "heart_of_the_sea":    [(1106, "irradiated_heart")],
    "nether_star":         [(1107, "titan_core")],
    "diamond_pickaxe":     [(1108, "radiation_drill")],
    "netherite_sword":     [(1301, "plutonium_sword")],
    "netherite_axe":       [(1302, "plutonium_axe")],
    "netherite_pickaxe":   [(1303, "plutonium_pickaxe")],
    "netherite_shovel":    [(1304, "plutonium_shovel")],
    "netherite_hoe":       [(1305, "plutonium_hoe")],
    "leather_helmet":      [(1306, "hazmat_helmet")],
    "leather_chestplate":  [(1307, "hazmat_chestplate")],
    "leather_leggings":    [(1308, "hazmat_leggings")],
    "leather_boots":       [(1309, "hazmat_boots")],
    "netherite_helmet":    [(1310, "plutonium_helmet")],
    "netherite_chestplate":[(1311, "plutonium_chestplate")],
    "netherite_leggings":  [(1312, "plutonium_leggings")],
    "netherite_boots":     [(1313, "plutonium_boots")],
    "arrow":               [(1314, "plutonium_arrow")],
    "yellow_wool":         [(1315, "industrial_fabric")],
    "honey_bottle":        [(1316, "radiation_antidote")],
    "glass_bottle":        [(1317, "radiation_serum")],
}

def build_override_model(base_item, overrides_list):
    """Build a vanilla item model JSON with CMD override predicates."""
    base = {
        "parent": f"minecraft:item/{base_item}",
        "overrides": [
            {
                "predicate": {"custom_model_data": cmd},
                "model": f"minecraft:item/nuclearcraft/{model_id}"
            }
            for cmd, model_id in overrides_list
        ]
    }
    return base

# ─────────────────────────────────────────────
#  PACK.MCMETA
# ─────────────────────────────────────────────

PACK_MCMETA = {
    "pack": {
        "pack_format": 34,
        "description": "§aNuclearCraft §7Resource Pack §8| §eCustom items, armor & tools"
    }
}

# ─────────────────────────────────────────────
#  LANG FILE
# ─────────────────────────────────────────────

LANG = {
    "item.minecraft.magma_cream.custom_1101":          "Radioactive Core",
    "item.minecraft.prismarine_crystals.custom_1102":  "Raw Plutonium Fragment",
    "item.minecraft.echo_shard.custom_1103":           "Refined Plutonium Ingot",
    "item.minecraft.wheat_seeds.custom_1104":          "Mutated Seed",
    "item.minecraft.pink_petals.custom_1105":          "Healing Petal",
    "item.minecraft.heart_of_the_sea.custom_1106":     "Irradiated Heart",
    "item.minecraft.nether_star.custom_1107":          "Titan Core",
    "item.minecraft.diamond_pickaxe.custom_1108":      "Radiation Drill",
    "item.minecraft.netherite_sword.custom_1301":      "Plutonium Sword",
    "item.minecraft.netherite_axe.custom_1302":        "Plutonium Axe",
    "item.minecraft.netherite_pickaxe.custom_1303":    "Plutonium Pickaxe",
    "item.minecraft.netherite_shovel.custom_1304":     "Plutonium Shovel",
    "item.minecraft.netherite_hoe.custom_1305":        "Plutonium Hoe",
    "item.minecraft.leather_helmet.custom_1306":       "Hazmat Helmet",
    "item.minecraft.leather_chestplate.custom_1307":   "Hazmat Chestplate",
    "item.minecraft.leather_leggings.custom_1308":     "Hazmat Leggings",
    "item.minecraft.leather_boots.custom_1309":        "Hazmat Boots",
    "item.minecraft.netherite_helmet.custom_1310":     "Plutonium Helmet",
    "item.minecraft.netherite_chestplate.custom_1311": "Plutonium Chestplate",
    "item.minecraft.netherite_leggings.custom_1312":   "Plutonium Leggings",
    "item.minecraft.netherite_boots.custom_1313":      "Plutonium Boots",
    "item.minecraft.arrow.custom_1314":                "Plutonium Arrow",
    "item.minecraft.yellow_wool.custom_1315":          "Industrial Fabric",
    "item.minecraft.honey_bottle.custom_1316":         "Radiation Antidote",
    "item.minecraft.glass_bottle.custom_1317":         "Radiation Serum",
}

# ─────────────────────────────────────────────
#  BUILD RESOURCE PACK
# ─────────────────────────────────────────────

OUT_DIR = "NuclearCraft_ResourcePack"
if os.path.exists(OUT_DIR):
    shutil.rmtree(OUT_DIR)

# Directory layout
ITEM_TEX_DIR   = f"{OUT_DIR}/assets/minecraft/textures/item/nuclearcraft"
ARMOR_TEX_DIR  = f"{OUT_DIR}/assets/minecraft/textures/models/armor"
ITEM_MOD_DIR   = f"{OUT_DIR}/assets/minecraft/models/item/nuclearcraft"
BASE_MOD_DIR   = f"{OUT_DIR}/assets/minecraft/models/item"
LANG_DIR       = f"{OUT_DIR}/assets/minecraft/lang"

for d in [ITEM_TEX_DIR, ARMOR_TEX_DIR, ITEM_MOD_DIR, BASE_MOD_DIR, LANG_DIR]:
    os.makedirs(d, exist_ok=True)

# 1) Write item textures
for name, img in TEXTURES.items():
    img.save(f"{ITEM_TEX_DIR}/{name}.png")
print(f"✅  {len(TEXTURES)} item textures written")

# 2) Write armor textures
hazmat_layer1.save(f"{ARMOR_TEX_DIR}/hazmat_layer_1.png")
hazmat_layer2.save(f"{ARMOR_TEX_DIR}/hazmat_layer_2.png")
plutonium_layer1.save(f"{ARMOR_TEX_DIR}/plutonium_layer_1.png")
plutonium_layer2.save(f"{ARMOR_TEX_DIR}/plutonium_layer_2.png")
print("✅  4 armor layer textures written")

# 3) Write custom item model JSONs
for name, model_data in CUSTOM_MODELS.items():
    with open(f"{ITEM_MOD_DIR}/{name}.json", "w") as f:
        json.dump(model_data, f, indent=2)
print(f"✅  {len(CUSTOM_MODELS)} custom item model JSONs written")

# 4) Write base vanilla override model JSONs
for base_item, overrides_list in OVERRIDES.items():
    model_data = build_override_model(base_item, overrides_list)
    with open(f"{BASE_MOD_DIR}/{base_item}.json", "w") as f:
        json.dump(model_data, f, indent=2)
print(f"✅  {len(OVERRIDES)} vanilla override model JSONs written")

# 5) Write lang file
with open(f"{LANG_DIR}/en_us.json", "w") as f:
    json.dump(LANG, f, indent=2)
print("✅  Language file written")

# 6) Write pack.mcmeta
with open(f"{OUT_DIR}/pack.mcmeta", "w") as f:
    json.dump(PACK_MCMETA, f, indent=2)
print("✅  pack.mcmeta written")

# 7) Generate a simple pack icon (pack.png) — 64x64 green radiation symbol
def make_pack_icon():
    img = Image.new("RGBA", (64, 64), (10, 10, 10, 255))
    pxl = img.load()
    # Draw a simple radiation triskelion symbol in green
    cx, cy = 32, 32
    import math
    for angle_offset in [0, 120, 240]:
        for r in range(8, 26):
            for a_deg in range(20, 100):
                a = math.radians(a_deg + angle_offset)
                x = int(cx + r * math.cos(a))
                y = int(cy + r * math.sin(a))
                if 0 <= x < 64 and 0 <= y < 64:
                    pxl[x, y] = (0, 220, 50, 255)
    # Central circle
    for r in range(5):
        for angle in range(360):
            a = math.radians(angle)
            x = int(cx + r * math.cos(a))
            y = int(cy + r * math.sin(a))
            if 0 <= x < 64 and 0 <= y < 64:
                pxl[x, y] = (0, 255, 65, 255)
    # Outer ring
    for angle in range(360):
        a = math.radians(angle)
        x = int(cx + 28 * math.cos(a))
        y = int(cy + 28 * math.sin(a))
        if 0 <= x < 64 and 0 <= y < 64:
            pxl[x, y] = (0, 180, 40, 255)
    return img

make_pack_icon().save(f"{OUT_DIR}/pack.png")
print("✅  pack.png icon written")

# 8) Zip it all up
ZIP_NAME = "NuclearCraft_ResourcePack.zip"
if os.path.exists(ZIP_NAME):
    os.remove(ZIP_NAME)
with zipfile.ZipFile(ZIP_NAME, "w", zipfile.ZIP_DEFLATED) as zf:
    for root, dirs, files in os.walk(OUT_DIR):
        for file in files:
            full = os.path.join(root, file)
            arcname = os.path.relpath(full, OUT_DIR)
            zf.write(full, arcname)
print(f"✅  {ZIP_NAME} created")

# Print summary
total_files = sum(len(f) for _, _, f in os.walk(OUT_DIR))
print(f"\n📦  Resource pack summary:")
print(f"    Item textures:      {len(TEXTURES)} PNG files (16×16)")
print(f"    Armor textures:     4 PNG files (64×32)")
print(f"    Custom models:      {len(CUSTOM_MODELS)} JSON files")
print(f"    Vanilla overrides:  {len(OVERRIDES)} JSON files")
print(f"    Total files:        {total_files}")
print(f"    Output:             {ZIP_NAME}")
