/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.walking.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.persister.walking.spi.AnyMappingDefinition;
import org.hibernate.type.AnyType;
import org.hibernate.type.MetaType;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class StandardAnyTypeDefinition implements AnyMappingDefinition {
	private final AnyType anyType;
	private final boolean definedAsLazy;
	private final List<DiscriminatorMapping> discriminatorMappings;

	public StandardAnyTypeDefinition(AnyType anyType, boolean definedAsLazy) {
		this.anyType = anyType;
		this.definedAsLazy = definedAsLazy;
		this.discriminatorMappings = interpretDiscriminatorMappings( anyType );
	}

	private static List<DiscriminatorMapping> interpretDiscriminatorMappings(AnyType anyType) {
		final Type discriminatorType = anyType.getDiscriminatorType();
		if ( ! MetaType.class.isInstance( discriminatorType ) ) {
			return Collections.emptyList();
		}

		final MetaType metaType = (MetaType) discriminatorType;
		final List<DiscriminatorMapping> discriminatorMappings = new ArrayList<DiscriminatorMapping>();
		for ( final Map.Entry<Object,String> entry : metaType.getDiscriminatorValuesToEntityNameMap().entrySet() ) {
			discriminatorMappings.add(
					new DiscriminatorMapping() {
						private final Object discriminatorValue = entry.getKey();
						private final String entityName = entry.getValue();

						@Override
						public Object getDiscriminatorValue() {
							return discriminatorValue;
						}

						@Override
						public String getEntityName() {
							return entityName;
						}
					}
			);
		}
		return discriminatorMappings;
	}

	@Override
	public AnyType getType() {
		return anyType;
	}

	@Override
	public boolean isLazy() {
		return definedAsLazy;
	}

	@Override
	public Type getIdentifierType() {
		return anyType.getIdentifierType();
	}

	@Override
	public Type getDiscriminatorType() {
		return anyType.getDiscriminatorType();
	}

	@Override
	public Iterable<DiscriminatorMapping> getMappingDefinedDiscriminatorMappings() {
		return discriminatorMappings;
	}
}
