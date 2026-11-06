package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.type.descriptor.java.BasicJavaType;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.MODULE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Form of {@link JavaType} used to describe the foreign-key part of an ANY mapping.
 * <p>
 * When applied to a package or module, this annotation acts as a default for every
 * {@link Any} and {@link ManyToAny} mapping declared by a class belonging to the
 * package or module. The key type is taken from the mapped attribute, then from the
 * package of its declaring class, and finally from the module.
 *
 * @see Any
 * @see AnyKeyJavaClass
 *
 * @since 6.0
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE, PACKAGE, MODULE})
@Retention( RUNTIME )
public @interface AnyKeyJavaType {
	/**
	 * The type descriptor to use
	 *
	 * @see JavaType#value
	 */
	Class<? extends BasicJavaType<?>> value();
}
