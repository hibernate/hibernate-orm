/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import java.util.function.Supplier;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Component;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;

/**
 * Support for instantiating embeddables as dynamic-map representation
 *
 * @author Steve Ebersole
 */
public class EmbeddableInstantiatorDynamicMap
		extends AbstractDynamicMapInstantiator
		implements EmbeddableInstantiator {
	public EmbeddableInstantiatorDynamicMap(Component bootDescriptor) {
		super( bootDescriptor.getRoleName() );
	}

	@Override
	public Object instantiate(Supplier<Object[]> valuesAccess, SessionFactoryImplementor sessionFactory) {
		return generateDataMap();
	}
}
