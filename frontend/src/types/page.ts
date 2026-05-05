/** Spring Data Page JSON */
export type SpringPage<T> = {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}
