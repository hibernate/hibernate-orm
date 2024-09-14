/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.internal;

import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Component;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.ValueAccess;

/**
 * Support for instantiating embeddables as dynamic-map representation
 *
 * @author Steve Ebersole
 */
public class EmbeddableInstantiatorDynamicMap
		extends AbstractDynamicMapInstantiator
		implements StandardEmbeddableInstantiator {
	private final Supplier<EmbeddableMappingType> runtimeDescriptorAccess;

	public EmbeddableInstantiatorDynamicMap(
			Component bootDescriptor,
			Supplier<EmbeddableMappingType> runtimeDescriptorAccess) {
		super( bootDescriptor.getRoleName() );
		this.runtimeDescriptorAccess = runtimeDescriptorAccess;
	}

	@Override
	public Object instantiate(ValueAccess valuesAccess, SessionFactoryImplementor sessionFactory) {
		final Map<?,?> dataMap = generateDataMap();

		Object[] values = valuesAccess == null ? null : valuesAccess.getValues();
		if ( values != null ) {
			final EmbeddableMappingType mappingType = runtimeDescriptorAccess.get();
			mappingType.setValues( dataMap, values );
		}

		return dataMap;
	}
}
