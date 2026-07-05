import { useEffect, useState, useCallback } from "react";
import { useAuth } from "../context/AuthContext";

interface TourStep {
  target: string; // CSS selector
  title: string;
  content: string;
  position: "top" | "bottom" | "left" | "right";
}

const TOUR_STEPS: TourStep[] = [
  {
    target: '[data-tour="dashboard"]',
    title: "Welcome to Emplo! 👋",
    content: "This is your Dashboard — a quick overview of your role, salary, certifications, and activity.",
    position: "right",
  },
  {
    target: '[data-tour="profile"]',
    title: "Your Profile",
    content: "View and manage your personal information, contact details, and profile photo here.",
    position: "right",
  },
  {
    target: '[data-tour="attendance"]',
    title: "Leave & Attendance",
    content: "Apply for leaves, check-in/out, and track your attendance history.",
    position: "right",
  },
  {
    target: '[data-tour="tasks"]',
    title: "Tasks",
    content: "See tasks assigned to you by your manager and mark them as complete.",
    position: "right",
  },
  {
    target: '[data-tour="onboarding"]',
    title: "Onboarding Checklist",
    content: "Complete your onboarding tasks here — upload documents, fill forms, and get started with the team.",
    position: "right",
  },
  {
    target: '[data-tour="documents"]',
    title: "Documents & Certifications",
    content: "Upload your educational documents and professional certifications for HR verification.",
    position: "right",
  },
  {
    target: '[data-tour="messages"]',
    title: "Messages",
    content: "Chat with your manager, teammates, and HR directly within the app.",
    position: "right",
  },
  {
    target: '[data-tour="notifications"]',
    title: "Notifications",
    content: "Stay updated on approvals, ticket responses, and important announcements here.",
    position: "right",
  },
];

const TOUR_STORAGE_KEY = "emplo_tour_completed";

export default function AppTour() {
  const { user } = useAuth();
  const [active, setActive] = useState(false);
  const [step, setStep] = useState(0);
  const [tooltipPos, setTooltipPos] = useState({ top: 0, left: 0 });
  const [targetRect, setTargetRect] = useState<DOMRect | null>(null);

  useEffect(() => {
    if (!user) return;
    // Only show tour once per user
    const key = `${TOUR_STORAGE_KEY}_${user.id}`;
    const completed = localStorage.getItem(key);
    if (!completed) {
      // Small delay so the page renders first
      const timer = setTimeout(() => setActive(true), 1500);
      return () => clearTimeout(timer);
    }
  }, [user]);

  const positionTooltip = useCallback((stepIndex: number) => {
    const currentStep = TOUR_STEPS[stepIndex];
    const el = document.querySelector(currentStep.target);
    if (!el) {
      // Skip steps with missing targets
      if (stepIndex < TOUR_STEPS.length - 1) {
        setStep(stepIndex + 1);
      } else {
        completeTour();
      }
      return;
    }

    const rect = el.getBoundingClientRect();
    setTargetRect(rect);

    let top = 0;
    let left = 0;

    switch (currentStep.position) {
      case "right":
        top = rect.top + rect.height / 2 - 80;
        left = rect.right + 16;
        break;
      case "left":
        top = rect.top + rect.height / 2 - 80;
        left = rect.left - 340;
        break;
      case "bottom":
        top = rect.bottom + 16;
        left = rect.left + rect.width / 2 - 160;
        break;
      case "top":
        top = rect.top - 180;
        left = rect.left + rect.width / 2 - 160;
        break;
    }

    // Keep tooltip within viewport
    top = Math.max(16, Math.min(top, window.innerHeight - 200));
    left = Math.max(16, Math.min(left, window.innerWidth - 340));

    setTooltipPos({ top, left });
  }, []);

  useEffect(() => {
    if (active) {
      positionTooltip(step);
    }
  }, [active, step, positionTooltip]);

  // Reposition on resize
  useEffect(() => {
    if (!active) return;
    const handler = () => positionTooltip(step);
    window.addEventListener("resize", handler);
    return () => window.removeEventListener("resize", handler);
  }, [active, step, positionTooltip]);

  const completeTour = () => {
    if (user) {
      localStorage.setItem(`${TOUR_STORAGE_KEY}_${user.id}`, "true");
    }
    setActive(false);
  };

  const nextStep = () => {
    if (step < TOUR_STEPS.length - 1) {
      setStep(step + 1);
    } else {
      completeTour();
    }
  };

  const prevStep = () => {
    if (step > 0) setStep(step - 1);
  };

  const skipTour = () => {
    completeTour();
  };

  if (!active) return null;

  const currentStep = TOUR_STEPS[step];

  return (
    <div className="tour-overlay">
      {/* Spotlight on target element */}
      {targetRect && (
        <div
          className="tour-spotlight"
          style={{
            top: targetRect.top - 4,
            left: targetRect.left - 4,
            width: targetRect.width + 8,
            height: targetRect.height + 8,
          }}
        />
      )}

      {/* Tooltip */}
      <div
        className="tour-tooltip"
        style={{ top: tooltipPos.top, left: tooltipPos.left }}
      >
        <div className="tour-tooltip-header">
          <span className="tour-step-badge">
            {step + 1} / {TOUR_STEPS.length}
          </span>
          <button className="tour-skip-btn" onClick={skipTour}>
            Skip tour
          </button>
        </div>
        <h3 className="tour-tooltip-title">{currentStep.title}</h3>
        <p className="tour-tooltip-content">{currentStep.content}</p>
        <div className="tour-tooltip-footer">
          <div className="tour-dots">
            {TOUR_STEPS.map((_, i) => (
              <span
                key={i}
                className={`tour-dot ${i === step ? "active" : ""} ${i < step ? "done" : ""}`}
              />
            ))}
          </div>
          <div className="tour-nav-btns">
            {step > 0 && (
              <button className="tour-btn-prev" onClick={prevStep}>
                Back
              </button>
            )}
            <button className="tour-btn-next" onClick={nextStep}>
              {step === TOUR_STEPS.length - 1 ? "Got it! 🎉" : "Next"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
