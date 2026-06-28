export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/40 px-4 py-12">
      <div className="w-full max-w-md">
        <div className="mb-8 flex flex-col items-center gap-2 text-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary text-lg font-bold text-primary-foreground">
            Pe
          </div>
          <h1 className="text-2xl font-semibold tracking-tight">InvoicePe</h1>
          <p className="text-sm text-muted-foreground">
            GST invoices and payments, sorted.
          </p>
        </div>
        {children}
      </div>
    </div>
  );
}
