import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import NoDataPanel from "./NoDataPanel";

describe("NoDataPanel", () => {
  it("renders an explicit unavailable state without a numeric placeholder", () => {
    const html = renderToStaticMarkup(<NoDataPanel detail="Awaiting observed responses" />);
    expect(html).toContain("No data available");
    expect(html).toContain("Awaiting observed responses");
    expect(html).not.toContain("0 ms");
  });
});
