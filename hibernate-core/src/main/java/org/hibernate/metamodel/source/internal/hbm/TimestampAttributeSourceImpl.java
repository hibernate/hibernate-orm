/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal.hbm;

import java.util.Collection;
import java.util.List;

import org.hibernate.internal.util.ValueHolder;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbTimestampElement;
import org.hibernate.metamodel.source.spi.RelationalValueSource;
import org.hibernate.metamodel.source.spi.ToolingHintSource;
import org.hibernate.metamodel.source.spi.VersionAttributeSource;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.NaturalIdMutability;
import org.hibernate.metamodel.spi.SingularAttributeNature;

/**
 * Implementation for {@code <timestamp/>} mappings
 *
 * @author Steve Ebersole
 */
class TimestampAttributeSourceImpl
		extends AbstractHbmSourceNode
		implements VersionAttributeSource {
	private final JaxbTimestampElement timestampElement;
	private final HibernateTypeSourceImpl typeSource;

	private final List<RelationalValueSource> valueSources;

	private final AttributePath attributePath;
	private final AttributeRole attributeRole;

	TimestampAttributeSourceImpl(
			MappingDocument mappingDocument,
			RootEntitySourceImpl rootEntitySource,
			final JaxbTimestampElement timestampElement) {
		super( mappingDocument );
		this.timestampElement = timestampElement;

		this.typeSource = new HibernateTypeSourceImpl(
				"db".equals( timestampElement.getSource().value() )
						? "dbtimestamp"
						: "timestamp"
		);

		this.valueSources = Helper.buildValueSources(
				sourceMappingDocument(),
				new Helper.ValueSourcesAdapter() {
					@Override
					public String getColumnAttribute() {
						return timestampElement.getColumnAttribute();
					}
					@Override
					public boolean isIncludedInInsertByDefault() {
						return true;
					}

					@Override
					public boolean isIncludedInUpdateByDefault() {
						return true;
					}
				}
		);

		this.attributePath = rootEntitySource.getAttributePathBase().append( getName() );
		this.attributeRole = rootEntitySource.getAttributeRoleBase().append( getName() );
	}

	@Override
	public String getName() {
		return timestampElement.getName();
	}

	@Override
	public AttributePath getAttributePath() {
		return attributePath;
	}

	@Override
	public AttributeRole getAttributeRole() {
		return attributeRole;
	}

	@Override
	public HibernateTypeSourceImpl getTypeInformation() {
		return typeSource;
	}

	@Override
	public String getPropertyAccessorName() {
		return timestampElement.getAccess();
	}

	private ValueHolder<PropertyGeneration> propertyGenerationValue = new ValueHolder<PropertyGeneration>(
			new ValueHolder.DeferredInitializer<PropertyGeneration>() {
				@Override
				public PropertyGeneration initialize() {
					final PropertyGeneration propertyGeneration = timestampElement.getGenerated() == null
							? PropertyGeneration.NEVER
							: PropertyGeneration.parse( timestampElement.getGenerated().value() );
					if ( propertyGeneration == PropertyGeneration.INSERT ) {
						throw makeMappingException( "'generated' attribute cannot be 'insert' for versioning property" );
					}
					return propertyGeneration;
				}
			}
	);

	@Override
	public PropertyGeneration getGeneration() {
		return propertyGenerationValue.getValue();
	}

	@Override
	public boolean isLazy() {
		return false;
	}

	@Override
	public NaturalIdMutability getNaturalIdMutability() {
		return NaturalIdMutability.NOT_NATURAL_ID;
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return false;
	}

	@Override
	public SingularAttributeNature getSingularAttributeNature() {
		return SingularAttributeNature.BASIC;
	}

	@Override
	public boolean isVirtualAttribute() {
		return false;
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return true;
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return true;
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return true;
	}

	@Override
	public String getContainingTableName() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		return valueSources;
	}

	@Override
	public boolean isSingular() {
		return true;
	}

	@Override
	public Collection<? extends ToolingHintSource> getToolingHintSources() {
		return timestampElement.getMeta();
	}

	@Override
	public String getUnsavedValue() {
		return timestampElement.getUnsavedValue().value();
	}
}
