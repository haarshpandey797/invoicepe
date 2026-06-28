"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Plus, Loader2, Mail, Phone } from "lucide-react";
import { api, getApiErrorMessage } from "@/lib/api";
import type { ApiResponse, Client } from "@/types";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";

const clientSchema = z.object({
  name: z.string().min(1, "Name is required"),
  email: z.string().email("Enter a valid email").optional().or(z.literal("")),
  phone: z
    .string()
    .regex(/^[0-9]{10,15}$/, "Phone must be 10–15 digits")
    .optional()
    .or(z.literal("")),
  gstin: z.string().optional().or(z.literal("")),
  billingAddress: z.string().optional().or(z.literal("")),
});

type ClientForm = z.infer<typeof clientSchema>;

export default function ClientsPage() {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);

  const { data, isLoading } = useQuery({
    queryKey: ["clients"],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<Client[]>>("/api/clients");
      return data.data;
    },
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ClientForm>({ resolver: zodResolver(clientSchema) });

  const createMutation = useMutation({
    mutationFn: (values: ClientForm) =>
      api.post("/api/clients", {
        name: values.name,
        email: values.email || undefined,
        phone: values.phone || undefined,
        gstin: values.gstin || undefined,
        billingAddress: values.billingAddress || undefined,
      }),
    onSuccess: () => {
      toast.success("Client added");
      setOpen(false);
      reset();
      queryClient.invalidateQueries({ queryKey: ["clients"] });
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">
          People and businesses you invoice.
        </p>
        <Button onClick={() => setOpen(true)}>
          <Plus className="h-4 w-4" />
          Add Client
        </Button>
      </div>

      <Card>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="space-y-3 p-6">
              {Array.from({ length: 4 }).map((_, i) => (
                <Skeleton key={i} className="h-12 w-full" />
              ))}
            </div>
          ) : !data || data.length === 0 ? (
            <p className="py-16 text-center text-sm text-muted-foreground">
              No clients yet. Add your first client to start invoicing.
            </p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Contact</TableHead>
                  <TableHead>GSTIN</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.map((client) => (
                  <TableRow key={client.id}>
                    <TableCell className="font-medium">{client.name}</TableCell>
                    <TableCell>
                      <div className="flex flex-col gap-1 text-sm text-muted-foreground">
                        {client.email && (
                          <span className="flex items-center gap-1.5">
                            <Mail className="h-3 w-3" />
                            {client.email}
                          </span>
                        )}
                        {client.phone && (
                          <span className="flex items-center gap-1.5">
                            <Phone className="h-3 w-3" />
                            {client.phone}
                          </span>
                        )}
                        {!client.email && !client.phone && "—"}
                      </div>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {client.gstin ?? "—"}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Add client</DialogTitle>
            <DialogDescription>
              Add a client you can bill on invoices.
            </DialogDescription>
          </DialogHeader>
          <form
            onSubmit={handleSubmit((v) => createMutation.mutate(v))}
            className="space-y-4"
          >
            <div className="space-y-2">
              <Label htmlFor="name">Name</Label>
              <Input id="name" placeholder="Acme Pvt Ltd" {...register("name")} />
              {errors.name && (
                <p className="text-xs text-destructive">{errors.name.message}</p>
              )}
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input id="email" type="email" {...register("email")} />
                {errors.email && (
                  <p className="text-xs text-destructive">{errors.email.message}</p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="phone">Phone</Label>
                <Input id="phone" placeholder="9876543210" {...register("phone")} />
                {errors.phone && (
                  <p className="text-xs text-destructive">{errors.phone.message}</p>
                )}
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="gstin">GSTIN</Label>
              <Input id="gstin" placeholder="22AAAAA0000A1Z5" {...register("gstin")} />
              {errors.gstin && (
                <p className="text-xs text-destructive">{errors.gstin.message}</p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="billingAddress">Billing address</Label>
              <Textarea id="billingAddress" {...register("billingAddress")} />
            </div>
            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => setOpen(false)}
              >
                Cancel
              </Button>
              <Button type="submit" disabled={createMutation.isPending}>
                {createMutation.isPending && (
                  <Loader2 className="h-4 w-4 animate-spin" />
                )}
                Add client
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
