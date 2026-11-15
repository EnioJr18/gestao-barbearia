import { Switch as SwitchPrimitive } from "@base-ui/react/switch"
import { motion } from "framer-motion"

import { cn } from "@/lib/utils"

function Switch({
  className,
  size = "default",
  checked,
  style,
  ...props
}: SwitchPrimitive.Root.Props & {
  size?: "sm" | "default"
}) {
  return (
    <SwitchPrimitive.Root
      data-slot="switch"
      data-size={size}
      className={cn(
        "peer group/switch relative inline-flex shrink-0 cursor-pointer items-center rounded-full border border-transparent transition-[background-color,box-shadow] duration-300 ease-in-out outline-none group-has-[:focus-visible]/field-label:border-transparent group-has-[:focus-visible]/field-label:ring-0 after:absolute after:-inset-x-3 after:-inset-y-2 focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/30 aria-invalid:border-destructive aria-invalid:ring-2 aria-invalid:ring-destructive/20 data-[size=default]:h-5 data-[size=default]:w-9 data-[size=default]:bg-input data-[size=sm]:h-4 data-[size=sm]:w-7 data-checked:bg-amber-500 data-unchecked:bg-input dark:data-unchecked:bg-input/80 data-disabled:cursor-not-allowed data-disabled:opacity-50",
        className
      )}
      checked={checked}
      style={{
        ...style,
        ...(checked === true
          ? { backgroundColor: "#f59e0b" }
          : checked === false
            ? { backgroundColor: "#292524" }
            : {}),
      }}
      {...props}
    >
      <SwitchPrimitive.Thumb
        data-slot="switch-thumb"
        className="pointer-events-none relative block size-4 bg-transparent"
      >
        <motion.span
          className="absolute left-0 top-0 size-4 rounded-full bg-background shadow-sm"
          animate={{ x: checked ? 16 : 0 }}
          transition={{ type: "spring", stiffness: 500, damping: 30 }}
        />
      </SwitchPrimitive.Thumb>
    </SwitchPrimitive.Root>
  )
}

export { Switch }
