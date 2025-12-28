package war.jnt.utility.mapping.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Section(val type: String)
