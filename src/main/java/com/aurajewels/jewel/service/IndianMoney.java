/*
 * MIT License
 *
 * Copyright (c) 2026 AuraJewels (Raviraj Bhosale)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.aurajewels.jewel.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Indian-locale formatting for the invoice PDF: rupee amounts (lakh/crore grouping), gram weights,
 * and rupees-in-words. The template does no math — everything is pre-formatted here. Grouping is
 * done by hand because the JVM's en-IN locale produces Western (thousands) grouping.
 *
 * @author Raviraj Bhosale
 */
final class IndianMoney {

    private IndianMoney() {}

    private static final String[] ONES = {
        "",
        "One",
        "Two",
        "Three",
        "Four",
        "Five",
        "Six",
        "Seven",
        "Eight",
        "Nine",
        "Ten",
        "Eleven",
        "Twelve",
        "Thirteen",
        "Fourteen",
        "Fifteen",
        "Sixteen",
        "Seventeen",
        "Eighteen",
        "Nineteen"
    };
    private static final String[] TENS = {
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    /** e.g. ₹1,23,456.00 (Indian grouping: last 3 digits, then groups of 2). */
    static String rupees(BigDecimal value) {
        BigDecimal v = nz(value).setScale(2, RoundingMode.HALF_UP);
        boolean negative = v.signum() < 0;
        v = v.abs();
        BigInteger whole = v.toBigInteger();
        int paise = v.subtract(new BigDecimal(whole)).movePointRight(2).intValueExact();
        return "₹"
                + (negative ? "-" : "")
                + groupIndian(whole.toString())
                + "."
                + String.format("%02d", paise);
    }

    /** e.g. 5.230 — 3 decimal places, no unit. */
    static String weight(BigDecimal value) {
        return nz(value).setScale(3, RoundingMode.HALF_UP).toPlainString();
    }

    /** e.g. "One Lakh Twenty Three Thousand ... Rupees and Fifty Paise Only". */
    static String rupeesInWords(BigDecimal value) {
        BigDecimal v = nz(value).setScale(2, RoundingMode.HALF_UP);
        long rupees = v.longValue();
        int paise = v.subtract(BigDecimal.valueOf(rupees)).movePointRight(2).intValueExact();

        StringBuilder sb = new StringBuilder();
        if (rupees == 0) {
            sb.append("Zero ");
        } else {
            append(sb, rupees / 10000000, "Crore");
            rupees %= 10000000;
            append(sb, rupees / 100000, "Lakh");
            rupees %= 100000;
            append(sb, rupees / 1000, "Thousand");
            rupees %= 1000;
            append(sb, rupees / 100, "Hundred");
            rupees %= 100;
            if (rupees > 0) {
                sb.append(twoDigits((int) rupees)).append(' ');
            }
        }
        sb.append("Rupees");
        if (paise > 0) {
            sb.append(" and ").append(twoDigits(paise)).append(" Paise");
        }
        sb.append(" Only");
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    /** Indian digit grouping on a plain integer string (no sign). */
    private static String groupIndian(String digits) {
        if (digits.length() <= 3) {
            return digits;
        }
        String last3 = digits.substring(digits.length() - 3);
        String rest = digits.substring(0, digits.length() - 3);
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = rest.length() - 1; i >= 0; i--) {
            sb.append(rest.charAt(i));
            count++;
            if (count % 2 == 0 && i > 0) {
                sb.append(',');
            }
        }
        return sb.reverse() + "," + last3;
    }

    private static void append(StringBuilder sb, long part, String label) {
        if (part > 0) {
            sb.append(twoDigits((int) part)).append(' ').append(label).append(' ');
        }
    }

    /** Words for 0..99. */
    private static String twoDigits(int n) {
        if (n < 20) {
            return ONES[n];
        }
        String t = TENS[n / 10];
        return n % 10 == 0 ? t : t + " " + ONES[n % 10];
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
