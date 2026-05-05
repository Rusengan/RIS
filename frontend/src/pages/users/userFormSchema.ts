import { z } from 'zod'

import type { RoleCode } from '../../types/role'

export const roleCodes = ['DRIVER', 'DISPATCHER', 'ADMIN'] as const satisfies readonly RoleCode[]

export const userCreateSchema = z.object({
  email: z.string().min(1, 'Обязательное поле').email('Некорректный email'),
  fullName: z.string().min(1, 'Обязательное поле'),
  roles: z
    .array(z.enum(roleCodes))
    .min(1, 'Выберите хотя бы одну роль'),
})

export type UserCreateValues = z.infer<typeof userCreateSchema>

export const userUpdateSchema = z.object({
  email: z.string().min(1, 'Обязательное поле').email('Некорректный email'),
  fullName: z.string().min(1, 'Обязательное поле'),
  enabled: z.boolean(),
  pictureUrl: z
    .string()
    .trim()
    .optional()
    .transform((v) => (v && v.length > 0 ? v : undefined)),
})

export type UserUpdateValues = z.infer<typeof userUpdateSchema>
