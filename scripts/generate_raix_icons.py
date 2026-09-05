import os
from PIL import Image, ImageDraw
from collections import deque

def extract_clean_shield():
    img = Image.open('branding/logo-app.png').convert('RGB')
    crop_x1, crop_y1, crop_x2, crop_y2 = 330, 250, 690, 580
    cropped = img.crop((crop_x1, crop_y1, crop_x2, crop_y2)).convert('RGBA')
    cw, ch = cropped.size

    visited = set()
    q = deque([(0, 0), (cw-1, 0), (0, ch-1), (cw-1, ch-1)])
    for pt in q:
        visited.add(pt)

    while q:
        x, y = q.popleft()
        for dx, dy in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = x + dx, y + dy
            if 0 <= nx < cw and 0 <= ny < ch and (nx, ny) not in visited:
                nr, ng, nb, _ = cropped.getpixel((nx, ny))
                is_gold_or_bright = (nr > 70 or ng > 65 or nb > 75)
                if not is_gold_or_bright:
                    visited.add((nx, ny))
                    q.append((nx, ny))

    for y in range(ch):
        for x in range(cw):
            if (x, y) in visited:
                cropped.putpixel((x, y), (0, 0, 0, 0))

    bbox = cropped.getbbox()
    return cropped.crop(bbox)

def generate_icons():
    shield = extract_clean_shield()
    bg_color = (11, 19, 37, 255) # #0B1325 (Azul Marinho Profundo)

    # 1. Android Mipmap densities
    densities = {
        'mipmap-mdpi': 48,
        'mipmap-hdpi': 72,
        'mipmap-xhdpi': 96,
        'mipmap-xxhdpi': 144,
        'mipmap-xxxhdpi': 192,
    }

    base_res = 'composeApp/src/androidMain/res'

    for folder, size in densities.items():
        dir_path = os.path.join(base_res, folder)
        os.makedirs(dir_path, exist_ok=True)

        # A. Square/rounded ic_launcher.webp
        square_img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        draw_sq = ImageDraw.Draw(square_img)
        corner_r = int(size * 0.18)
        draw_sq.rounded_rectangle([(0, 0), (size-1, size-1)], radius=corner_r, fill=bg_color)

        shield_scale = size * 0.70
        sw = int(shield_scale * (shield.width / max(shield.width, shield.height)))
        sh = int(shield_scale * (shield.height / max(shield.width, shield.height)))
        scaled_shield = shield.resize((sw, sh), Image.Resampling.LANCZOS)

        pos_x = (size - sw) // 2
        pos_y = (size - sh) // 2
        square_img.paste(scaled_shield, (pos_x, pos_y), scaled_shield)
        square_img.save(os.path.join(dir_path, 'ic_launcher.webp'), 'WEBP', quality=100)

        # B. Round ic_launcher_round.webp
        round_img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        draw_rd = ImageDraw.Draw(round_img)
        draw_rd.ellipse([(0, 0), (size-1, size-1)], fill=bg_color)
        round_img.paste(scaled_shield, (pos_x, pos_y), scaled_shield)
        round_img.save(os.path.join(dir_path, 'ic_launcher_round.webp'), 'WEBP', quality=100)
        print(f"Generated {folder}: {size}x{size}")

    # 2. Adaptive Icon Foreground
    fg_size = 432
    fg_img = Image.new('RGBA', (fg_size, fg_size), (0, 0, 0, 0))
    fg_shield_scale = 260
    fsw = int(fg_shield_scale * (shield.width / max(shield.width, shield.height)))
    fsh = int(fg_shield_scale * (shield.height / max(shield.width, shield.height)))
    scaled_fg = shield.resize((fsw, fsh), Image.Resampling.LANCZOS)
    fpos_x = (fg_size - fsw) // 2
    fpos_y = (fg_size - fsh) // 2
    fg_img.paste(scaled_fg, (fpos_x, fpos_y), scaled_fg)

    drawable_dir = os.path.join(base_res, 'drawable')
    os.makedirs(drawable_dir, exist_ok=True)
    fg_img.save(os.path.join(drawable_dir, 'ic_launcher_foreground.png'), 'PNG')
    print("Generated adaptive ic_launcher_foreground.png (432x432)")

    # 3. Desktop resources
    desktop_res = 'composeApp/src/desktopMain/resources'
    os.makedirs(desktop_res, exist_ok=True)

    desk_size = 256
    desk_img = Image.new('RGBA', (desk_size, desk_size), (0, 0, 0, 0))
    desk_draw = ImageDraw.Draw(desk_img)
    desk_draw.rounded_rectangle([(0, 0), (desk_size-1, desk_size-1)], radius=46, fill=bg_color)
    d_scale = 180
    dsw = int(d_scale * (shield.width / max(shield.width, shield.height)))
    dsh = int(d_scale * (shield.height / max(shield.width, shield.height)))
    scaled_desk = shield.resize((dsw, dsh), Image.Resampling.LANCZOS)
    desk_img.paste(scaled_desk, ((desk_size - dsw) // 2, (desk_size - dsh) // 2), scaled_desk)
    desk_img.save(os.path.join(desktop_res, 'icon.png'), 'PNG')

    ico_sizes = [(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)]
    desk_img.save(os.path.join(desktop_res, 'icon.ico'), format='ICO', sizes=ico_sizes)
    print("Generated desktop icon.png and icon.ico (multi-size)")

if __name__ == '__main__':
    generate_icons()
