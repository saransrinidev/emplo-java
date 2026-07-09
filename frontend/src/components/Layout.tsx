import { useEffect, useState, useRef } from "react";
import { Outlet, useNavigate, Link } from "react-router-dom";
import {
  Bell,
  Menu,
  FileText,
  Award,
  CreditCard,
  ShieldAlert
} from "lucide-react";
import Sidebar from "./Sidebar";
import CommandPalette from "./CommandPalette";
import AppTour from "./AppTour";
import ChatWidget from "./ChatWidget";
import { useSidebar } from "../context/SidebarContext";
import { useAuth } from "../context/AuthContext";
import { notificationsApi, type NotificationItem } from "../api/notifications";

// Helper: extract employee ID from notification message if present: [employee:UUID]
function extractEmployeeId(message: string): string | null {
  const match = message.match(/\[employee:([a-f0-9-]+)\]/i);
  return match ? match[1] : null;
}

// Clean notification message for display
function cleanNotificationMessage(message: string): string {
  return message.replace(/\[employee:[a-f0-9-]+\]/gi, "").trim();
}

// Map notification to a route
function getNotificationRoute(notification: NotificationItem): string | null {
  const title = notification.title.toLowerCase();
  const message = notification.message.toLowerCase();
  const employeeId = extractEmployeeId(notification.message);

  if (employeeId) {
    if (title.includes("document")) return `/employees/${employeeId}?tab=documents`;
    if (title.includes("certification") || title.includes("certificate")) return `/employees/${employeeId}?tab=certifications`;
    if (title.includes("salary")) return `/employees/${employeeId}?tab=salary`;
    if (title.includes("performance") || title.includes("review")) return `/employees/${employeeId}?tab=performance`;
    return `/employees/${employeeId}`;
  }

  if (title.includes("document") || message.includes("document")) return "/documents";
  if (title.includes("certification") || title.includes("certificate") || message.includes("certif")) return "/certifications";
  if (title.includes("salary") || message.includes("salary")) return "/salary";
  if (title.includes("performance") || message.includes("review") || message.includes("performance")) return "/performance";
  if (title.includes("profile") || message.includes("profile")) return "/profile";
  if (title.includes("permission") || message.includes("permission")) return "/profile";
  if (title.includes("edit access")) return "/profile";

  return null;
}

// Map notification category to Lucide components & styles
function getNotificationCategory(notification: NotificationItem) {
  const title = notification.title.toLowerCase();
  const message = notification.message.toLowerCase();

  if (title.includes("document") || message.includes("document")) {
    return {
      icon: FileText,
      colorClass: "doc-notif",
      label: "Document"
    };
  }
  if (title.includes("certification") || title.includes("certificate") || message.includes("certif")) {
    return {
      icon: Award,
      colorClass: "cert-notif",
      label: "Certification"
    };
  }
  if (title.includes("salary") || message.includes("salary") || title.includes("payroll")) {
    return {
      icon: CreditCard,
      colorClass: "salary-notif",
      label: "Salary"
    };
  }
  if (title.includes("permission") || title.includes("edit access") || message.includes("permission") || message.includes("access")) {
    return {
      icon: ShieldAlert,
      colorClass: "access-notif",
      label: "Security"
    };
  }
  return {
    icon: Bell,
    colorClass: "general-notif",
    label: "Alert"
  };
}

// Relative time formatter helper
function formatTimeAgo(dateString: string): string {
  try {
    const now = new Date();
    const date = new Date(dateString);
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) return "Just now";
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays === 1) return "Yesterday";
    return date.toLocaleDateString();
  } catch {
    return "";
  }
}

