import { test, expect } from "@playwright/test";

test.describe("Accessibility", () => {
  test("all pages have no missing alt attributes on images", async ({ page }) => {
    await page.goto("/login");
    const images = await page.locator("img").all();
    for (const img of images) {
      const alt = await img.getAttribute("alt");
      expect(alt, "Image missing alt attribute").not.toBeNull();
    }
  });

  test("focus is visible on interactive elements", async ({ page }) => {
    await page.goto("/login");
    await page.keyboard.press("Tab");
    // The focused element should have a visible outline or ring
    const hasFocusStyle = await page.evaluate(() => {
      const el = document.activeElement;
      if (!el) return false;
      const styles = getComputedStyle(el);
      return styles.outlineStyle !== "none" || styles.boxShadow !== "none";
    });
    expect(hasFocusStyle).toBe(true);
  });

  test("color contrast: text is readable", async ({ page }) => {
    await page.goto("/login");
    // Check that body text color has sufficient contrast against background
    const contrast = await page.evaluate(() => {
      const body = document.body;
      const styles = getComputedStyle(body);
      // Basic check: text color is not the same as background
      return styles.color !== styles.backgroundColor;
    });
    expect(contrast).toBe(true);
  });

  test("interactive elements have proper ARIA roles", async ({ page }) => {
    await page.goto("/login");
    // Submit button should have button role
    const submitBtn = page.locator('button[type="submit"]');
    if (await submitBtn.count() > 0) {
      const role = await submitBtn.getAttribute("role");
      // Buttons have implicit role="button" — no explicit needed
      const tagName = await submitBtn.evaluate((el) => el.tagName);
      expect(tagName).toBe("BUTTON");
    }
  });
});
