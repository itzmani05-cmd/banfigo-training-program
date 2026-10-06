import { useState } from 'react';
import { money } from '../format';

// Grouped bars: money in vs money out per month. Colors are validated for color-blind separation.
const WIDTH = 760;
const HEIGHT = 260;
const MARGIN = { top: 12, right: 8, bottom: 28, left: 56 };
const PLOT_W = WIDTH - MARGIN.left - MARGIN.right;
const PLOT_H = HEIGHT - MARGIN.top - MARGIN.bottom;
const BAR_GAP = 2;

const SERIES = [
  { key: 'moneyIn', label: 'Money in', color: 'var(--series-in)' },
  { key: 'moneyOut', label: 'Money out', color: 'var(--series-out)' },
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

  const values = flows.map((f) => ({ ...f, moneyIn: Number(f.moneyIn), moneyOut: Number(f.moneyOut) }));
  const max = niceMax(Math.max(0, ...values.flatMap((f) => [f.moneyIn, f.moneyOut])));
  const isEmpty = values.every((f) => f.moneyIn === 0 && f.moneyOut === 0);
  // With no data, a scale would be meaningless: draw just the baseline
  const ticks = isEmpty ? [0] : [0, 0.25, 0.5, 0.75, 1].map((t) => t * max);
  const y = (v) => MARGIN.top + PLOT_H - (v / max) * PLOT_H;

  const groupW = PLOT_W / values.length;
  const barW = Math.min(28, (groupW * 0.6 - BAR_GAP) / 2);

  return (
    <div className="viz-root">
      <div className="legend">
        {SERIES.map((s) => (
          <span key={s.key} className="legend-item">
            <span className="legend-swatch" style={{ background: s.color }} />
            {s.label}
          </span>
        ))}
      </div>

      <div className="chart-wrap">
        <svg viewBox={`0 0 ${WIDTH} ${HEIGHT}`} role="img" aria-label="Money in and money out per month">
          {ticks.map((t) => (
            <g key={t}>
              <line
                x1={MARGIN.left} x2={WIDTH - MARGIN.right} y1={y(t)} y2={y(t)}
                stroke={t === 0 ? 'var(--axis)' : 'var(--grid)'} strokeWidth="1"
              />
              <text x={MARGIN.left - 8} y={y(t)} dy="0.32em" textAnchor="end" className="axis-label">
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
                  const h = (f[s.key] / max) * PLOT_H;
                  if (h <= 0) return null;
                  const x = j === 0 ? center - BAR_GAP / 2 - barW : center + BAR_GAP / 2;
                  return (
                    <path
                      key={s.key}
                      d={barPath(x, MARGIN.top + PLOT_H - h, barW, h)}
                      fill={s.color}
                      opacity={hovered === null || hovered === i ? 1 : 0.45}
                    />
                  );
                })}
                <text x={center} y={HEIGHT - 8} textAnchor="middle" className="axis-label">
                  {monthLabel(f.month, false)}
                </text>
                {/* Hit target covers the whole month column, larger than the bars */}
                <rect
                  x={groupX} y={MARGIN.top} width={groupW} height={PLOT_H}
                  fill="transparent"
                  onMouseEnter={() => setHovered(i)}
                  onMouseLeave={() => setHovered(null)}
                />
              </g>
            );
          })}
        </svg>

        {isEmpty && <p className="chart-empty">No transactions in the last {values.length} months.</p>}

        {hovered !== null && (
          <div
            className="chart-tooltip"
            style={{ left: `${((MARGIN.left + (hovered + 0.5) * groupW) / WIDTH) * 100}%` }}
          >
            <strong>{monthLabel(values[hovered].month, true)}</strong>
            {SERIES.map((s) => (
              <div key={s.key} className="tooltip-row">
                <span className="legend-swatch" style={{ background: s.color }} />
                <span>{s.label}</span>
                <span className="tooltip-value">{money(values[hovered][s.key])}</span>
              </div>
            ))}
          </div>
        )}
      </div>

      <details>
        <summary>Show as table</summary>
        <table>
          <thead>
            <tr>
              <th>Month</th>
              <th>Money in</th>
              <th>Money out</th>
            </tr>
          </thead>
          <tbody>
            {values.map((f) => (
              <tr key={f.month}>
                <td>{monthLabel(f.month, true)}</td>
                <td>{money(f.moneyIn)}</td>
                <td>{money(f.moneyOut)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </details>
    </div>
  );
}
