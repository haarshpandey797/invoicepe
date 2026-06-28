/**
 * Client-side GST preview that mirrors the backend GstCalculator:
 *   - intra-state (same first 2 GSTIN digits): CGST 9% + SGST 9%
 *   - inter-state: IGST 18%
 *   - missing/short GSTIN: defaults to intra-state
 * The backend remains the source of truth; this is only for the live preview.
 */

export const INTRA_RATE = 9;
export const INTER_RATE = 18;

export interface GstBreakdown {
  subtotal: number;
  cgstRate: number;
  sgstRate: number;
  igstRate: number;
  cgstAmount: number;
  sgstAmount: number;
  igstAmount: number;
  total: number;
  interState: boolean;
}

function round2(value: number): number {
  return Math.round((value + Number.EPSILON) * 100) / 100;
}

export function isInterState(
  userGstin?: string | null,
  clientGstin?: string | null
): boolean {
  const u = (userGstin ?? "").trim();
  const c = (clientGstin ?? "").trim();
  if (u.length < 2 || c.length < 2) return false; // default intra-state
  return u.slice(0, 2) !== c.slice(0, 2);
}

export function calculateGst(
  subtotal: number,
  userGstin?: string | null,
  clientGstin?: string | null
): GstBreakdown {
  const base = round2(subtotal);
  if (isInterState(userGstin, clientGstin)) {
    const igst = round2((base * INTER_RATE) / 100);
    return {
      subtotal: base,
      cgstRate: 0,
      sgstRate: 0,
      igstRate: INTER_RATE,
      cgstAmount: 0,
      sgstAmount: 0,
      igstAmount: igst,
      total: round2(base + igst),
      interState: true,
    };
  }
  const cgst = round2((base * INTRA_RATE) / 100);
  const sgst = round2((base * INTRA_RATE) / 100);
  return {
    subtotal: base,
    cgstRate: INTRA_RATE,
    sgstRate: INTRA_RATE,
    igstRate: 0,
    cgstAmount: cgst,
    sgstAmount: sgst,
    igstAmount: 0,
    total: round2(base + cgst + sgst),
    interState: false,
  };
}
