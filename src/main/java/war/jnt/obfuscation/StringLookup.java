package war.jnt.obfuscation;

import war.jnt.core.Processor;
import war.jnt.core.header.Header;
import war.jnt.core.source.Source;
import war.metaphor.engine.Context;
import war.metaphor.engine.Engine;

import java.util.HashMap;
import java.util.Map;

public class StringLookup {

    public static Map<Engine, Integer> engines = new HashMap<>();

    public static int add(Engine engine) {
        return engines.computeIfAbsent(engine, _ -> engines.size());
    }

    public static void add(Engine engine, Integer id) {
        engines.put(engine, id);
    }

    public static void make(Processor processor) {

//        String header = "#pragma once\n" +
//                "#include \"jni.h\"\n" +
//                "#include <stdio.h>\n" +
//                "#include <stdint.h>\n" +
//                "#include <stdatomic.h>\n\n" +
//                "#include <emmintrin.h>\n\n" +
//                "jchar decode_character(jchar value, jint id);\n" +
//                "jchar* jnt_decode_string(jchar* str, jint length, jint id, jint* should_decode);\n\n";

        String header = """
                #pragma once
                #include "jni.h"
                #include <stdint.h>
                #include <stdatomic.h>
                
                // windows x86_64 check for SSE support
                #if defined(WIN32) || defined(_WIN32) || defined(__WIN32__) || defined(__NT__)
                #include <immintrin.h>
                #define USE_SSE
                #endif
                
                #ifndef ALTERNATIVE_SSE_INTRINSICS_H
                #define ALTERNATIVE_SSE_INTRINSICS_H
                
                #include <stdint.h>
                
                #ifndef USE_SSE
                
                typedef struct { int32_t val; } __m128i;
                typedef struct { float val; } __m128;
                
                static inline __m128i _mm_srli_epi32(__m128i a, int imm) {
                    __m128i r = { ((uint32_t)(a.val)) >> (imm & 31) };
                    return r;
                }
                
                static inline __m128i _mm_slli_epi32(__m128i a, int imm) {
                    __m128i r = { (uint32_t)(a.val) << (imm & 31) };
                    return r;
                }
                
                // _mm_set1_epi32: set all elements to the int32 value (scalar fallback just stores one)
                static inline __m128i _mm_set1_epi32(int32_t x) {
                    __m128i r = { x };
                    return r;
                }
                
                // _mm_cvtsi128_si32: extract int from __m128i scalar fallback
                static inline int32_t _mm_cvtsi128_si32(__m128i x) {
                    return x.val;
                }
                
                // _mm_add_epi32: add two __m128i scalars
                static inline __m128i _mm_add_epi32(__m128i a, __m128i b) {
                    __m128i r = { a.val + b.val };
                    return r;
                }
                
                // _mm_sub_epi32: subtract two __m128i scalars
                static inline __m128i _mm_sub_epi32(__m128i a, __m128i b) {
                    __m128i r = { a.val - b.val };
                    return r;
                }
                
                // _mm_shuffle_epi32: no-op for scalar fallback (returns input)
                static inline __m128i _mm_shuffle_epi32(__m128i x, int imm8) {
                    (void)imm8; // unused
                    return x;
                }
                
                // _mm_xor_si128: xor two __m128i scalars
                static inline __m128i _mm_xor_si128(__m128i a, __m128i b) {
                    __m128i r = { a.val ^ b.val };
                    return r;
                }
                
                // _mm_setzero_si128: zero scalar fallback
                static inline __m128i _mm_setzero_si128(void) {
                    __m128i r = { 0 };
                    return r;
                }
                
                // _mm_mullo_epi32: multiply low 32 bits (scalar multiply)
                static inline __m128i _mm_mullo_epi32(__m128i a, __m128i b) {
                    __m128i r = { a.val * b.val };
                    return r;
                }
                
                // _mm_or_si128: bitwise or on scalars
                static inline __m128i _mm_or_si128(__m128i a, __m128i b) {
                    __m128i r = { a.val | b.val };
                    return r;
                }
                
                // _mm_cvtepi32_ps: convert int to float (__m128)
                static inline __m128 _mm_cvtepi32_ps(__m128i a) {
                    __m128 r = { (float)(a.val) };
                    return r;
                }
                
                // _mm_div_ps: float division (__m128)
                static inline __m128 _mm_div_ps(__m128 a, __m128 b) {
                    __m128 r = { b.val != 0.0f ? a.val / b.val : 0.0f };
                    return r;
                }
                
                // _mm_cvttps_epi32: convert float to int (truncate) (__m128i)
                static inline __m128i _mm_cvttps_epi32(__m128 a) {
                    __m128i r = { (int32_t)(a.val) };
                    return r;
                }
                
                // _mm_cvtsi32_si128: convert int to __m128i
                static inline __m128i _mm_cvtsi32_si128(int32_t a) {
                    __m128i r = { a };
                    return r;
                }
                
                // _mm_sll_epi32: shift left logical (__m128i)
                static inline __m128i _mm_sll_epi32(__m128i a, __m128i count) {
                    __m128i r = { a.val << (count.val & 31) };
                    return r;
                }
                
                // _mm_and_si128: bitwise and (__m128i)
                static inline __m128i _mm_and_si128(__m128i a, __m128i b) {
                    __m128i r = { a.val & b.val };
                    return r;
                }
                
                // _mm_sra_epi32: arithmetic shift right (__m128i)
                static inline __m128i _mm_sra_epi32(__m128i a, __m128i count) {
                    __m128i r = { a.val >> (count.val & 31) };
                    return r;
                }
                
                // _mm_andnot_si128: bitwise and NOT a (~a & b)
                static inline __m128i _mm_andnot_si128(__m128i a, __m128i b) {
                    __m128i r = { (~a.val) & b.val };
                    return r;
                }
                
                // _mm_srl_epi32: logical shift right (__m128i)
                static inline __m128i _mm_srl_epi32(__m128i a, __m128i count) {
                    __m128i r = { ((uint32_t)(a.val)) >> (count.val & 31) };
                    return r;
                }
                
                #else
                
                #include <emmintrin.h>
                #include <immintrin.h>
                
                #endif // USE_SSE
                
                #endif // ALTERNATIVE_SSE_INTRINSICS_H
                
                jchar decode_character(jchar value, jint id);
                jchar* jnt_decode_string(jchar* str, jint length, jint id);
                """;

        processor.getHeaders().add(
                new Header("lib/string_obfuscation.h", header)
        );

        StringBuilder source = new StringBuilder();

        source.append("#include \"string_obfuscation.h\"\n\n");

        source.append("\nfinline jchar decode_character(jchar value, jint id) {\n");
        source.append("\tjlong tmp = value;\n");
        source.append("\tswitch (id) {\n");
        engines.forEach((engine, id) -> {
            source.append("\tcase ").append(id).append(":\n");
            source.append("\t").append(MutationUtil.VOLATILE_ASM);
            Context ctx = engine.getContext();
            ctx.varName = "tmp";
            source.append(engine.getSourceInstructions());
            source.append("\tvalue = tmp;\n");
            source.append("\t\treturn value;\n");
        });
        source.append("\tdefault:\n");
        source.append("\t\treturn value;\n");
        source.append("\t}\n");
        source.append("}\n\n");

        source.append("finline jchar* jnt_decode_string(jchar* str, jint length, jint id) {\n");
        source.append("\tfor (jint i = 0; i < length; ++i) {\n");
        source.append("\t\tstr[i] = decode_character(str[i], id);\n");
        source.append("\t}\n");
        source.append("\treturn str;\n");
        source.append("}\n\n");

        processor.getSources().add(
                new Source("lib/string_obfuscation.c", source.toString())
        );

    }

    public static String toUTF16(String str) {
        StringBuilder cn = new StringBuilder();
        cn.append("(jchar[]) {");
        for (char c : str.toCharArray()) {
            cn.append(" 0x")
                    .append(String.format("%04X", (int) c))
                    .append(", ");
        }
        if (!str.isEmpty()) cn.delete(cn.length() - 2, cn.length());
        cn.append(" }");
        return cn.toString();
    }
}
