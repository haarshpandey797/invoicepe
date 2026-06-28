import { redirect } from "next/navigation";

export default function RootPage() {
  // The (dashboard) layout guard bounces unauthenticated users to /login.
  redirect("/dashboard");
}
