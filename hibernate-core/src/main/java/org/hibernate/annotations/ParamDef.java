/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.function.Supplier;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Details about a parameter declared in a {@link FilterDef}.
 * <p>
 * Mainly used to support cases where the proper {@link #type type}
 * cannot be deduced by Hibernate.
 *
 * @see FilterDef#parameters()
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
@Target({})
@Retention(RUNTIME)
public @interface ParamDef {
	/**
	 * The name of the parameter.
	 */
	String name();

	/**
	 * The type to use when binding the parameter value.
	 * <p>
	 * Generally deduced from the bind value.  Allows to
	 * specify a specific type to use.
	 * <p>
	 * The supplied {@link Class} may be one of the following:<ul>
	 *     <li>
	 *         a {@link org.hibernate.usertype.UserType}
	 *     </li>
	 *     <li>
	 *         an {@link jakarta.persistence.AttributeConverter}
	 *     </li>
	 *     <li>
	 *         a {@link org.hibernate.type.descriptor.java.JavaType}
	 *     </li>
	 *     <li>
	 *         any Java type resolvable from
	 *         {@link org.hibernate.type.descriptor.java.spi.JavaTypeRegistry}
	 *     </li>
	 * </ul>
	 */
	Class<?> type();

	/**
	 * A class implementing {@link Supplier} which provides arguments
	 * to this parameter. This is especially useful in the case of
	 * {@linkplain FilterDef#autoEnabled auto-enabled} filters.
	 * <p>
	 * When a resolver is specified for a filter parameter, it's not
	 * necessary to provide an argument for the parameter by calling
	 * {@link org.hibernate.Filter#setParameter(String, Object)}.
	 */
	Class<? extends Supplier> resolver() default Supplier.class;
}
