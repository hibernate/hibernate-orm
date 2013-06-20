/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
