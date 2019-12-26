/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.lang.reflect.Method;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.tuple.component.ComponentMetamodel;

/**
 * @author Gavin King
 */
public class EmbeddedComponentType extends ComponentType {

	/**
	 * @deprecated Use the other constructor
	 */
	@Deprecated
	public EmbeddedComponentType(TypeFactory.TypeScope typeScope, ComponentMetamodel metamodel) {
		super( metamodel );
	}

	public EmbeddedComponentType(ComponentMetamodel metamodel) {
		super( metamodel );
	}

	public boolean isEmbedded() {
		return true;
	}

	public boolean isMethodOf(Method method) {
		return componentTuplizer.isMethodOf( method );
	}

	@Override
	public Object instantiate(Object parent, SharedSessionContractImplementor session) throws HibernateException {
		final boolean useParent = parent != null &&
				//TODO: Yuck! This is not quite good enough, it's a quick
				//hack around the problem of having a to-one association
				//that refers to an embedded component:
				super.getReturnedClass().isInstance( parent );

		return useParent ? parent : super.instantiate( parent, session );
	}
}
