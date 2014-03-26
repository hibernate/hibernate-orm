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
import java.util.Map;

import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbColumnElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbIdElement;
import org.hibernate.metamodel.source.spi.AttributeSourceContainer;
import org.hibernate.metamodel.source.spi.HibernateTypeSource;
import org.hibernate.metamodel.source.spi.RelationalValueSource;
import org.hibernate.metamodel.source.spi.SingularAttributeSource;
import org.hibernate.metamodel.source.spi.SizeSource;
import org.hibernate.metamodel.source.spi.ToolingHintSource;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.NaturalIdMutability;
import org.hibernate.metamodel.spi.SingularAttributeNature;

/**
 * Implementation for {@code <id/>} mappings
 *
 * @author Steve Ebersole
 */
class SingularIdentifierAttributeSourceImpl
		extends AbstractHbmSourceNode
		implements SingularAttributeSource {

	private final JaxbIdElement idElement;
	private final HibernateTypeSource typeSource;
	private final List<RelationalValueSource> valueSources;

	private final AttributeRole attributeRole;
	private final AttributePath attributePath;

	public SingularIdentifierAttributeSourceImpl(
			MappingDocument mappingDocument,
			AttributeSourceContainer container,
			final JaxbIdElement idElement) {
		super( mappingDocument );
		this.idElement = idElement;
		this.typeSource = new HibernateTypeSource() {
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

			@Override
			public JavaTypeDescriptor getJavaType() {
				return null;
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
					public SizeSource getSizeSource() {
						return Helper.createSizeSourceIfMapped(
								idElement.getLength(),
								null,
								null
						);
					}

					@Override
					public List<JaxbColumnElement> getColumn() {
						return idElement.getColumn();
					}

					@Override
					public boolean isIncludedInInsertByDefault() {
						return true;
					}

                    @Override
                    public boolean isForceNotNull() {
                        return true;
                    }
                }
		);

		this.attributeRole = container.getAttributeRoleBase().append( getName() );
		this.attributePath = container.getAttributePathBase().append( getName() );
	}

	@Override
	public String getName() {
		return idElement.getName() == null
				? "id"
				: idElement.getName();
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
	public HibernateTypeSource getTypeInformation() {
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
		return false;
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return false;
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
		return idElement.getMeta();
	}
}
