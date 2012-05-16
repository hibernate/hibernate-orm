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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation used to indicate that an entity represents shared (non tenant aware) data in a multi-tenant
 * application.
 *
 * Valid only at the root of an inheritance hierarchy.
 *
 * @author Steve Ebersole
 */
@java.lang.annotation.Target(TYPE)
@Retention(RUNTIME)
public @interface MultiTenancy {
	public boolean shared() default true;

	/**
	 * The discriminator values can be either be handled as literals or handled through JDBC parameter binding.
	 * {@code true} here (the default) indicates that the parameter binding approach should be used; {@code false}
	 * indicates the value should be handled as a literal.
	 * <p/>
	 * Care should be used specifying to use literals here.  PreparedStatements will not be able to be reused
	 * nearly as often by the database/driver which can potentially cause a significant performance impact to your
	 * application.
	 */
	public boolean useParameterBinding() default true;
}
