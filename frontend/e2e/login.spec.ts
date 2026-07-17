import { test, expect } from "@playwright/test";

test.describe("Login Page", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/login");
  });

  test("displays login form with no prefilled passwords", async ({ page }) => {
    const passwordInput = page.locator('input[type="password"]');
    await expect(passwordInput).toHaveValue("");
    await expect(passwordInput).not.toHaveAttribute("value", /Secret/);
  });

  test("is keyboard navigable", async ({ page }) => {
    // Tab through form elements
    await page.keyboard.press("Tab"); // should focus email
    const focused1 = await page.evaluate(() => document.activeElement?.tagName);
    expect(focused1).toBe("INPUT");

    await page.keyboard.press("Tab"); // should focus password
    const focused2 = await page.evaluate(() => document.activeElement?.getAttribute("type"));
    expect(focused2).toBe("password");

    await page.keyboard.press("Tab"); // should focus submit button
    const focused3 = await page.evaluate(() => document.activeElement?.tagName);
    expect(focused3).toBe("BUTTON");
  });

  test("shows error on invalid credentials", async ({ page }) => {
    await page.fill('input[type="email"]', "wrong@test.com");
    await page.fill('input[type="password"]', "wrongpass");
    await page.click('button[type="submit"]');
    // Should show an error message
    await expect(page.locator("text=/invalid|error|failed/i")).toBeVisible({ timeout: 5000 });
  });

  test("has proper form labels for screen readers", async ({ page }) => {
    // Check that inputs have associated labels or aria-label
    const emailInput = page.locator('input[type="email"]');
    const hasLabel = await emailInput.evaluate((el) => {
      const id = el.id;
      if (id && document.querySelector(`label[for="${id}"]`)) return true;
      if (el.getAttribute("aria-label")) return true;
      if (el.getAttribute("placeholder")) return true;
      return false;
    });
    expect(hasLabel).toBe(true);
  });
});

test.describe("Responsive Design", () => {
  test("login page renders correctly on mobile", async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 }); // iPhone SE
    await page.goto("/login");
    const form = page.locator("form");
    await expect(form).toBeVisible();
    // Form should not overflow the viewport
    const box = await form.boundingBox();
    if (box) {
      expect(box.width).toBeLessThanOrEqual(375);
    }
  });

  test("login page renders correctly on tablet", async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 }); // iPad
    await page.goto("/login");
    const form = page.locator("form");
    await expect(form).toBeVisible();
  });
});
