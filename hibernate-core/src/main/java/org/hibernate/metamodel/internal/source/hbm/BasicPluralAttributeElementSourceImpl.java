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

import org.hibernate.jaxb.spi.hbm.JaxbColumnElement;
import org.hibernate.jaxb.spi.hbm.JaxbElementElement;
import org.hibernate.metamodel.spi.source.BasicPluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.HibernateTypeSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;
import org.hibernate.metamodel.spi.source.SizeSource;

/**
 * @author Steve Ebersole
 */
public class BasicPluralAttributeElementSourceImpl
		extends AbstractHbmSourceNode
		implements BasicPluralAttributeElementSource {
	private final List<RelationalValueSource> valueSources;
	private final HibernateTypeSource typeSource;

	public BasicPluralAttributeElementSourceImpl(
			MappingDocument sourceMappingDocument,
			final JaxbElementElement elementElement) {
		super( sourceMappingDocument );
		this.valueSources = Helper.buildValueSources(
				sourceMappingDocument(),
				new Helper.ValueSourcesAdapter() {
					@Override
					public boolean isIncludedInInsertByDefault() {
						return BasicPluralAttributeElementSourceImpl.this.areValuesIncludedInInsertByDefault();
					}

					@Override
					public boolean isIncludedInUpdateByDefault() {
						return BasicPluralAttributeElementSourceImpl.this.areValuesIncludedInUpdateByDefault();
					}

					@Override
					public String getColumnAttribute() {
						return elementElement.getColumnAttribute();
					}

					@Override
					public String getFormulaAttribute() {
						return elementElement.getFormulaAttribute();
					}

					@Override
					public SizeSource getSizeSource() {
						return Helper.createSizeSourceIfMapped(
								elementElement.getLength(),
								elementElement.getPrecision(),
								elementElement.getScale()
						);
					}

					@Override
					public List<JaxbColumnElement> getColumn() {
						return elementElement.getColumn();
					}

					@Override
					public List<String> getFormula() {
						return elementElement.getFormula();
					}

					@Override
					public boolean isForceNotNull() {
						return elementElement.isNotNull();
					}
				}
		);

		this.typeSource = new HibernateTypeSource() {
			@Override
			public String getName() {
				if ( elementElement.getTypeAttribute() != null ) {
					return elementElement.getTypeAttribute();
				}
				else if ( elementElement.getType() != null ) {
					return elementElement.getType().getName();
				}
				else {
					return null;
				}
			}

			@Override
			public Map<String, String> getParameters() {
				return elementElement.getType() != null
						? Helper.extractParameters( elementElement.getType().getParam() )
						: java.util.Collections.<String, String>emptyMap();
			}
			@Override
			public Class getJavaType() {
				return null;
			}
		};
	}

	@Override
	public Nature getNature() {
		return Nature.BASIC;
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		return valueSources;
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
	public HibernateTypeSource getExplicitHibernateTypeSource() {
		return typeSource;
	}
}
