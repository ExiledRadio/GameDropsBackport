"""Vanilla 26.3 oracle for abandoned camps (from Tricky Trials' trial chambers oracle).

python -I oracle.py <serverjar> <workdir> <seed> [regions]
Creates <workdir>/<seed>/, runs the vanilla server with that seed, force-loads every abandoned camp
candidate chunk of regions -R..R-1 (structure set abandoned_camp: random_spread 37/8, salt 91231127),
saves, stops, then writes <workdir>/<seed>.json with every saved abandoned_camp_* start. The self-test
(-Pselftest=camps with -Dgamedropsbackport.campsOracle=<that json>) lays each out and compares.
"""
import json
import os
import subprocess
import sys
import threading
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import nbtread  # noqa: E402

SPACING, SEPARATION, SALT = 37, 8, 91231127
M = (1 << 48) - 1


def jrandom(seed):
    return (seed ^ 0x5DEECE66D) & M


def nxt(state, bits):
    state = (state * 0x5DEECE66D + 0xB) & M
    r = state >> (48 - bits)
    if bits == 32 and r >= 1 << 31:
        r -= 1 << 32
    return state, r


def next_int(state, bound):
    if bound & (bound - 1) == 0:
        state, r = nxt(state, 31)
        return state, (bound * r) >> 31
    while True:
        state, bits = nxt(state, 31)
        val = bits % bound
        if bits - val + (bound - 1) < (1 << 31):
            return state, val


def candidate(seed, rx, rz):
    s = (rx * 341873128712 + rz * 132897987541 + seed + SALT) & ((1 << 64) - 1)
    st = jrandom(s)
    st, ox = next_int(st, SPACING - SEPARATION)
    st, oz = next_int(st, SPACING - SEPARATION)
    return rx * SPACING + ox, rz * SPACING + oz


def region_dir(d):
    """26.x keeps the overworld under dimensions/minecraft/overworld."""
    return os.path.join(d, 'world', 'dimensions', 'minecraft', 'overworld', 'region')


def main():
    jar, work, seed = sys.argv[1], sys.argv[2], int(sys.argv[3])
    radius = int(sys.argv[4]) if len(sys.argv) > 4 else 2
    d = os.path.join(work, str(seed))
    os.makedirs(d, exist_ok=True)
    with open(os.path.join(d, 'eula.txt'), 'w') as f:
        f.write('eula=true\n')
    with open(os.path.join(d, 'server.properties'), 'w') as f:
        f.write('level-seed=%d\nonline-mode=false\nserver-port=25991\nspawn-protection=0\n'
                'view-distance=2\nsimulation-distance=2\nlevel-name=world\nsync-chunk-writes=true\npause-when-empty-seconds=-1\n' % seed)
    cands = [(rx, rz) + candidate(seed, rx, rz) for rx in range(-radius, radius) for rz in range(-radius, radius)]
    parse_only = os.path.exists(region_dir(d))
    java = r'C:\Program Files\Java\jdk-25.0.3\bin\java.exe'
    if parse_only:
        return parse(d, work, seed, cands)
    p = subprocess.Popen([java, '-Xmx4G', '-jar', os.path.abspath(jar), 'nogui'], cwd=d, stdin=subprocess.PIPE,
                         stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)
    print('pid', p.pid, flush=True)
    lines = []
    done = threading.Event()

    def reader():
        for line in p.stdout:
            lines.append(line)
            if 'Done (' in line:
                done.set()
    threading.Thread(target=reader, daemon=True).start()
    done.wait(600)

    def cmd(c):
        p.stdin.write(c + '\n')
        p.stdin.flush()
    for rx, rz, cx, cz in cands:
        cmd('forceload add %d %d' % (cx * 16, cz * 16))
    time.sleep(90)
    cmd('save-all flush')
    time.sleep(20)
    cmd('stop')
    p.wait(300)
    with open(os.path.join(d, 'oracle-server.log'), 'w') as f:
        f.writelines(lines)
    parse(d, work, seed, cands)


def parse(d, work, seed, cands):
    out = {'seed': seed, 'candidates': [], 'starts': []}
    region = region_dir(d)
    for rx, rz, cx, cz in cands:
        out['candidates'].append([cx, cz])
        tag = nbtread.chunk(region, cx, cz)
        if tag is None:
            out['starts'].append({'chunk': [cx, cz], 'missing': True})
            continue
        starts = tag.get('structures', {}).get('starts', {})
        sid = next((k for k in starts if k.startswith('minecraft:abandoned_camp_')), None)
        s = starts.get(sid) if sid else None
        if s is None or s.get('id') == 'INVALID':
            out['starts'].append({'chunk': [cx, cz], 'valid': False, 'status': tag.get('Status')})
            continue
        pieces = []
        for c in s.get('Children', []):
            el = c.get('pool_element', {})
            pieces.append({'location': el.get('location'), 'type': el.get('element_type'),
                           'pos': [c.get('PosX'), c.get('PosY'), c.get('PosZ')], 'rotation': c.get('rotation'),
                           'ground': c.get('ground_level_delta'), 'bb': list(c.get('BB', []))})
        out['starts'].append({'chunk': [cx, cz], 'valid': True, 'structure': sid, 'pieces': pieces})
    with open(os.path.join(work, '%d.json' % seed), 'w') as f:
        json.dump(out, f, indent=1)
    print('wrote', sum(1 for s in out['starts'] if s.get('valid')), 'valid of', len(cands), flush=True)


if __name__ == '__main__':
    main()
