"use client";

import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";
import type { AuthUser } from "@/types";

/**
 * Plain localStorage key that holds just the raw JWT. lib/api.ts reads this
 * directly so the axios interceptors never have to import the store (avoids a
 * client/SSR import cycle). It is kept in sync with the persisted store below.
 */
export const TOKEN_KEY = "token";

interface AuthState {
  token: string | null;
  user: AuthUser | null;
  isAuthenticated: boolean;
  hasHydrated: boolean;
  login: (token: string, user: AuthUser) => void;
  logout: () => void;
  setHasHydrated: (value: boolean) => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      isAuthenticated: false,
      hasHydrated: false,

      login: (token, user) => {
        if (typeof window !== "undefined") {
          window.localStorage.setItem(TOKEN_KEY, token);
        }
        set({ token, user, isAuthenticated: true });
      },

      logout: () => {
        if (typeof window !== "undefined") {
          window.localStorage.removeItem(TOKEN_KEY);
        }
        set({ token: null, user: null, isAuthenticated: false });
      },

      setHasHydrated: (value) => set({ hasHydrated: value }),
    }),
    {
      name: "invoicepe-auth",
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        token: state.token,
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
      onRehydrateStorage: () => (state) => {
        // Keep the standalone token key in sync after rehydration.
        if (state?.token && typeof window !== "undefined") {
          window.localStorage.setItem(TOKEN_KEY, state.token);
        }
        state?.setHasHydrated(true);
      },
    }
  )
);
