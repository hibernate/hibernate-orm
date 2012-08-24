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
import org.hibernate.jaxb.spi.hbm.JaxbIndexElement;
import org.hibernate.jaxb.spi.hbm.JaxbMapKeyElement;
import org.hibernate.metamodel.spi.source.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.spi.source.PluralAttributeIndexSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;

/**
 *
 */
public class MapAttributeIndexSource extends AbstractHbmSourceNode implements PluralAttributeIndexSource {
	private final Nature nature;
	private final List<RelationalValueSource> valueSources;
	private final ExplicitHibernateTypeSource typeSource;

	public MapAttributeIndexSource(MappingDocument sourceMappingDocument, final JaxbMapKeyElement mapKey) {
		super( sourceMappingDocument );
		valueSources = Helper.buildValueSources(
				sourceMappingDocument(),
				new Helper.ValueSourcesAdapter() {

					@Override
					public String getColumnAttribute() {
						return mapKey.getColumnAttribute();
					}

					@Override
					public List<JaxbColumnElement> getColumn() {
						return mapKey.getColumn();
					}

					@Override
					public List<String> getFormula() {
						return mapKey.getFormula();
					}

					@Override
					public String getFormulaAttribute() {
						return mapKey.getFormulaAttribute();
					}

					@Override
					public boolean isIncludedInInsertByDefault() {
						return areValuesIncludedInInsertByDefault();
					}

					@Override
					public boolean isIncludedInUpdateByDefault() {
						return areValuesIncludedInUpdateByDefault();
					}
				}
		);
		this.typeSource = new ExplicitHibernateTypeSource() {
			@Override
			public String getName() {
				if ( mapKey.getTypeAttribute() != null ) {
					return mapKey.getTypeAttribute();
				}
				if ( mapKey.getType() != null ) {
					return mapKey.getType().getName();
				}
				return null;
			}

			@Override
			public Map<String, String> getParameters() {
				return mapKey.getType() != null
						? Helper.extractParameters( mapKey.getType().getParam() )
						: java.util.Collections.<String, String>emptyMap();
			}
		};
		this.nature = Nature.BASIC;
	}

	public MapAttributeIndexSource(MappingDocument sourceMappingDocument, final JaxbIndexElement indexElement) {
		super( sourceMappingDocument );
		valueSources = Helper.buildValueSources(
				sourceMappingDocument,
				new Helper.ValueSourcesAdapter() {

					@Override
					public String getColumnAttribute() {
						return indexElement.getColumnAttribute();
					}

					@Override
					public List<JaxbColumnElement> getColumn() {
						return indexElement.getColumn();
					}

					@Override
					public boolean isIncludedInInsertByDefault() {
						return areValuesIncludedInInsertByDefault();
					}

					@Override
					public boolean isIncludedInUpdateByDefault() {
						return areValuesIncludedInUpdateByDefault();
					}
				}
		);
		typeSource = new ExplicitHibernateTypeSource() {
			@Override
			public String getName() {
				return indexElement.getType();
			}

			@Override
			public Map<String, String> getParameters() {
				return java.util.Collections.<String, String>emptyMap();
			}
		};

		this.nature = Nature.BASIC;
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
		return false;
	}

	@Override
	public Nature getNature() {
		return nature;
	}

	@Override
	public ExplicitHibernateTypeSource explicitHibernateTypeSource() {
		return typeSource;
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		return valueSources;
	}
}
