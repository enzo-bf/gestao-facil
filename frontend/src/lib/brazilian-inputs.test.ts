import { describe, expect, it } from "vitest";
import {
  formatBrazilianDocument,
  formatBrazilianPhone,
  formatMoneyInput,
  isValidBrazilianDocument,
  isValidBrazilianPhone,
  parseMoneyInput,
} from "./brazilian-inputs";

describe("Brazilian input validation and masks", () => {
  it("formats and validates CPF and CNPJ", () => {
    expect(formatBrazilianDocument("52998224725")).toBe("529.982.247-25");
    expect(isValidBrazilianDocument("529.982.247-25")).toBe(true);
    expect(formatBrazilianDocument("11222333000181")).toBe("11.222.333/0001-81");
    expect(isValidBrazilianDocument("11.222.333/0001-81")).toBe(true);
    expect(isValidBrazilianDocument("111.111.111-11")).toBe(false);
  });

  it("formats and validates Brazilian telephone numbers", () => {
    expect(formatBrazilianPhone("11987654321")).toBe("(11) 98765-4321");
    expect(isValidBrazilianPhone("(11) 98765-4321")).toBe(true);
    expect(isValidBrazilianPhone("(00) 00000-0000")).toBe(false);
    expect(isValidBrazilianPhone("(11)))98765-4321")).toBe(false);
  });

  it("formats Brazilian currency while preserving decimal precision", () => {
    expect(formatMoneyInput("123456")).toBe("1.234,56");
    expect(parseMoneyInput("1.234,56")).toBe("1234.56");
  });
});
