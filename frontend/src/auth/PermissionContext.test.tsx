import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const authState = vi.hoisted(() => ({
  user: { permissions: ["CLIENT_VIEW", "OS_VIEW"] as string[] },
}));

vi.mock("./AuthContext", () => ({
  useAuth: () => ({ user: authState.user }),
}));

import { PermissionProvider, usePermission } from "./PermissionContext";

function PermissionProbe() {
  const { hasPermission } = usePermission();
  return (
    <div>
      <span>{String(hasPermission("CLIENT_VIEW"))}</span>
      <span>{String(hasPermission("CLIENT_DELETE"))}</span>
    </div>
  );
}

describe("PermissionContext", () => {
  beforeEach(() => {
    authState.user = { permissions: ["CLIENT_VIEW", "OS_VIEW"] };
  });

  it("reports only permissions assigned to the authenticated user", () => {
    render(
      <PermissionProvider>
        <PermissionProbe />
      </PermissionProvider>,
    );

    expect(screen.getByText("true")).toBeInTheDocument();
    expect(screen.getByText("false")).toBeInTheDocument();
  });
});
