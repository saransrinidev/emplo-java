import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import { AuthProvider } from "./context/AuthContext";
import { ThemeProvider } from "./context/ThemeContext";
import { SidebarProvider } from "./context/SidebarContext";
import { ToastProvider } from "./components/Toast";
import "./styles/global.css";
import "./styles/components.css";
import "./styles/compensation.css";
import "./styles/onboarding.css";
import "./styles/tour.css";
import "./styles/performance.css";
import "./styles/chat.css";
import "./styles/reimbursements.css";
import "./styles/policies.css";
import "./styles/certifications.css";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <BrowserRouter>
      <ThemeProvider>
        <SidebarProvider>
          <AuthProvider>
            <ToastProvider>
              <App />
            </ToastProvider>
          </AuthProvider>
        </SidebarProvider>
      </ThemeProvider>
    </BrowserRouter>
  </StrictMode>,
);
