/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.type;

import java.lang.reflect.Method;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.tuple.component.ComponentMetamodel;

/**
 * @author Gavin King
 */
public class EmbeddedComponentType extends ComponentType {
	public EmbeddedComponentType(TypeFactory.TypeScope typeScope, ComponentMetamodel metamodel) {
		super( typeScope, metamodel );
	}

	public boolean isEmbedded() {
		return true;
	}

	public boolean isMethodOf(Method method) {
		return componentTuplizer.isMethodOf( method );
	}

	public Object instantiate(Object parent, SessionImplementor session) throws HibernateException {
		final boolean useParent = parent!=null &&
		                          //TODO: Yuck! This is not quite good enough, it's a quick
		                          //hack around the problem of having a to-one association
		                          //that refers to an embedded component:
		                          super.getReturnedClass().isInstance(parent);

		return useParent ? parent : super.instantiate(parent, session);
	}
}
