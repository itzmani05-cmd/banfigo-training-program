import { useEffect, useRef, useState } from 'react';
import { money } from '../format';
import { Table, Td } from './ui';

// Grouped bars: money in vs money out per month. Colors are validated for color-blind separation.
// The SVG is drawn at the container's real pixel width (not scaled), so labels stay 12px on phones.
const MARGIN = { top: 12, right: 8, bottom: 28, left: 48 };
const BAR_GAP = 2;

// Tracks an element's width; starts at a desktop-ish default until the first measurement
function useWidth(ref, initial = 760) {
  const [width, setWidth] = useState(initial);
  useEffect(() => {
    const observer = new ResizeObserver(([entry]) => setWidth(Math.round(entry.contentRect.width)));
    observer.observe(ref.current);
    return () => observer.disconnect();
  }, [ref]);
  return width;
}

const SERIES = [
  { key: 'moneyIn', label: 'Money in', color: 'var(--color-series-in)' },
  { key: 'moneyOut', label: 'Money out', color: 'var(--color-series-out)' },
];

const compact = new Intl.NumberFormat('en', { notation: 'compact', maximumFractionDigits: 2 });
const monthLabel = (ym, withYear) => {
  const [y, m] = ym.split('-').map(Number);
  return new Date(y, m - 1, 1).toLocaleString('en', withYear ? { month: 'short', year: 'numeric' } : { month: 'short' });
};

// Rounds the axis maximum up to 1, 2 or 5 x 10^n so tick labels stay readable
const niceMax = (v) => {
  if (v <= 0) return 1;
  const exp = 10 ** Math.floor(Math.log10(v));
  const f = v / exp;
  return (f <= 1 ? 1 : f <= 2 ? 2 : f <= 5 ? 5 : 10) * exp;
};

// Bar with 4px rounded top, square bottom on the baseline
const barPath = (x, y, w, h) => {
  const r = Math.min(4, w / 2, h);
  return `M${x},${y + h} V${y + r} Q${x},${y} ${x + r},${y} H${x + w - r} Q${x + w},${y} ${x + w},${y + r} V${y + h} Z`;
};

export default function MonthlyFlowChart({ flows }) {
  const [hovered, setHovered] = useState(null);
  const wrapRef = useRef(null);
  const width = Math.max(useWidth(wrapRef), 260);
  const height = width < 500 ? 200 : 260;
  const plotW = width - MARGIN.left - MARGIN.right;
  const plotH = height - MARGIN.top - MARGIN.bottom;

  const values = flows.map((f) => ({ ...f, moneyIn: Number(f.moneyIn), moneyOut: Number(f.moneyOut) }));
  const max = niceMax(Math.max(0, ...values.flatMap((f) => [f.moneyIn, f.moneyOut])));
  const isEmpty = values.every((f) => f.moneyIn === 0 && f.moneyOut === 0);
  // With no data, a scale would be meaningless: draw just the baseline
  const ticks = isEmpty ? [0] : [0, 0.25, 0.5, 0.75, 1].map((t) => t * max);
  const y = (v) => MARGIN.top + plotH - (v / max) * plotH;

  const groupW = plotW / values.length;
  const barW = Math.min(28, (groupW * 0.6 - BAR_GAP) / 2);

  return (
    <div>
      <div className="mb-2 flex gap-4 text-label">
        {SERIES.map((s) => (
          <span key={s.key} className="inline-flex items-center gap-1.5">
            <span className="inline-block size-3 rounded-sm" style={{ background: s.color }} />
            {s.label}
          </span>
        ))}
      </div>

      <div className="relative" ref={wrapRef}>
        <svg viewBox={`0 0 ${width} ${height}`} className="block h-auto w-full" role="img" aria-label="Money in and money out per month">
          {ticks.map((t) => (
            <g key={t}>
              <line
                x1={MARGIN.left} x2={width - MARGIN.right} y1={y(t)} y2={y(t)}
                stroke={t === 0 ? 'var(--color-axis)' : 'var(--color-line)'} strokeWidth="1"
              />
              <text x={MARGIN.left - 8} y={y(t)} dy="0.32em" textAnchor="end" className="fill-subtle text-caption">
                {compact.format(t)}
              </text>
            </g>
          ))}

          {values.map((f, i) => {
            const groupX = MARGIN.left + i * groupW;
            const center = groupX + groupW / 2;
            return (
              <g key={f.month}>
                {SERIES.map((s, j) => {
                  const h = (f[s.key] / max) * plotH;
                  if (h <= 0) return null;
                  const x = j === 0 ? center - BAR_GAP / 2 - barW : center + BAR_GAP / 2;
                  return (
                    <path
                      key={s.key}
                      d={barPath(x, MARGIN.top + plotH - h, barW, h)}
                      fill={s.color}
                      opacity={hovered === null || hovered === i ? 1 : 0.45}
                    />
                  );
                })}
                <text x={center} y={height - 8} textAnchor="middle" className="fill-subtle text-caption">
                  {monthLabel(f.month, false)}
                </text>
                {/* Hit target covers the whole month column, larger than the bars */}
                <rect
                  x={groupX} y={MARGIN.top} width={groupW} height={plotH}
                  fill="transparent"
                  onMouseEnter={() => setHovered(i)}
                  onMouseLeave={() => setHovered(null)}
                />
              </g>
            );
          })}
        </svg>

        {isEmpty && <p className="pointer-events-none absolute inset-0 m-0 flex items-center justify-center text-muted italic">No transactions in the last {values.length} months.</p>}

        {hovered !== null && (
          <div
            className="pointer-events-none absolute top-0 -translate-x-1/2 rounded-md border border-line bg-paper px-3 py-2 text-label whitespace-nowrap shadow-md"
            style={{ left: `${((MARGIN.left + (hovered + 0.5) * groupW) / width) * 100}%` }}
          >
            <strong>{monthLabel(values[hovered].month, true)}</strong>
            {SERIES.map((s) => (
              <div key={s.key} className="mt-1 flex items-center gap-1.5">
                <span className="inline-block size-3 rounded-sm" style={{ background: s.color }} />
                <span>{s.label}</span>
                <span className="ml-auto pl-3 font-bold tabular-nums">{money(values[hovered][s.key])}</span>
              </div>
            ))}
          </div>
        )}
      </div>

      <details>
        <summary className="mt-3 cursor-pointer text-label text-muted hover:text-ink">Show as table</summary>
        <div className="mt-3">
          <Table columns={['Month', '>Money in', '>Money out']}>
            {values.map((f) => (
              <tr key={f.month}>
                <Td>{monthLabel(f.month, true)}</Td>
                <Td className="text-right tabular-nums">{money(f.moneyIn)}</Td>
                <Td className="text-right tabular-nums">{money(f.moneyOut)}</Td>
              </tr>
            ))}
          </Table>
        </div>
      </details>
    </div>
  );
}
