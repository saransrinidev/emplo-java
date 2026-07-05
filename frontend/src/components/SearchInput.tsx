import { Search, X } from "lucide-react";

interface SearchInputProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  className?: string;
}

/**
 * Reusable search input with icon and optional clear button.
 * Usage:
 * ```tsx
 * <SearchInput value={search} onChange={setSearch} placeholder="Search employees..." />
 * ```
 */
export default function SearchInput({
  value,
  onChange,
  placeholder = "Search...",
  className = "",
}: SearchInputProps) {
  return (
    <div className={`search-input-wrapper ${className}`}>
      <Search size={15} className="search-icon-inside" />
      <input
        className="input search-bar-input"
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
      />
      {value && (
        <button
          className="search-clear-btn"
          onClick={() => onChange("")}
          aria-label="Clear search"
          type="button"
        >
          <X size={14} />
        </button>
      )}
    </div>
  );
}
