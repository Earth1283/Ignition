# SIMD Density Combinators

Status: math library implemented and self-contained. Not wired into worldgen yet (see
[Remaining work](#remaining-work)).

## What it does

Vectorizes the density-function combinator arithmetic used during chunk terrain shaping
(add/mul/min/max/clamp/abs/square/cube over noise-sample arrays) using the JDK's Vector
API, targeting AVX/FMA-capable CPUs. The build already enables the Vector API module
(`--add-modules=jdk.incubator.vector`, inherited from Gale/Pufferfish), but nothing in
Leaf actually used it before this.

## Why this target

Vectorizing the gradient noise generation itself (`ImprovedNoise`/`PerlinNoise`) was
considered and rejected: it does data-dependent gather through a permutation table,
which doesn't vectorize cleanly with lane-wise operations, and
`0056-Optimize-noise-generation.patch` already hand-optimized that scalar path.

The combinator layer that combines already-computed noise samples
(`DensityFunctions.Add`/`Mul`/`Min`/`Max`/`Clamp`/etc., evaluated per-chunk via
`NoiseChunk`) is dense, branch-free, elementwise math over arrays — a much better fit.
It also runs for every point in the chunk-gen interpolation grid, for every chunk
generated.

Because the underlying noise samples stay scalar, this bounds the achievable win to the
combinator fraction of chunk-gen time, not the whole pipeline. If that turns out too
narrow once measured, block/sky light propagation is a fallback candidate — integer
arithmetic, so it has no floating-point exactness risk at all (see below).

## Bit-exactness constraint

Any change to noise/density math that isn't bit-identical to vanilla changes generated
terrain for a given seed — chunk-border mismatches, lost seed parity with vanilla/Paper.
`org.dreeam.leaf.math.simd.DensityCombinatorOps` is held to the same bar
`0056` set for its scalar rewrite:

- No `.fma(...)` — fused multiply-add is a single rounding step, not bit-identical to
  separate multiply and add.
- No `double` → `float` narrowing — stays on `DoubleVector`, not `FloatVector`, even
  though that halves the lane width.
- Lane-wise `add`/`mul`/`min`/`max`/`abs` on `DoubleVector` are bit-identical to scalar
  IEEE 754 as long as reduction order isn't changed, which these operations preserve.

Every method has a scalar tail loop for array lengths that aren't a multiple of the
vector width (`VectorSpecies.loopBound`), and `out` may safely alias `a` or `b`.

## Remaining work

1. **Confirm `DensityFunction`'s real API shape against the applied source tree.**
   The class/package names used here (`DensityFunction`, `DensityFunctions`,
   `NoiseChunk`) are reconstructed from general vanilla worldgen knowledge, not read
   from this checkout — there is no `net/minecraft/...` source present, only patches.
   In particular, confirm whether a batch/array evaluation entry point exists.
2. **If no batch entry point exists, one has to be introduced** — call sites that
   currently evaluate one point at a time need restructuring to first materialize a
   `double[]` of inputs, then call the batch op. That's real patch surface, not just a
   helper call.
3. **Correctness gate before any perf claim**: generate a fixed-seed test region with
   the SIMD path compiled out vs. in, diff resulting chunk data (block-state hash per
   chunk), require zero divergence.
4. Wire the relevant `DensityFunctions` implementation to call
   `DensityCombinatorOps` instead of its per-point scalar loop, as the only vanilla-side
   edit — new patch, not a modification of `0056`.

## Configuration

`performance.simd-density-combinators` in `leaf.yml`
(`org.dreeam.leaf.config.modules.opt.SimdDensityCombinators`):

| Key | Default | Meaning |
|---|---|---|
| `enabled` | `false` | Master toggle. Off until step 3 above (bit-exactness diffing) has actually been verified, then flip the default to `true`. |

No other knobs — the feature is a bit-exact drop-in with no behavior to tune once the
correctness gate passes.

## Verification (not yet run)

- **Blocking, before anything else**: fixed-seed region, diff block-state hashes per
  chunk with the SIMD path off vs. on. Zero divergence required.
- Only then: chunk-gen wall-clock time via spark, SIMD off vs. on, over a fixed
  pre-generation batch, isolating time attributed to `DensityFunctions`/`NoiseChunk`
  combinator methods specifically.
