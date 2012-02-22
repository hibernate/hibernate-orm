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
package org.hibernate.metamodel.internal.source.hbm;

import java.util.List;
import java.util.Map;

import org.hibernate.internal.jaxb.mapping.hbm.JaxbHibernateMapping;
import org.hibernate.internal.util.Value;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.spi.source.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;
import org.hibernate.metamodel.spi.source.SingularAttributeNature;
import org.hibernate.metamodel.spi.source.VersionAttributeSource;


/**
 * Implementation for {@code <version/>} mappings
 *
 * @author Steve Ebersole
 */
class VersionAttributeSourceImpl
		extends AbstractHbmSourceNode
		implements VersionAttributeSource {
	private final JaxbHibernateMapping.JaxbClass.JaxbVersion versionElement;
	private final List<RelationalValueSource> valueSources;

	VersionAttributeSourceImpl(
			MappingDocument mappingDocument,
			final JaxbHibernateMapping.JaxbClass.JaxbVersion versionElement) {
		super( mappingDocument );
		this.versionElement = versionElement;
		this.valueSources = Helper.buildValueSources(
				sourceMappingDocument(),
				new Helper.ValueSourcesAdapter() {
					@Override
					public String getColumnAttribute() {
						return versionElement.getColumnAttribute();
					}

					@Override
					public String getFormulaAttribute() {
						return null;
					}

					@Override
					public List getColumnOrFormulaElements() {
						return versionElement.getColumn();
					}

					@Override
					public String getContainingTableName() {
						// by definition the version should come from the primary table of the root entity.
						return null;
					}

					@Override
					public boolean isIncludedInInsertByDefault() {
						return Helper.getBooleanValue( versionElement.isInsert(), true );
					}

					@Override
					public boolean isIncludedInUpdateByDefault() {
						return true;
					}
				}
		);
	}

	private final ExplicitHibernateTypeSource typeSource = new ExplicitHibernateTypeSource() {
		@Override
		public String getName() {
			return versionElement.getType() == null ? "integer" : versionElement.getType();
		}

		@Override
		public Map<String, String> getParameters() {
			return null;
		}
	};

	@Override
	public String getUnsavedValue() {
		return versionElement.getUnsavedValue();
	}

	@Override
	public String getName() {
		return versionElement.getName();
	}

	@Override
	public ExplicitHibernateTypeSource getTypeInformation() {
		return typeSource;
	}

	@Override
	public String getPropertyAccessorName() {
		return versionElement.getAccess();
	}

	private Value<PropertyGeneration> propertyGenerationValue = new Value<PropertyGeneration>(
			new Value.DeferredInitializer<PropertyGeneration>() {
				@Override
				public PropertyGeneration initialize() {
					final PropertyGeneration propertyGeneration = versionElement.getGenerated() == null
							? PropertyGeneration.NEVER
							: PropertyGeneration.parse( versionElement.getGenerated().value() );
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
	public SingularAttributeNature getNature() {
		return SingularAttributeNature.BASIC;
	}

	@Override
	public boolean isVirtualAttribute() {
		return false;
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return Helper.getBooleanValue( versionElement.isInsert(), true );
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
	public List<RelationalValueSource> relationalValueSources() {
		return valueSources;
	}

	@Override
	public boolean isSingular() {
		return true;
	}

	@Override
	public Iterable<MetaAttributeSource> metaAttributes() {
		return Helper.buildMetaAttributeSources( versionElement.getMeta() );
	}
}
