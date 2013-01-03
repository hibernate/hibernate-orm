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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.jaxb.spi.hbm.JaxbColumnElement;
import org.hibernate.jaxb.spi.hbm.JaxbIndexElement;
import org.hibernate.jaxb.spi.hbm.JaxbListIndexElement;
import org.hibernate.metamodel.spi.binding.PluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.source.BasicPluralAttributeIndexSource;
import org.hibernate.metamodel.spi.source.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;

/**
 *
 */
public class ListAttributeIndexSource extends AbstractHbmSourceNode implements BasicPluralAttributeIndexSource {
	private final List< RelationalValueSource > valueSources;
	private final ExplicitHibernateTypeSource typeSource;
	private final int base;

	public ListAttributeIndexSource( MappingDocument sourceMappingDocument, final JaxbListIndexElement indexElement ) {
		super( sourceMappingDocument );
		valueSources = Helper.buildValueSources( sourceMappingDocument, new Helper.ValueSourcesAdapter() {

			List< JaxbColumnElement > columnElements = indexElement.getColumn() == null ? Collections.EMPTY_LIST : Collections.singletonList( indexElement.getColumn() );

			@Override
			public String getColumnAttribute() {
				return indexElement.getColumnAttribute();
			}

			@Override
			public List<JaxbColumnElement> getColumn() {
				return columnElements;
			}

			@Override
			public boolean isIncludedInInsertByDefault() {
				return areValuesIncludedInInsertByDefault();
			}

			@Override
			public boolean isIncludedInUpdateByDefault() {
				return areValuesIncludedInUpdateByDefault();
			}
		} );
		typeSource = new ExplicitHibernateTypeSource() {

			@Override
			public String getName() {
				return "integer";
			}

			@Override
			public Map< String, String > getParameters() {
				return java.util.Collections.< String, String >emptyMap();
			}
		};
		base = Integer.parseInt( indexElement.getBase() );
	}

	public ListAttributeIndexSource( MappingDocument sourceMappingDocument, final JaxbIndexElement indexElement ) {
		super( sourceMappingDocument );
		valueSources = Helper.buildValueSources( sourceMappingDocument, new Helper.ValueSourcesAdapter() {

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
		} );
		typeSource = new ExplicitHibernateTypeSource() {

			@Override
			public String getName() {
				return StringHelper.isEmpty( indexElement.getType() ) ? "integer" : indexElement.getType();
			}

			@Override
			public Map< String, String > getParameters() {
				return java.util.Collections.< String, String >emptyMap();
			}
		};
		base = 0;
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
	public int base() {
		return base;
	}

	@Override
	public PluralAttributeIndexBinding.Nature getNature() {
		return PluralAttributeIndexBinding.Nature.BASIC;
	}

	@Override
	public ExplicitHibernateTypeSource explicitHibernateTypeSource() {
		return typeSource;
	}

	@Override
	public List< RelationalValueSource > relationalValueSources() {
		return valueSources;
	}
}
