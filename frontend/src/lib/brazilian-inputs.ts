export function digitsOnly(value: string): string {
  return value.replace(/\D/g, "");
}

export function formatBrazilianPhone(value: string): string {
  const digits = digitsOnly(value).slice(0, 11);
  if (!digits) return "";
  if (!/^[\d()+\-\s]*$/.test(value)) return formatBrazilianPhone(digits);
  if (digits.length <= 2) return `(${digits}`;
  if (digits.length <= 6) return `(${digits.slice(0, 2)}) ${digits.slice(2)}`;
  const split = digits.length === 11 ? 7 : 6;
  return `(${digits.slice(0, 2)}) ${digits.slice(2, split)}-${digits.slice(split)}`;
}

export function formatBrazilianDocument(value: string): string {
  const digits = digitsOnly(value).slice(0, 14);
  if (digits.length <= 11) {
    let formatted = digits.slice(0, 3);
    if (digits.length > 3) formatted += `.${digits.slice(3, 6)}`;
    if (digits.length > 6) formatted += `.${digits.slice(6, 9)}`;
    if (digits.length > 9) formatted += `-${digits.slice(9, 11)}`;
    return formatted;
  }
  let formatted = digits.slice(0, 2);
  if (digits.length > 2) formatted += `.${digits.slice(2, 5)}`;
  if (digits.length > 5) formatted += `.${digits.slice(5, 8)}`;
  if (digits.length > 8) formatted += `/${digits.slice(8, 12)}`;
  if (digits.length > 12) formatted += `-${digits.slice(12, 14)}`;
  return formatted;
}

export function isValidBrazilianPhone(value: string): boolean {
  const formatted = value.trim();
  if (!/^(?:\d{10,11}|\(\d{2}\) ?\d{4,5}-\d{4})$/.test(formatted)) return false;
  const digits = digitsOnly(value);
  if (digits.length !== 10 && digits.length !== 11) return false;
  const areaCode = Number(digits.slice(0, 2));
  if (areaCode < 11 || areaCode > 99) return false;
  return digits.length === 11 ? digits[2] === "9" : /^[2-5]/.test(digits.slice(2));
}

export function isValidBrazilianDocument(value: string): boolean {
  const formatted = value.trim();
  if (!/^(?:\d{11}|\d{14}|\d{3}\.\d{3}\.\d{3}-\d{2}|\d{2}\.\d{3}\.\d{3}\/\d{4}-\d{2})$/.test(formatted)) return false;
  const digits = digitsOnly(value);
  if (digits.length !== 11 && digits.length !== 14) return false;
  if (/^(\d)\1+$/.test(digits)) return false;
  if (digits.length === 11) {
    const first = cpfDigit(digits.slice(0, 9));
    const second = cpfDigit(digits.slice(0, 9) + first);
    return digits.endsWith(`${first}${second}`);
  }
  const first = cnpjDigit(digits.slice(0, 12), [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]);
  const second = cnpjDigit(digits.slice(0, 12) + first, [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]);
  return digits.endsWith(`${first}${second}`);
}

export function formatMoneyInput(value: string): string {
  const digits = digitsOnly(value);
  if (!digits) return "";
  const cents = Number(digits);
  if (!Number.isSafeInteger(cents)) return "";
  return (cents / 100).toLocaleString("pt-BR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

export function parseMoneyInput(value: string): string {
  return value.replace(/\./g, "").replace(",", ".").trim();
}

function cpfDigit(base: string): number {
  const sum = [...base].reduce((total, digit, index) => total + Number(digit) * (base.length + 1 - index), 0);
  const remainder = (sum * 10) % 11;
  return remainder === 10 ? 0 : remainder;
}

function cnpjDigit(base: string, weights: number[]): number {
  const sum = [...base].reduce((total, digit, index) => total + Number(digit) * weights[index], 0);
  const remainder = sum % 11;
  return remainder < 2 ? 0 : 11 - remainder;
}
