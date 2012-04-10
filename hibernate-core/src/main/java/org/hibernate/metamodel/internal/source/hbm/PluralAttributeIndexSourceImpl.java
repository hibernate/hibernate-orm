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

import org.hibernate.internal.jaxb.mapping.hbm.JaxbColumnElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbIndexElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbListIndexElement;
import org.hibernate.metamodel.spi.source.AttributeSourceContainer;
import org.hibernate.metamodel.spi.source.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.spi.source.PluralAttributeIndexSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;

/**
 *
 */
public class PluralAttributeIndexSourceImpl extends AbstractHbmSourceNode implements PluralAttributeIndexSource {

	private final List< RelationalValueSource > valueSources;
	private final ExplicitHibernateTypeSource typeSource;
	private final int base;

	public PluralAttributeIndexSourceImpl(
			MappingDocument mappingDocument,
			final JaxbListIndexElement indexElement,
			final AttributeSourceContainer container ) {
		super( mappingDocument );
		valueSources = Helper.buildValueSources( sourceMappingDocument(), new Helper.ValueSourcesAdapter() {

			List< JaxbColumnElement > columnElements = indexElement.getColumn() == null
					? Collections.EMPTY_LIST
					: Collections.singletonList( indexElement.getColumn() );

			@Override
			public String getColumnAttribute() {
				return indexElement.getColumnAttribute();
			}

			@Override
			public List getColumnOrFormulaElements() {
				return columnElements;
			}

			@Override
			public String getContainingTableName() {
				return null;
			}

			@Override
			public String getFormulaAttribute() {
				return null;
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

	public PluralAttributeIndexSourceImpl(
			MappingDocument mappingDocument,
			final JaxbIndexElement indexElement,
			final AttributeSourceContainer container ) {
		super( mappingDocument );
		valueSources = Helper.buildValueSources( sourceMappingDocument(), new Helper.ValueSourcesAdapter() {

			List< JaxbColumnElement > columnElements = indexElement.getColumn() == null
					? Collections.EMPTY_LIST
					: Collections.singletonList( indexElement.getColumn() );

			@Override
			public String getColumnAttribute() {
				return indexElement.getColumnAttribute();
			}

			@Override
			public List getColumnOrFormulaElements() {
				return columnElements;
			}

			@Override
			public String getContainingTableName() {
				return null;
			}

			@Override
			public String getFormulaAttribute() {
				return null;
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

		base = 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.metamodel.spi.source.ColumnBindingDefaults#areValuesIncludedInInsertByDefault()
	 */
	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.metamodel.spi.source.ColumnBindingDefaults#areValuesIncludedInUpdateByDefault()
	 */
	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.metamodel.spi.source.ColumnBindingDefaults#areValuesNullableByDefault()
	 */
	@Override
	public boolean areValuesNullableByDefault() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.metamodel.spi.source.PluralAttributeIndexSource#base()
	 */
	@Override
	public int base() {
		return base;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.metamodel.spi.source.PluralAttributeIndexSource#explicitHibernateTypeSource()
	 */
	@Override
	public ExplicitHibernateTypeSource explicitHibernateTypeSource() {
		return typeSource;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.metamodel.spi.source.RelationalValueSourceContainer#relationalValueSources()
	 */
	@Override
	public List< RelationalValueSource > relationalValueSources() {
		return valueSources;
	}
}
