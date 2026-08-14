package org.dreeam.leaf.math.simd;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorSpecies;

/**
 * Vectorized density-function combinator math for chunk generation. Every result is
 * bit-identical to the equivalent scalar computation: no {@code fma}, no float narrowing,
 * no reassociation. {@code out} may safely alias {@code a} or {@code b}.
 */
public final class DensityCombinatorOps {

    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;

    private DensityCombinatorOps() {
    }

    public static void add(double[] a, double[] b, double[] out, int length) {
        int i = 0;
        int bound = SPECIES.loopBound(length);
        for (; i < bound; i += SPECIES.length()) {
            DoubleVector.fromArray(SPECIES, a, i)
                .add(DoubleVector.fromArray(SPECIES, b, i))
                .intoArray(out, i);
        }
        for (; i < length; i++) {
            out[i] = a[i] + b[i];
        }
    }

    public static void mul(double[] a, double[] b, double[] out, int length) {
        int i = 0;
        int bound = SPECIES.loopBound(length);
        for (; i < bound; i += SPECIES.length()) {
            DoubleVector.fromArray(SPECIES, a, i)
                .mul(DoubleVector.fromArray(SPECIES, b, i))
                .intoArray(out, i);
        }
        for (; i < length; i++) {
            out[i] = a[i] * b[i];
        }
    }

    public static void min(double[] a, double[] b, double[] out, int length) {
        int i = 0;
        int bound = SPECIES.loopBound(length);
        for (; i < bound; i += SPECIES.length()) {
            DoubleVector.fromArray(SPECIES, a, i)
                .min(DoubleVector.fromArray(SPECIES, b, i))
                .intoArray(out, i);
        }
        for (; i < length; i++) {
            out[i] = Math.min(a[i], b[i]);
        }
    }

    public static void max(double[] a, double[] b, double[] out, int length) {
        int i = 0;
        int bound = SPECIES.loopBound(length);
        for (; i < bound; i += SPECIES.length()) {
            DoubleVector.fromArray(SPECIES, a, i)
                .max(DoubleVector.fromArray(SPECIES, b, i))
                .intoArray(out, i);
        }
        for (; i < length; i++) {
            out[i] = Math.max(a[i], b[i]);
        }
    }

    public static void clamp(double[] a, double min, double max, double[] out, int length) {
        int i = 0;
        int bound = SPECIES.loopBound(length);
        DoubleVector minVector = DoubleVector.broadcast(SPECIES, min);
        DoubleVector maxVector = DoubleVector.broadcast(SPECIES, max);
        for (; i < bound; i += SPECIES.length()) {
            DoubleVector.fromArray(SPECIES, a, i)
                .max(minVector)
                .min(maxVector)
                .intoArray(out, i);
        }
        for (; i < length; i++) {
            out[i] = Math.min(Math.max(a[i], min), max);
        }
    }

    public static void abs(double[] a, double[] out, int length) {
        int i = 0;
        int bound = SPECIES.loopBound(length);
        for (; i < bound; i += SPECIES.length()) {
            DoubleVector.fromArray(SPECIES, a, i)
                .abs()
                .intoArray(out, i);
        }
        for (; i < length; i++) {
            out[i] = Math.abs(a[i]);
        }
    }

    public static void square(double[] a, double[] out, int length) {
        mul(a, a, out, length);
    }

    public static void cube(double[] a, double[] out, int length) {
        int i = 0;
        int bound = SPECIES.loopBound(length);
        for (; i < bound; i += SPECIES.length()) {
            DoubleVector v = DoubleVector.fromArray(SPECIES, a, i);
            v.mul(v).mul(v).intoArray(out, i);
        }
        for (; i < length; i++) {
            out[i] = a[i] * a[i] * a[i];
        }
    }
}
