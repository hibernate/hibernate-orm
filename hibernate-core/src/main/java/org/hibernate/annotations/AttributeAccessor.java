/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Names a persistent property access strategy ({@link org.hibernate.property.access.spi.PropertyAccessStrategy}) to use.
 *
 * Can be specified at either:<ul>
 *     <li>
 *         <strong>TYPE</strong> level, which will act as naming the default accessor strategy for
 *         all attributes on the class which do not explicitly name an accessor strategy
 *     </li>
 *     <li>
 *         <strong>METHOD/FIELD</strong> level, which will be in effect for just that attribute.
 *     </li>
 * </ul>
 *
 * Should only be used to name custom {@link org.hibernate.property.access.spi.PropertyAccessStrategy}.  For
 * {@code property/field} access, the JPA {@link javax.persistence.Access} annotation should be preferred
 * using the appropriate {@link javax.persistence.AccessType}.  However, if this annotation is used with
 * either {@code value="property"} or {@code value="field"}, it will act just as the corresponding usage
 * of {@link javax.persistence.Access}.
 *
 * @see org.hibernate.property.access.spi.PropertyAccessStrategy
 * @see org.hibernate.property.access.spi.PropertyAccessStrategyResolver
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
@java.lang.annotation.Target({ TYPE, METHOD, FIELD })
@Retention(RUNTIME)
public @interface AttributeAccessor {
	/**
	 * Names the {@link org.hibernate.property.access.spi.PropertyAccessStrategy} strategy.
	 */
	String value();
}
