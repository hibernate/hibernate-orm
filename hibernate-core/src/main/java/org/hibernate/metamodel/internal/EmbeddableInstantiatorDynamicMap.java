/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Component;
import org.hibernate.metamodel.mapping.IEmbeddableMappingType;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;

/**
 * Support for instantiating embeddables as dynamic-map representation
 *
 * @author Steve Ebersole
 */
public class EmbeddableInstantiatorDynamicMap
		extends AbstractDynamicMapInstantiator
		implements EmbeddableInstantiator {
	private final Supplier<IEmbeddableMappingType> runtimeDescriptorAccess;

	public EmbeddableInstantiatorDynamicMap(
			Component bootDescriptor,
			Supplier<IEmbeddableMappingType> runtimeDescriptorAccess) {
		super( bootDescriptor.getRoleName() );
		this.runtimeDescriptorAccess = runtimeDescriptorAccess;
	}

	@Override
	public Object instantiate(Supplier<Object[]> valuesAccess, SessionFactoryImplementor sessionFactory) {
		final Map<?,?> dataMap = generateDataMap();

		if ( valuesAccess != null ) {
			final IEmbeddableMappingType mappingType = runtimeDescriptorAccess.get();
			mappingType.setPropertyValues( dataMap, valuesAccess.get() );
		}

		return dataMap;
	}
}
