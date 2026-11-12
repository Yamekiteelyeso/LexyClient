from PIL import Image, ImageDraw, ImageFilter
import random, math, io, cairosvg
from pathlib import Path
root=Path(__file__).parent/'src/main/resources/assets/lexy-client/textures/gui';root.mkdir(parents=True,exist_ok=True)
# 32 original simple vector glyphs, all with consistent 1.8px round strokes.
G={
0:'<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>',
1:'<path d="M4 3l17 17M20 3L3 20M5 2l3 7-5-1M19 2l-3 7 5-1M5 22l4-4M19 22l-4-4"/>',
2:'<circle cx="15" cy="4" r="2"/><path d="m12 9-3 4 5 3 3 5M12 9l6 2 3-3M9 13l-5 3M14 16l-5 6"/>',
3:'<circle cx="12" cy="7.5" r="3.5"/><path d="M4 21v-2c0-4 3-6 8-6s8 2 8 6v2"/>',
4:'<path d="M2 12c3-5 7-7 10-7s7 2 10 7c-3 5-7 7-10 7s-7-2-10-7Z"/><circle cx="12" cy="12" r="3"/>',
5:'<rect x="2.5" y="3" width="19" height="18" rx="2"/><path d="M2.5 9h19M9 9v12M9 15h12"/>',
6:'<path d="M4 18a10 10 0 1 1 16 0"/><path d="m12 15 5-6"/><circle cx="12" cy="15" r="1.4"/><path d="M5 21h14"/>',
7:'<path d="M14 5a6 6 0 0 0-8 8L2 17l5 5 4-4a6 6 0 0 0 8-8l-4 4-5-5 4-4Z"/>',
8:'<path d="M10 17V3l11-2v14"/><ellipse cx="7" cy="18" rx="3" ry="2"/><ellipse cx="18" cy="16" rx="3" ry="2"/>',
9:'<path d="m12 2 9 9-9 11-9-11z"/><path d="m3 11h18M12 2l4 9-4 11-4-11z"/>',
10:'<rect x="3" y="5" width="15" height="15" rx="2"/><path d="M8 2h13v15M6 9h9M6 13h7"/>',
11:'<path d="M4 7h16M4 17h16"/><circle cx="10" cy="7" r="2.5" fill="#121925"/><circle cx="16" cy="17" r="2.5" fill="#121925"/>',
12:'<path d="M12 2 21 6v6c0 5-4 8-9 10-5-2-9-5-9-10V6z"/><path d="m8 12 3 3 5-6"/>',
13:'<rect x="4" y="3" width="16" height="18" rx="7"/><path d="M12 3v7M4 11h16"/>',
14:'<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="4"/><path d="M12 1v5M12 18v5M1 12h5M18 12h5"/>',
15:'<circle cx="12" cy="12" r="9"/><path d="M12 5v14M5 12h14"/><circle cx="12" cy="12" r="3"/>',
16:'<circle cx="10" cy="10" r="6"/><path d="m14.5 14.5 6.5 6.5"/>',
17:'<path d="m2 18 6-8 4 4 5-8 5 3"/><path d="M16 6h6v6"/>',
18:'<path d="M3 4h18v16H3zM3 9h18M9 4v16"/><path d="m12 13 3 2-3 2"/>',
19:'<path d="m12 2 8 4v12l-8 4-8-4V6zM4 6l8 5 8-5M12 11v11"/>',
20:'<path d="M12 22s8-8 8-13a8 8 0 0 0-16 0c0 5 8 13 8 13Z"/><circle cx="12" cy="9" r="2.5"/>',
21:'<path d="M4 5h16v14H4zM7 9h2M11 9h2M15 9h2M7 13h10M7 17h2M11 17h2M15 17h2"/>',
22:'<path d="M2 6h20v12H2zM7 6V4h10v2M6 11h4M14 11h4M6 15h12"/>',
23:'<path d="M4 4h16v12H4zM8 20h8M12 16v4"/>',
24:'<path d="M5 12h14M12 5v14M5 5l14 14M19 5 5 19"/>',
25:'<path d="M12 3v3M12 18v3M3 12h3M18 12h3M5.5 5.5l2 2M16.5 16.5l2 2M18.5 5.5l-2 2M7.5 16.5l-2 2"/><circle cx="12" cy="12" r="4"/>',
26:'<path d="m12 2 3 7 8 1-6 5 2 8-7-4-7 4 2-8-6-5 8-1z"/>',
27:'<path d="M5 4h14v16H5zM8 7h8v3H8zM8 14h8M8 17h4"/>',
28:'<path d="M5 7h14v13H5zM8 3h8v4M9 11h6M12 9v5M8 17h8"/>',
29:'<path d="M4 5h16M4 10h16M4 15h16M4 20h11"/>',
30:'<path d="M12 3a9 9 0 1 0 9 9M12 3v9l6-6"/>',
31:'<path d="M8 4H4v16h16v-5M11 4h9v8h-9zM12 8h7M9 14l3 3 5-6"/>'}
canvas=Image.new('RGBA',(8*32,4*32),(0,0,0,0))
for i in range(32):
    body=G[i]
    svg=f'<svg xmlns="http://www.w3.org/2000/svg" width="80" height="80" viewBox="0 0 24 24"><g stroke="#EAF3FF" fill="none" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">{body}</g></svg>'
    data=cairosvg.svg2png(bytestring=svg.encode(),output_width=80,output_height=80)
    im=Image.open(io.BytesIO(data)).convert('RGBA').resize((27,27),Image.Resampling.LANCZOS)
    x=(i%8)*32+2;y=(i//8)*32+2;canvas.alpha_composite(im,(x,y))
canvas.save(root/'icons.png')
# Original Lexy prism logo. Dark transparent mark and blue/white crystalline lattice.
logo=Image.new('RGBA',(256,256),(0,0,0,0));d=ImageDraw.Draw(logo)
# Rounded dark acrylic tile.
d.rounded_rectangle((7,7,249,249),radius=54,fill=(12,23,37,255),outline=(69,130,191,125),width=2)
d.polygon([(127,29),(213,78),(213,177),(127,226),(41,177),(41,78)],fill=(17,43,68,255),outline=(61,132,211,255),width=6)
d.line([(127,43),(127,203)],fill=(109,184,247,130),width=4)
d.line([(58,88),(127,129),(196,88)],fill=(109,184,247,170),width=5)
d.line([(63,168),(127,205),(191,168)],fill=(109,184,247,140),width=5)
d.line([(80,85),(80,167),(146,167)],fill=(239,247,255,255),width=17,joint='curve')
d.line([(167,83),(167,132),(127,156)],fill=(71,161,255,255),width=17,joint='curve')
logo.save(root/'logo.png')
# Original Minecraft-inspired atmospheric backdrop: procedural terrain silhouettes, no borrowed screenshot.
W,H=1600,900
im=Image.new('RGB',(W,H));pix=im.load(); random.seed(112)
for y in range(H):
    t=y/H
    bg=(int(4+5*t),int(14+20*t),int(27+30*t))
    for x in range(W):
        glow=math.exp(-((x-W*.72)/(W*.26))**2-((y-H*.40)/(H*.42))**2)*15
        pix[x,y]=(int(bg[0]+glow*.3),int(bg[1]+glow*.65),int(bg[2]+glow))
d=ImageDraw.Draw(im,'RGBA')
# Moon lighting and fog.
for R in range(195,30,-5):
 a=max(0,int(17*(1-R/210)))
 d.ellipse((1190-R,195-R,1190+R,195+R),fill=(112,178,237,a))
d.ellipse((1152,157,1228,233),fill=(181,210,235,195))
# Pixelated mountain layers with rectangular terraced ridges.
random.seed(19)
for layer,(base,alpha,color) in enumerate([(540,215,(21,53,67)),(610,220,(12,36,52)),(730,255,(6,24,34))]):
    pts=[(0,H)]; heights=[]
    step=36 if layer==0 else 24
    for x in range(0,W+step,step):
        y=base+int(math.sin(x/110+layer*2.0)*50+math.sin(x/47+layer)*17+random.randrange(-9,10))
        heights.append((x,y));pts.append((x,y))
    pts.append((W,H));d.polygon(pts,fill=(*color,alpha))
    # blocky cliff overhangs
    for x,y in heights:
        if random.random()<.18:d.rectangle((x-12,y-15,x+12,y+19),fill=(*color,alpha))
# Foreground stylized trees.
random.seed(25)
for x in range(-30,W+50,65):
    h=random.randrange(55,170);yy=H-110+random.randrange(-20,30)
    d.rectangle((x-4,yy-h//2,x+8,yy+20),fill=(8,22,30,230))
    for lev in range(4):
        r=int((45-lev*10)*(h/110));cy=yy-h+lev*19
        d.polygon([(x-r,cy+33),(x,cy-8),(x+r,cy+33)],fill=(10+lev,35+lev*2,44+lev*2,225))
# central readability overlay
veil=Image.new('RGBA',(W,H),(0,0,0,0));vd=ImageDraw.Draw(veil)
for y in range(H):
 a=int(10+58*max(0,1-abs(y/H-.5)*2));vd.line((0,y,W,y),fill=(2,6,12,a))
im=Image.alpha_composite(im.convert('RGBA'),veil)
im=im.filter(ImageFilter.GaussianBlur(12))
# soft noise / vignette
px=im.load();import numpy as np
arr=np.asarray(im.convert('RGB')).astype(np.float32)
yy,xx=np.ogrid[:H,:W];distance=((xx-W*.5)/(W*.85))**2+((yy-H*.5)/(H*.84))**2
f=np.clip(1.0-distance*.67,.26,1.0)[...,None];arr*=f
im=Image.fromarray(np.uint8(np.clip(arr,0,255)),'RGB')
im.save(root/'backdrop.png',optimize=True)
print('assets',[(p.name,p.stat().st_size) for p in root.glob('*.png')])

# Lexy 4.2 Mono: keep generated resources strictly black, white and grey.
# This final pass is required when regenerating artwork from the original vectors.
for asset in root.glob('*.png'):
    rgb = Image.open(asset).convert('RGBA')
    alpha = rgb.getchannel('A')
    grey = rgb.convert('L')
    if asset.name == 'backdrop.png':
        grey = grey.point(lambda y: round(y * .64))
    mono = Image.merge('RGBA',(grey, grey, grey, alpha))
    mono.save(asset, optimize=True)