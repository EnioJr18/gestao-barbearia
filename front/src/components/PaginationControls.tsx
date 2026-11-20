import { Button } from '@/components/ui/button'

interface PaginationControlsProps {
  page: number
  totalPages: number
  totalElements: number
  first?: boolean
  last?: boolean
  onPageChange: (page: number) => void
}

export function PaginationControls({
  page,
  totalPages,
  totalElements,
  first,
  last,
  onPageChange,
}: PaginationControlsProps) {
  if (totalPages === 0) return null

  return (
    <div className="flex flex-col items-center justify-between gap-3 border-t px-6 py-4 text-base sm:flex-row">
      <span className="text-muted-foreground">
        {totalElements} {totalElements === 1 ? 'registro' : 'registros'}
      </span>
      <div className="flex items-center gap-3">
        <Button
          type="button"
          variant="outline"
          disabled={first ?? page === 0}
          onClick={() => onPageChange(page - 1)}
        >
          Anterior
        </Button>
        <span>
          Página {page + 1} de {totalPages}
        </span>
        <Button
          type="button"
          variant="outline"
          disabled={last ?? page + 1 >= totalPages}
          onClick={() => onPageChange(page + 1)}
        >
          Próxima
        </Button>
      </div>
    </div>
  )
}
