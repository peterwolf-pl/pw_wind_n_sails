#!/usr/bin/env python3
import os
from PIL import Image, ImageDraw

os.makedirs("src/main/resources/assets/pw_wind_n_sails/textures/entity", exist_ok=True)
os.makedirs("src/main/resources/assets/pw_wind_n_sails/textures/item", exist_ok=True)
os.makedirs("src/main/resources/assets/pw_wind_n_sails", exist_ok=True)

# 1. Entity Texture (256x256)
img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# Oak / Spruce wood colors
WOOD_BASE = (142, 108, 64, 255)
WOOD_DARK = (118, 88, 50, 255)
WOOD_LIGHT = (168, 132, 82, 255)
WOOD_TRIM = (95, 70, 38, 255)

# Sailcloth canvas colors
SAIL_WHITE = (242, 238, 226, 255)
SAIL_SHADOW = (218, 212, 196, 255)
SAIL_LINE = (195, 188, 172, 255)
SAIL_EDGE = (180, 172, 155, 255)

# Iron hardware
IRON_DARK = (60, 62, 65, 255)
IRON_LIGHT = (140, 145, 150, 255)

# Fill entire entity sheet with basic wood tones then detail specific UV regions
# Region 1: Bottom floor (0,0 to 140, 48)
for y in range(0, 48):
    for x in range(0, 140):
        c = WOOD_BASE if (x + y // 4) % 8 != 0 else WOOD_DARK
        if x % 20 == 0:
            c = WOOD_TRIM
        img.putpixel((x, y), c)

# Region 2: Side walls (0, 48 to 180, 104)
for y in range(48, 104):
    for x in range(0, 180):
        c = WOOD_LIGHT if y % 6 in (0, 1) else WOOD_BASE
        if y == 48 or y == 103 or x == 0:
            c = WOOD_TRIM
        img.putpixel((x, y), c)

# Region 3: Bow, stern & benches (0, 104 to 140, 140)
for y in range(104, 140):
    for x in range(0, 140):
        c = WOOD_BASE if (x + y) % 6 != 0 else WOOD_LIGHT
        if x < 4 or y < 106:
            c = WOOD_TRIM
        img.putpixel((x, y), c)

# Mast & Top Cap (104, 0 to 135, 75)
for y in range(0, 75):
    for x in range(104, 135):
        c = WOOD_LIGHT if x % 3 != 0 else WOOD_BASE
        if y == 0 or y == 74:
            c = IRON_DARK
        img.putpixel((x, y), c)

# Boom spar (0, 140 to 80, 175)
for y in range(140, 175):
    for x in range(0, 80):
        c = WOOD_BASE if y % 2 == 0 else WOOD_LIGHT
        if x < 6:
            c = IRON_LIGHT
        img.putpixel((x, y), c)

# Rudder & tiller (90, 104 to 130, 150)
for y in range(104, 150):
    for x in range(90, 130):
        c = WOOD_DARK if x > 100 else WOOD_TRIM
        if y < 110:
            c = IRON_DARK
        img.putpixel((x, y), c)

# Mainsail cloth panels (0, 178 to 220, 256)
for y in range(178, 256):
    for x in range(0, 220):
        # Canvas weave texture
        c = SAIL_WHITE if (x + y) % 3 != 0 else SAIL_SHADOW
        # Seam stitch lines every 12 pixels
        if x % 14 == 0 or y % 14 == 0:
            c = SAIL_LINE
        if y == 178 or y == 255 or x == 0:
            c = SAIL_EDGE
        img.putpixel((x, y), c)

img.save("src/main/resources/assets/pw_wind_n_sails/textures/entity/sailboat.png")

# 2. Item Icon Texture (32x32)
item_img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
item_draw = ImageDraw.Draw(item_img)

# Wooden boat hull
# Hull base polygon
hull_poly = [(4, 24), (27, 24), (30, 20), (2, 20)]
item_draw.polygon(hull_poly, fill=WOOD_BASE, outline=WOOD_TRIM)
# Gunwale
item_draw.line([(2, 20), (30, 20)], fill=WOOD_LIGHT, width=1)
# Water ripple beneath
item_draw.line([(1, 26), (8, 26)], fill=(120, 200, 255, 200), width=1)
item_draw.line([(14, 27), (24, 27)], fill=(120, 200, 255, 180), width=1)

# Mast
item_draw.line([(13, 20), (13, 3)], fill=WOOD_DARK, width=2)
item_draw.line([(13, 3), (13, 2)], fill=IRON_LIGHT, width=1)

# Boom
item_draw.line([(13, 19), (28, 19)], fill=WOOD_DARK, width=1)

# Triangular mainsail
sail_poly = [(14, 5), (14, 18), (27, 18)]
item_draw.polygon(sail_poly, fill=SAIL_WHITE, outline=SAIL_SHADOW)
item_draw.line([(14, 11), (21, 18)], fill=SAIL_LINE, width=1)

# Rudder & tiller
item_draw.line([(4, 21), (3, 26)], fill=WOOD_TRIM, width=1)
item_draw.line([(4, 20), (7, 20)], fill=IRON_DARK, width=1)

item_img.save("src/main/resources/assets/pw_wind_n_sails/textures/item/sailboat.png")

# 3. Mod Icon (128x128)
icon_img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
icon_draw = ImageDraw.Draw(icon_img)

# Circular emblem background: Ocean gradient
for r in range(60, 0, -1):
    c_blue = int(25 + (60 - r) * 1.5)
    icon_draw.ellipse(
        [64 - r, 64 - r, 64 + r, 64 + r],
        fill=(20, c_blue, 110 + int((60 - r) * 1.8), 255)
    )

# Gold rim
icon_draw.ellipse([3, 3, 125, 125], outline=(230, 190, 80, 255), width=3)
icon_draw.ellipse([6, 6, 122, 122], outline=(150, 110, 40, 255), width=1)

# Wind streaks / breeze curves over water
icon_draw.arc([15, 20, 110, 50], start=190, end=350, fill=(200, 240, 255, 180), width=2)
icon_draw.arc([25, 35, 115, 65], start=180, end=340, fill=(240, 250, 255, 210), width=2)
icon_draw.arc([10, 75, 110, 105], start=10, end=170, fill=(130, 210, 255, 160), width=2)

# Sailing boat in center
# Hull
icon_draw.polygon([(26, 92), (96, 92), (106, 80), (20, 80)], fill=(140, 100, 60, 255), outline=(90, 60, 30, 255))
icon_draw.line([(20, 80), (106, 80)], fill=(175, 130, 80, 255), width=2)

# Mast & Boom
icon_draw.line([(52, 80), (52, 22)], fill=(90, 60, 30, 255), width=4)
icon_draw.line([(52, 78), (98, 78)], fill=(110, 75, 40, 255), width=3)

# Billowing Mainsail
sail_pts = [(55, 26), (55, 75), (96, 75)]
icon_draw.polygon(sail_pts, fill=(245, 242, 232, 255), outline=(210, 205, 190, 255))
icon_draw.line([(55, 42), (76, 75)], fill=(225, 220, 205, 255), width=2)
icon_draw.line([(55, 58), (88, 75)], fill=(225, 220, 205, 255), width=2)

# Water spray at bow
icon_draw.ellipse([98, 86, 112, 94], fill=(220, 245, 255, 200))
icon_draw.ellipse([16, 88, 28, 95], fill=(200, 235, 255, 180))

icon_img.save("src/main/resources/assets/pw_wind_n_sails/icon.png")

# 4. Flagpole Wood Texture (16x16)
pole_img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
for y in range(16):
    for x in range(16):
        c = WOOD_LIGHT if (x + y // 2) % 4 != 0 else WOOD_BASE
        if x in (0, 15) or y in (0, 15):
            c = WOOD_DARK
        pole_img.putpixel((x, y), c)
os.makedirs("src/main/resources/assets/pw_wind_n_sails/textures/block", exist_ok=True)
pole_img.save("src/main/resources/assets/pw_wind_n_sails/textures/block/flagpole.png")

# 5. Flag Entity Texture (128x64)
# Authentic maritime ensign: white & crimson field with navy trim & golden emblem
flag_img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
flag_draw = ImageDraw.Draw(flag_img)

# Top stripe crimson, bottom stripe white, navy hoist trim
CRIMSON = (196, 32, 48, 255)
WHITE = (248, 245, 238, 255)
NAVY = (24, 48, 92, 255)
GOLD = (235, 195, 60, 255)

flag_draw.rectangle([0, 0, 127, 31], fill=CRIMSON)
flag_draw.rectangle([0, 32, 127, 63], fill=WHITE)
# Hoist strip (sleeve attaching to mast)
flag_draw.rectangle([0, 0, 8, 63], fill=NAVY)
flag_draw.line([(8, 0), (8, 63)], fill=(40, 70, 125, 255), width=1)

# Central maritime gold anchor emblem
flag_draw.ellipse([34, 18, 54, 38], outline=GOLD, width=2)
flag_draw.line([(44, 20), (44, 48)], fill=GOLD, width=2)
flag_draw.line([(38, 28), (50, 28)], fill=GOLD, width=2)
flag_draw.arc([36, 36, 52, 48], start=0, end=180, fill=GOLD, width=2)

flag_img.save("src/main/resources/assets/pw_wind_n_sails/textures/entity/flag.png")

# 6. Flagpole Item Icon (32x32)
fp_item = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
fp_draw = ImageDraw.Draw(fp_item)

# Slender mast running from bottom left to top
fp_draw.line([(8, 30), (8, 2)], fill=WOOD_DARK, width=2)
fp_draw.line([(7, 2), (9, 2)], fill=GOLD, width=2) # Gold truck on top
fp_draw.rectangle([4, 28, 12, 31], fill=WOOD_BASE, outline=WOOD_TRIM) # Pedestal

# Flag flying out to the right
fp_draw.rectangle([9, 4, 27, 10], fill=CRIMSON)
fp_draw.rectangle([9, 11, 27, 16], fill=WHITE)
fp_draw.line([(9, 4), (9, 16)], fill=NAVY, width=1)

fp_item.save("src/main/resources/assets/pw_wind_n_sails/textures/item/flagpole.png")

print("Generated textures successfully!")
