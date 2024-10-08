/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javafx.css;

import static javafx.geometry.NodeOrientation.INHERIT;

import com.sun.javafx.css.CompoundSelector;
import com.sun.javafx.css.ImmutablePseudoClassSetsCache;
import com.sun.javafx.css.PseudoClassState;
import com.sun.javafx.css.SimpleSelector;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Used by {@link Rule} to determine whether or not the selector applies to a
 * given object.
 *
 * Returned by {@link Selector} matches in the event of a match.
 *
 * @since 9
 */
public final class Match implements Comparable<Match> {

    static Match of(Selector selector) {
        return switch(selector) {
            case SimpleSelector s -> ofSimpleSelector(s);
            case CompoundSelector cs -> ofCompoundSelector(cs);
        };
    }

    private static Match ofCompoundSelector(CompoundSelector selector) {
        PseudoClassState pseudoClasses = new PseudoClassState();
        int idCount = 0;
        int styleClassCount = 0;
        List<SimpleSelector> containedSelectors = selector.getSelectors();

        for (int n = 0, max = containedSelectors.size(); n < max; n++) {
            Match match = ofSimpleSelector(containedSelectors.get(n));

            pseudoClasses.addAll(match.getPseudoClasses());
            idCount += match.idCount;
            styleClassCount += match.styleClassCount;
        }

        return new Match(selector, pseudoClasses, idCount, styleClassCount);
    }

    private static Match ofSimpleSelector(SimpleSelector selector) {
        int idCount = selector.getId().isEmpty() ? 0 : 1;
        int styleClassCount = selector.getStyleClassSet().size();

        return new Match(selector, selector.getPseudoClassStates(), idCount, styleClassCount);
    }

    private final Selector selector;
    private final Set<PseudoClass> pseudoClasses;

    private final int styleClassCount;
    private final int idCount;

    // CSS3 spec gives weight to id count, then style class count,
    // then pseudoclass count, and finally matching types (i.e., java name count)
    private final int specificity;

    Match(final Selector selector, Set<PseudoClass> pseudoClasses, int idCount, int styleClassCount) {
        Objects.requireNonNull(selector);
        Objects.requireNonNull(pseudoClasses);

        this.selector = selector;
        this.idCount = idCount;
        this.styleClassCount = styleClassCount;
        this.pseudoClasses = ImmutablePseudoClassSetsCache.of(pseudoClasses);
        int nPseudoClasses = pseudoClasses.size();
        if (selector instanceof SimpleSelector simple) {
            if (simple.getNodeOrientation() != INHERIT) {
                nPseudoClasses += 1;
            }
        }
        specificity = (idCount << 8) | (styleClassCount << 4) | nPseudoClasses;
    }

    /**
     * Gets the {@code Selector}.
     *
     * @return the {@code Selector}, never {@code null}
     */
    public Selector getSelector() {
        return selector;
    }

    /**
     * Gets the pseudo class states as an immutable set.
     *
     * @return the pseudo class state, never {@code null}
     */
    public Set<PseudoClass> getPseudoClasses() {
        return pseudoClasses;
    }

    /**
     * Gets the specificity.
     * @return the specificity
     */
    public int getSpecificity() {
        return specificity;
    }

    /**
     * Compares this object with the given {@code Match} object.
     * <p>
     * Comparison is based on the specificity of the objects.
     * Specificity is calculated based on the id count, the style class count and
     * the pseudoclass count.
     * @param o the {@code Match} object to be compared
     * @return the difference between the specificity of this object and
     * the specificity of the given {@code Match} object
     */
    @Override public int compareTo(Match o) {
        return specificity - o.specificity;
    }

}
