import { z } from 'zod'

export const vehicleStatuses = ['ACTIVE', 'MAINTENANCE', 'RETIRED'] as const

export const vehicleFormSchema = z.object({
  plateNumber: z
    .string()
    .transform((s) => s.trim().toUpperCase())
    .pipe(
      z
        .string()
        .regex(/^[A-Z0-9-]+$/, 'Только латиница A–Z, цифры и дефис'),
    ),
  brand: z.string().min(1, 'Обязательное поле'),
  model: z.string().min(1, 'Обязательное поле'),
  capacityKg: z.preprocess(
    (val) =>
      val === '' || val === null || val === undefined ? undefined : val,
    z.union([z.undefined(), z.coerce.number().int().min(0)]),
  ),
  status: z.enum(vehicleStatuses),
})

export type VehicleFormValues = z.infer<typeof vehicleFormSchema>
