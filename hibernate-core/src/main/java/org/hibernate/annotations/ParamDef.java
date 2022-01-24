/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Details about a parameter defined in a FilterDef.
 * <p/>
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
	 * <p/>
	 * Generally deduced from the bind value.  Allows to
	 * specify a specific type to use.
	 * <p/>
	 * The supplied Class can be one of the following:<ul>
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
	 *         any Java type resolvable from {@link org.hibernate.type.descriptor.java.spi.JavaTypeRegistry}
	 *     </li>
	 * </ul>
	 */
	Class<?> type();
}
