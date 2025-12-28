package war.jnt.cache

import war.jnt.cache.struct.*

/**
 * @author etho
 */
class Cache {
    companion object {
        val klasses = mutableListOf<CachedClass>()
        val methods = mutableListOf<CachedMethod>()
        val fields  = mutableListOf<CachedField>()
        val indyArgs = mutableListOf<CachedIndyArgs>()

        // Fast index maps
        private val klassIndex = mutableMapOf<String, Int>()
        private val methodIndex = mutableMapOf<Quadruple<String, String, String, Boolean>, Int>()
        private val fieldIndex = mutableMapOf<Quadruple<String, String, String, Boolean>, Int>()
        private val indyArgsIndex = mutableMapOf<Array<Any>, Int>()

        fun request_klass(name: String): Int {
            return klassIndex[name] ?: run {
                val id = klasses.size
                klasses.add(CachedClass(name))
                klassIndex[name] = id
                id
            }
        }

        fun request_method(m: CachedMethod): Int {
            val key = Quadruple(m.owner, m.name, m.desc, m.isStatic)
            return methodIndex[key] ?: run {
                val id = methods.size
                methods.add(m)
                methodIndex[key] = id
                id
            }
        }

        fun request_field(f: CachedField): Int {
            val key = Quadruple(f.owner, f.name, f.desc, f.isStatic)
            return fieldIndex[key] ?: run {
                val id = fields.size
                fields.add(f)
                fieldIndex[key] = id
                id
            }
        }

        fun request_indy_args(args: CachedIndyArgs): Int {
            return indyArgsIndex[args.arguments] ?: run {
                val id = indyArgs.size
                indyArgs.add(args)
                indyArgsIndex[args.arguments] = id
                id
            }
        }

        fun cachedClasses(): Int = klasses.size
        fun cachedMethods(): Int = methods.size
        fun cachedFields(): Int = fields.size
        fun cachedIndyArgs(): Int = indyArgs.size

        fun clearAll() {
            klasses.clear()
            methods.clear()
            fields.clear()
            indyArgs.clear()
            klassIndex.clear()
            methodIndex.clear()
            fieldIndex.clear()
        }
    }
}