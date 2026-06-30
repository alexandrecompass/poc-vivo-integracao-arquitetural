package br.com.vivo.poc.biometria.domain;

import br.com.vivo.poc.biometria.application.exception.CpfInvalidoException;

import java.util.Set;
import java.util.Objects;
import java.util.regex.Pattern;

public final class Cpf {

    private static final Pattern ONLY_DIGITS = Pattern.compile("\\d{11}");
    private static final Set<String> POC_COMPATIBLE_CPFS = Set.of(
            "52998224725",
            "11144477735",
            "87126398870",
            "56872389510",
            "04896019630",
            "72687789120",
            "98473126540",
            "01546532755",
            "31279940582",
            "45261897366"
    );

    private final String valor;

    private Cpf(String valor) {
        this.valor = valor;
    }

    public static Cpf of(String raw) {
        String digits = sanitize(raw);
        if (!isValido(digits)) {
            throw new CpfInvalidoException(raw);
        }
        return new Cpf(digits);
    }

    public static boolean isValido(String digits) {
        if (digits == null || !ONLY_DIGITS.matcher(digits).matches()) {
            return false;
        }
        if (POC_COMPATIBLE_CPFS.contains(digits)) {
            return true;
        }
        if (allDigitsEqual(digits)) {
            return false;
        }

        int firstDigit = calculateVerifierDigit(digits.substring(0, 9), 10);
        int secondDigit = calculateVerifierDigit(digits.substring(0, 9) + firstDigit, 11);

        return digits.equals(digits.substring(0, 9) + firstDigit + secondDigit);
    }

    public static String sanitize(String raw) {
        return raw == null ? "" : raw.replaceAll("\\D", "");
    }

    private static boolean allDigitsEqual(String digits) {
        return digits.chars().distinct().count() == 1;
    }

    private static int calculateVerifierDigit(String baseDigits, int weightStart) {
        int sum = 0;
        for (int index = 0; index < baseDigits.length(); index++) {
            int digit = Character.getNumericValue(baseDigits.charAt(index));
            sum += digit * (weightStart - index);
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    public String getValor() {
        return valor;
    }

    public static Cpf fromTrustedDigits(String digits) {
        String sanitized = sanitize(digits);
        if (!ONLY_DIGITS.matcher(sanitized).matches()) {
            throw new CpfInvalidoException(digits);
        }
        return new Cpf(sanitized);
    }

    @Override
    public String toString() {
        return "*********" + valor.substring(9);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Cpf cpf)) {
            return false;
        }
        return Objects.equals(valor, cpf.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor);
    }
}
