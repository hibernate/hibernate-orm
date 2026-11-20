package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.MODULE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the Java class to use for the foreign key handling
 * related to an {@link Any} mapping.
 * <p>
 * The specified class is resolved to a {@link BasicJavaType}
 * via the {@link JavaTypeRegistry}.
 * <p>
 * When applied to a package or module, this annotation acts as a
 * default for every {@link Any} and {@link ManyToAny} mapping declared
 * by a class belonging to the package or module. The key type is taken
 * from the mapped attribute, then from the package of its declaring
 * class, and finally from the module.
 *
 * @see Any
 * @see AnyKeyJavaType
 *
 * @since 6.0
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE, PACKAGE, MODULE})
@Retention( RUNTIME )
public @interface AnyKeyJavaClass {
	/**
	 * The Java Class
	 */
	Class<?> value();
}
