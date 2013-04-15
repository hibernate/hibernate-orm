/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Names a {@link org.hibernate.property.PropertyAccessor} strategy to use.
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
 * Should only be used to name custom {@link org.hibernate.property.PropertyAccessor}.  For {@code property/field}
 * access, the JPA {@link javax.persistence.Access} annotation should be preferred using the appropriate
 * {@link javax.persistence.AccessType}.  However, if this annotation is used with either {@code value="property"}
 * or {@code value="field"}, it will act just as the corresponding usage of {@link javax.persistence.Access}.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
@java.lang.annotation.Target({ TYPE, METHOD, FIELD })
@Retention(RUNTIME)
public @interface AttributeAccessor {
	/**
	 * Names the {@link org.hibernate.property.PropertyAccessor} strategy.
	 */
	String value();
}
