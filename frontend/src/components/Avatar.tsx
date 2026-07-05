interface AvatarProps {
  name: string;
  src?: string | null;
  size?: number;
  className?: string;
}

const GRADIENTS = [
  "linear-gradient(135deg, #031273 0%, #041890 100%)",
  "linear-gradient(135deg, #3b82f6 0%, #06b6d4 100%)",
  "linear-gradient(135deg, #10b981 0%, #059669 100%)",
  "linear-gradient(135deg, #f59e0b 0%, #d97706 100%)",
  "linear-gradient(135deg, #ec4899 0%, #f43f5e 100%)",
  "linear-gradient(135deg, #1e40af 0%, #1d4ed8 100%)",
];

function hashString(str: string): number {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash);
  }
  return hash;
}

/**
 * Reusable avatar component — shows profile photo or initials with gradient background.
 * Usage:
 * ```tsx
 * <Avatar name="John Doe" src={user.profile_photo} size={36} />
 * <Avatar name="Jane Smith" />  // shows "JS" with random gradient
 * ```
 */
export default function Avatar({ name, src, size = 36, className = "" }: AvatarProps) {
  const initials = name
    .split(" ")
    .map((n) => n[0])
    .join("")
    .toUpperCase()
    .slice(0, 2) || "??";

  const gradient = GRADIENTS[Math.abs(hashString(name)) % GRADIENTS.length];
  const fontSize = Math.max(size * 0.32, 10);

  if (src) {
    return (
      <img
        className={`ui-avatar ${className}`}
        src={src}
        alt={name}
        style={{ width: size, height: size, borderRadius: "50%", objectFit: "cover" }}
      />
    );
  }

  return (
    <div
      className={`ui-avatar ${className}`}
      style={{
        width: size,
        height: size,
        borderRadius: "50%",
        background: gradient,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        fontSize,
        fontWeight: 700,
        color: "#fff",
        flexShrink: 0,
      }}
    >
      {initials}
    </div>
  );
}
