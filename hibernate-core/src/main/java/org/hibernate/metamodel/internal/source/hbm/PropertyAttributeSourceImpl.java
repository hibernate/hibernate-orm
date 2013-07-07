/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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

import org.hibernate.jaxb.spi.hbm.JaxbColumnElement;
import org.hibernate.jaxb.spi.hbm.JaxbPropertyElement;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.source.HibernateTypeSource;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;
import org.hibernate.metamodel.spi.source.SingularAttributeSource;
import org.hibernate.metamodel.spi.source.SizeSource;

/**
 * Implementation for {@code <property/>} mappings
 *
 * @author Steve Ebersole
 */
class PropertyAttributeSourceImpl extends AbstractHbmSourceNode implements SingularAttributeSource {
	private final JaxbPropertyElement propertyElement;
	private final HibernateTypeSource typeSource;
	private final List<RelationalValueSource> valueSources;
	private final SingularAttributeBinding.NaturalIdMutability naturalIdMutability;
	private final String containingTableName;

	PropertyAttributeSourceImpl(
			MappingDocument sourceMappingDocument,
			final JaxbPropertyElement propertyElement,
			final String logicalTableName,
			SingularAttributeBinding.NaturalIdMutability naturalIdMutability) {
		super( sourceMappingDocument );
		this.propertyElement = propertyElement;
		this.typeSource = new HibernateTypeSource() {
			private final String name = propertyElement.getTypeAttribute() != null
					? propertyElement.getTypeAttribute()
					: propertyElement.getType() != null
							? propertyElement.getType().getName()
							: null;
			private final Map<String, String> parameters = ( propertyElement.getType() != null )
					? Helper.extractParameters( propertyElement.getType().getParam() )
					: null;

			@Override
			public String getName() {
				return name;
			}

			@Override
			public Map<String, String> getParameters() {
				return parameters;
			}
			@Override
			public Class getJavaType() {
				return null;
			}
		};
		this.containingTableName = logicalTableName;
		this.valueSources = Helper.buildValueSources(
				sourceMappingDocument(),
				new Helper.ValueSourcesAdapter() {
					@Override
					public String getColumnAttribute() {
						return propertyElement.getColumnAttribute();
					}

					@Override
					public SizeSource getSizeSource() {
						// TODO: propertyElement.getPrecision() and getScale() return String,
						//       but should return int
						return Helper.createSizeSourceIfMapped(
								propertyElement.getLength(),
								propertyElement.getPrecision() == null ? null : Integer.valueOf( propertyElement.getPrecision() ),
								propertyElement.getScale() == null ? null : Integer.valueOf( propertyElement.getScale() )
						);
					}

					@Override
					public String getFormulaAttribute() {
						return propertyElement.getFormulaAttribute();
					}

					@Override
					public List<JaxbColumnElement> getColumn() {
						return propertyElement.getColumn();
					}

					@Override
					public List<String> getFormula() {
						return propertyElement.getFormula();
					}

					@Override
					public String getContainingTableName() {
						return logicalTableName;
					}

					@Override
					public boolean isIncludedInInsertByDefault() {
						return Helper.getValue( propertyElement.isInsert(), true );
					}

					@Override
					public boolean isIncludedInUpdateByDefault() {
						return Helper.getValue( propertyElement.isUpdate(), true );
					}
				}
		);
		this.naturalIdMutability = naturalIdMutability;
	}

	@Override
	public String getName() {
		return propertyElement.getName();
	}

	@Override
	public HibernateTypeSource getTypeInformation() {
		return typeSource;
	}

	@Override
	public String getPropertyAccessorName() {
		return propertyElement.getAccess();
	}

	@Override
	public PropertyGeneration getGeneration() {
		return PropertyGeneration.parse( propertyElement.getGenerated().value() );
	}

	@Override
	public boolean isLazy() {
		return Helper.getValue( propertyElement.isLazy(), false );
	}

	@Override
	public SingularAttributeBinding.NaturalIdMutability getNaturalIdMutability() {
		return naturalIdMutability;
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return Helper.getValue( propertyElement.isOptimisticLock(), true );
	}

	@Override
	public Nature getNature() {
		return Nature.BASIC;
	}

	@Override
	public boolean isVirtualAttribute() {
		return false;
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return Helper.getValue( propertyElement.isInsert(), true );
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return Helper.getValue( propertyElement.isUpdate(), true );
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return ! Helper.getValue( propertyElement.isNotNull(), false );
	}

	@Override
	public String getContainingTableName() {
		return containingTableName;
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
	public Iterable<? extends MetaAttributeSource> getMetaAttributeSources() {
		return propertyElement.getMeta();
	}
}
