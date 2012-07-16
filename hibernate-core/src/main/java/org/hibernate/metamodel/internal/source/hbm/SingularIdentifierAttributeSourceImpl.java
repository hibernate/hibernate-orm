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
import org.hibernate.internal.jaxb.mapping.hbm.JaxbIdElement;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.source.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;
import org.hibernate.metamodel.spi.source.SingularAttributeNature;
import org.hibernate.metamodel.spi.source.SingularAttributeSource;

/**
 * Implementation for {@code <id/>} mappings
 *
 * @author Steve Ebersole
 */
class SingularIdentifierAttributeSourceImpl
		extends AbstractHbmSourceNode
		implements SingularAttributeSource {
	private final JaxbIdElement idElement;
	private final ExplicitHibernateTypeSource typeSource;
	private final List<RelationalValueSource> valueSources;

	public SingularIdentifierAttributeSourceImpl(
			MappingDocument mappingDocument,
			final JaxbIdElement idElement) {
		super( mappingDocument );
		this.idElement = idElement;
		this.typeSource = new ExplicitHibernateTypeSource() {
			private final String name = idElement.getTypeAttribute() != null
					? idElement.getTypeAttribute()
					: idElement.getType() != null
							? idElement.getType().getName()
							: null;
			private final Map<String, String> parameters = ( idElement.getType() != null )
					? Helper.extractParameters( idElement.getType().getParam() )
					: null;

			@Override
			public String getName() {
				return name;
			}

			@Override
			public Map<String, String> getParameters() {
				return parameters;
			}
		};
		this.valueSources = Helper.buildValueSources(
				sourceMappingDocument(),
				new Helper.ValueSourcesAdapter() {
					@Override
					public String getColumnAttribute() {
						return idElement.getColumnAttribute();
					}

					@Override
					public String getFormulaAttribute() {
						return null;
					}

					@Override
					public List getColumnOrFormulaElements() {
						return idElement.getColumn();
					}

					@Override
					public String getContainingTableName() {
						// by definition, the identifier should be bound to the primary table of the root entity
						return null;
					}

					@Override
					public boolean isIncludedInInsertByDefault() {
						return true;
					}

					@Override
					public boolean isIncludedInUpdateByDefault() {
						return false;
					}

                    @Override
                    public boolean isForceNotNull() {
                        return true;
                    }
                }
		);
	}

	@Override
	public String getName() {
		return idElement.getName() == null
				? "id"
				: idElement.getName();
	}

	@Override
	public ExplicitHibernateTypeSource getTypeInformation() {
		return typeSource;
	}

	@Override
	public String getPropertyAccessorName() {
		return idElement.getAccess();
	}

	@Override
	public PropertyGeneration getGeneration() {
		return PropertyGeneration.INSERT;
	}

	@Override
	public boolean isLazy() {
		return false;
	}

	@Override
	public SingularAttributeBinding.NaturalIdMutability getNaturalIdMutability() {
		return SingularAttributeBinding.NaturalIdMutability.NOT_NATURAL_ID;
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
		return true;
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return false;
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return false;
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
		return Helper.buildMetaAttributeSources( idElement.getMeta() );
	}
}
