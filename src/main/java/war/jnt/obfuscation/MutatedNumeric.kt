package war.jnt.obfuscation

import org.objectweb.asm.Type
import java.security.SecureRandom

class MutatedNumeric(
    private val psh: String,
    private val cst: Any,
    private val type: Type,
) {
    companion object {
        // SSE dogshit, TODO: improve the handling of this
        var tc: Int = 0
        var sc: Int = 0
        var ttc: Int = 0
        var zc: Int = 0
    }

    private val rand = SecureRandom()

    fun get(): String {
        return when (type.sort) {
            Type.INT -> {
                val sb = StringBuilder()
                /* get rid of all of this trash... for now! */
//
//                val n = rand.nextInt()
//
//                val key = rand.nextInt()
//                val enc = (cst as Int) xor key
//
//                val fakeKey = rand.nextInt()
//
//                val keyName = mutDict.gen(6)

//                sb.append("\tint $keyName;\n")
//                sb.append("\tif ((atoi(\"$n\") * atoi(\"$n\") + atoi(\"$n\")) % 2 == 0) $keyName = atoi(\"$key\");\n")
//                sb.append("\telse $keyName = atoi(\"$fakeKey\");\n")
//
//                sb.append("\tasm volatile (\"\" ::: \"memory\");\n")

                // TODO very unstable and smokes the stack sometimes
//                val tcn = "mtc${tc}"
//                val scn = "msc${sc}"
//                val ttcn = "mttc${ttc}"
//                val zcn = "mzc${zc}"
//
//                val key = rand.nextInt()
//                val value = cst as Int
//                val enc = value xor key
//
//                sb.append("\t__m128i $tcn = _mm_and_si128(_mm_set1_epi32($enc), _mm_set1_epi32($key));\n")
//                sb.append("\t__m128i $scn = _mm_add_epi32(_mm_set1_epi32($enc), _mm_set1_epi32($key));\n")
//                sb.append("\t__m128i $ttcn = _mm_slli_epi32($tcn, 1);\n")
//                sb.append("\t__m128i $zcn = _mm_sub_epi32($scn, $ttcn);\n")
//                sb.append("\t$psh.i = _mm_cvtsi128_si32($zcn);\n")

                sb.append("\t$psh.i = ${(cst as Int)};\n")

                tc++
                sc++
                ttc++
                zc++

                return sb.toString()
            }
            else -> "// Mutation failure. Unknown type"
        }
    }
}