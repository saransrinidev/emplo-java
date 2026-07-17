import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, it, expect } from "vitest";
import Login from "../pages/Login";
import { AuthProvider } from "../context/AuthContext";
import { ThemeProvider } from "../context/ThemeContext";

function renderLogin() {
  return render(
    <MemoryRouter>
      <ThemeProvider>
        <AuthProvider>
          <Login />
        </AuthProvider>
      </ThemeProvider>
    </MemoryRouter>,
  );
}

describe("Login Page", () => {
  it("renders email and password inputs", () => {
    renderLogin();
    expect(screen.getByLabelText(/email/i) || screen.getByPlaceholderText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i) || screen.getByPlaceholderText(/password/i)).toBeInTheDocument();
  });

  it("does NOT prefill any default password", () => {
    renderLogin();
    const passwordInput = document.querySelector('input[type="password"]') as HTMLInputElement;
    expect(passwordInput.value).toBe("");
    expect(passwordInput.placeholder).not.toContain("Secret");
  });

  it("has a submit button", () => {
    renderLogin();
    const btn = screen.getByRole("button", { name: /sign in|log in|login/i });
    expect(btn).toBeInTheDocument();
  });

  it("email input has autocomplete attribute", () => {
    renderLogin();
    const emailInput = document.querySelector('input[type="email"]') as HTMLInputElement;
    if (emailInput) {
      expect(emailInput.getAttribute("autocomplete")).toBeTruthy();
    }
  });
});
