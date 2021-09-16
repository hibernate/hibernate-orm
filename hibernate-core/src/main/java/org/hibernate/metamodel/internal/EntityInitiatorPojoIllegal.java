/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import java.util.Locale;

import org.hibernate.InstantiationException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.EntityInstantiator;
import org.hibernate.persister.entity.EntityPersister;

/**
 * EntityInstantiator for cases where the underlying Java type cannot
 * be instantiated - generally, either the class is abstract or it does
 * not define an appropriate (no-arg, etc.) constructor
 *
 * @author Steve Ebersole
 */
public class EntityInitiatorPojoIllegal implements EntityInstantiator {
	private final EntityPersister runtimeMapping;

	public EntityInitiatorPojoIllegal(EntityPersister runtimeMapping) {
		this.runtimeMapping = runtimeMapping;
	}

	@Override
	public boolean canBeInstantiated() {
		return false;
	}

	@Override
	public Object instantiate(SessionFactoryImplementor sessionFactory) {
		throw new InstantiationException(
				String.format(
						Locale.ROOT,
						"Illegal attempt to instantiate entity `%s` (%s)",
						runtimeMapping.getJavaTypeDescriptor().getJavaTypeClass(),
						runtimeMapping.getEntityName()
				),
				runtimeMapping.getJavaTypeDescriptor().getJavaTypeClass()
		);
	}

	@Override
	public boolean isInstance(Object object, SessionFactoryImplementor sessionFactory) {
		return runtimeMapping.getJavaTypeDescriptor().getJavaTypeClass().isInstance( object );
	}

	@Override
	public boolean isSameClass(Object object, SessionFactoryImplementor sessionFactory) {
		// don't think this can happen
		return false;
	}
}
