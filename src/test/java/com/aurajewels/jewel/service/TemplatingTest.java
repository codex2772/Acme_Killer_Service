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

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TemplatingTest {

    // ── MustacheLite ────────────────────────────────────────────────

    @Test
    void substitutesAndHtmlEscapesVariables() {
        String out = MustacheLite.render("Hi {{ name }}", Map.of("name", "A & B <c> \"d\""));
        assertThat(out).isEqualTo("Hi A &amp; B &lt;c&gt; &quot;d&quot;");
    }

    @Test
    void sectionRendersWhenTruthySkipsWhenFalsy() {
        String tmpl = "{{# x }}[{{ x }}]{{/ x }}";
        assertThat(MustacheLite.render(tmpl, Map.of("x", "v"))).isEqualTo("[v]");
        assertThat(MustacheLite.render(tmpl, Map.of())).isEmpty();
    }

    @Test
    void invertedSectionRendersWhenFalsyOrEmptyList() {
        assertThat(MustacheLite.render("{{^ x }}none{{/ x }}", Map.of())).isEqualTo("none");
        assertThat(MustacheLite.render("{{^ x }}none{{/ x }}", Map.of("x", "v"))).isEmpty();
        assertThat(MustacheLite.render("{{^ items }}empty{{/ items }}", Map.of("items", List.of())))
                .isEqualTo("empty");
    }

    @Test
    void iteratesListsAndFallsThroughToParentContext() {
        String tmpl = "{{# items }}{{ index }}:{{ name }}@{{ top }};{{/ items }}";
        Map<String, Object> data =
                Map.of(
                        "top",
                        "T",
                        "items",
                        List.of(
                                Map.of("index", 1, "name", "Ring"),
                                Map.of("index", 2, "name", "Chain")));
        // parent 'top' is visible inside each item frame
        assertThat(MustacheLite.render(tmpl, data)).isEqualTo("1:Ring@T;2:Chain@T;");
    }

    // ── IndianMoney ─────────────────────────────────────────────────

    @Test
    void formatsIndianGroupedRupees() {
        assertThat(IndianMoney.rupees(new BigDecimal("111240"))).isEqualTo("₹1,11,240.00");
        assertThat(IndianMoney.rupees(new BigDecimal("1234567.5"))).isEqualTo("₹12,34,567.50");
        assertThat(IndianMoney.rupees(null)).isEqualTo("₹0.00");
    }

    @Test
    void formatsWeightTo3Decimals() {
        assertThat(IndianMoney.weight(new BigDecimal("5.2301"))).isEqualTo("5.230");
        assertThat(IndianMoney.weight(new BigDecimal("10"))).isEqualTo("10.000");
    }

    @Test
    void rupeesInWordsIndianSystem() {
        assertThat(IndianMoney.rupeesInWords(new BigDecimal("111240")))
                .isEqualTo("One Lakh Eleven Thousand Two Hundred Forty Rupees Only");
        assertThat(IndianMoney.rupeesInWords(new BigDecimal("100.50")))
                .isEqualTo("One Hundred Rupees and Fifty Paise Only");
        assertThat(IndianMoney.rupeesInWords(BigDecimal.ZERO)).isEqualTo("Zero Rupees Only");
    }
}