export default function Layout() {
  const { mobileOpen, openMobile, closeMobile } = useSidebar();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [unreadCount, setUnreadCount] = useState(0);
  const [isOpen, setIsOpen] = useState(false);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const dropdownRef = useRef<HTMLDivElement | null>(null);

  const loadUnreadCount = async () => {
    if (!user) return;
    try {
      const r = await notificationsApi.unreadCount();
      setUnreadCount(r.count);
    } catch { }
  };

  const fetchRecentNotifications = async () => {
    try {
      const list = await notificationsApi.list();
      setNotifications(list.slice(0, 5));
    } catch { }
  };

  useEffect(() => {
    loadUnreadCount();
  }, [user]);

  useEffect(() => {
    if (isOpen) {
      fetchRecentNotifications();
    }
  }, [isOpen]);

  // Click outside to close listener
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [isOpen]);

  const handleMarkAllRead = async () => {
    try {
      await notificationsApi.markAllRead();
      setUnreadCount(0);
      setNotifications((prev) => prev.map((item) => ({ ...item, is_read: true })));
    } catch (e) {
      console.error("Failed to mark all as read", e);
    }
  };

  const handleNotificationClick = async (item: NotificationItem) => {
    setIsOpen(false);
    if (!item.is_read) {
      try {
        await notificationsApi.markOneRead(item.id);
        loadUnreadCount();
      } catch (e) {
        console.error("Failed to mark notification as read", e);
      }
    }
    const route = getNotificationRoute(item);
    if (route) {
      navigate(route);
    }
  };

  return (
    <div className="app-shell">
      {/* Mobile overlay */}
      <div
        className={`sidebar-overlay ${mobileOpen ? "active" : ""}`}
        onClick={closeMobile}
      />
      <Sidebar />
      <main className="app-main">
        <div className="top-bar">
          <button
            className="mobile-menu-btn"
            onClick={openMobile}
            aria-label="Open menu"
          >
            <Menu size={20} />
          </button>

          {/* Command palette trigger in navbar */}
          <CommandPalette />

          <div style={{ display: "flex", alignItems: "center", gap: 12, marginLeft: "auto" }}>
            
            {/* Notification Dropdown Container */}
            <div className="notification-dropdown-wrapper" ref={dropdownRef}>
              <button
                className={`top-bar-btn ${isOpen ? "active" : ""}`}
                onClick={() => setIsOpen(!isOpen)}
                aria-label="Notifications"
              >
                <Bell size={20} />
                {unreadCount > 0 && (
                  <span className="notification-badge-count">{unreadCount}</span>
                )}
              </button>

              {isOpen && (
                <div className="notification-dropdown">
                  <div className="notif-dropdown-header">
                    <h3>Notifications</h3>
                    {unreadCount > 0 && (
                      <button className="notif-dropdown-mark-all" onClick={handleMarkAllRead}>
                        Mark all read
                      </button>
                    )}
                  </div>

                  <div className="notif-dropdown-list">
                    {notifications.length === 0 ? (
                      <div style={{ textAlign: "center", padding: "2.5rem 1rem", color: "var(--text-secondary)" }}>
                        <Bell size={32} style={{ color: "var(--text-muted)", marginBottom: "8px", opacity: 0.6 }} />
                        <p style={{ margin: 0, fontSize: "13px" }}>No new notifications</p>
                      </div>
                    ) : (
                      notifications.map((item) => {
                        const category = getNotificationCategory(item);
                        const Icon = category.icon;
                        const cleanMsg = cleanNotificationMessage(item.message);
                        const timeAgo = formatTimeAgo(item.created_at);

                        return (
                          <div
                            key={item.id}
                            className={`notif-dropdown-item ${!item.is_read ? "unread" : ""}`}
                            onClick={() => handleNotificationClick(item)}
                          >
                            {!item.is_read && <span className="notif-dropdown-item-unread-dot" />}
                            <span className={`notif-dropdown-item-icon ${category.colorClass}`}>
                              <Icon size={16} />
                            </span>
                            <div className="notif-dropdown-item-content">
                              <div className="notif-dropdown-item-title">{item.title}</div>
                              <div className="notif-dropdown-item-message">{cleanMsg}</div>
                              <div className="notif-dropdown-item-time">{timeAgo}</div>
                            </div>
                          </div>
                        );
                      })
                    )}
                  </div>

                  <div className="notif-dropdown-footer">
                    <Link
                      to="/notifications"
                      className="notif-dropdown-footer-link"
                      onClick={() => setIsOpen(false)}
                    >
                      View All Notifications
                    </Link>
                  </div>
                </div>
              )}
            </div>

          </div>
        </div>
        <Outlet />
      </main>
      <AppTour />
      <ChatWidget />
    </div>
  );
}
