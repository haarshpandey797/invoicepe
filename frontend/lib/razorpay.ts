// Razorpay Checkout helper: lazy-loads checkout.js and exposes minimal types
// so we can open the subscription checkout without pulling in `any`.

const CHECKOUT_SRC = "https://checkout.razorpay.com/v1/checkout.js";

export interface RazorpaySuccessResponse {
  razorpay_payment_id: string;
  razorpay_subscription_id: string;
  razorpay_signature: string;
}

export interface RazorpayCheckoutOptions {
  key: string;
  subscription_id: string;
  name: string;
  description?: string;
  image?: string;
  prefill?: { name?: string; email?: string; contact?: string };
  theme?: { color?: string };
  handler?: (response: RazorpaySuccessResponse) => void;
  modal?: { ondismiss?: () => void };
}

interface RazorpayInstance {
  open: () => void;
}

type RazorpayConstructor = new (options: RazorpayCheckoutOptions) => RazorpayInstance;

declare global {
  // eslint-disable-next-line no-var
  interface Window {
    Razorpay?: RazorpayConstructor;
  }
}

/** Injects the Razorpay checkout script once; resolves true when ready. */
export function loadRazorpayCheckout(): Promise<boolean> {
  return new Promise((resolve) => {
    if (typeof window === "undefined") {
      resolve(false);
      return;
    }
    if (window.Razorpay) {
      resolve(true);
      return;
    }
    const existing = document.querySelector<HTMLScriptElement>(
      `script[src="${CHECKOUT_SRC}"]`
    );
    if (existing) {
      existing.addEventListener("load", () => resolve(true));
      existing.addEventListener("error", () => resolve(false));
      return;
    }
    const script = document.createElement("script");
    script.src = CHECKOUT_SRC;
    script.async = true;
    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);
    document.body.appendChild(script);
  });
}

export function openRazorpayCheckout(options: RazorpayCheckoutOptions): boolean {
  if (typeof window === "undefined" || !window.Razorpay) {
    return false;
  }
  new window.Razorpay(options).open();
  return true;
}
