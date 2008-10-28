/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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

//$Id: PropertyTypeExtractor.java 9316 2006-02-22 20:47:31Z epbernard $
package org.hibernate.annotations.common.reflection.java;

import java.lang.reflect.Type;
import java.util.Collection;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.java.generics.TypeEnvironment;
import org.hibernate.annotations.common.reflection.java.generics.TypeUtils;

/**
 * The Java X-layer equivalent to a Java <code>Type</code>.
 *
 * @author Emmanuel Bernard
 * @author Paolo Perrotta
 */
abstract class JavaXType {

	private final TypeEnvironment context;
	private final JavaReflectionManager factory;
	private final Type approximatedType;
	private final Type boundType;

	protected JavaXType(Type unboundType, TypeEnvironment context, JavaReflectionManager factory) {
		this.context = context;
		this.factory = factory;
		this.boundType = context.bind( unboundType );
		this.approximatedType = factory.toApproximatingEnvironment( context ).bind( unboundType );
	}

	abstract public boolean isArray();

	abstract public boolean isCollection();

	abstract public XClass getElementClass();

	abstract public XClass getClassOrElementClass();

	abstract public Class<? extends Collection> getCollectionClass();

	abstract public XClass getMapKey();

	abstract public XClass getType();

	public boolean isResolved() {
		return TypeUtils.isResolved( boundType );
	}

	protected Type approximate() {
		return approximatedType;
	}

	protected XClass toXClass(Type type) {
		return factory.toXClass( type, context );
	}
}
