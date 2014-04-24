/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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

import java.util.List;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbBagElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbColumnElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbIdbagElement;
import org.hibernate.metamodel.source.spi.CollectionIdSource;
import org.hibernate.metamodel.source.spi.ColumnSource;
import org.hibernate.metamodel.source.spi.Orderable;
import org.hibernate.metamodel.source.spi.RelationalValueSource;
import org.hibernate.metamodel.source.spi.SizeSource;
import org.hibernate.metamodel.spi.PluralAttributeNature;

/**
 * @author Steve Ebersole
 */
public class IdBagSourceImpl extends AbstractPluralAttributeSourceImpl implements Orderable {
	public final CollectionIdSource collectionIdSource;

	public IdBagSourceImpl(
			MappingDocument mappingDocument,
			final JaxbIdbagElement element,
			AbstractEntitySourceImpl abstractEntitySource) {
		super( mappingDocument, element, abstractEntitySource );

		final List<RelationalValueSource> relationalValueSources = Helper.buildValueSources(
				sourceMappingDocument(),
				new Helper.ValueSourcesAdapter() {
					@Override
					public String getColumnAttribute() {
						return element.getCollectionId().getColumnAttribute();
					}

					@Override
					public SizeSource getSizeSource() {
						return Helper.createSizeSourceIfMapped(
								element.getCollectionId().getLength(),
								null,
								null
						);
					}

					@Override
					public List<JaxbColumnElement> getColumn() {
						return element.getCollectionId().getColumn();
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

		ColumnSource collectionIdColumnSource = null;
		if ( relationalValueSources != null && relationalValueSources.isEmpty() ) {
			if ( relationalValueSources.size() > 1 ) {
				throw makeMappingException( "Expecting just a single column for collection id (idbag)" );
			}

			final RelationalValueSource relationalValueSource = relationalValueSources.get( 0 );
			if ( !ColumnSource.class.isInstance( relationalValueSource ) ) {
				throw makeMappingException( "Expecting column for collection id (idbag), but found formula" );
			}

			collectionIdColumnSource = (ColumnSource) relationalValueSource;
		}

		final HibernateTypeSourceImpl typeSource = new HibernateTypeSourceImpl( element.getCollectionId().getType() );
		this.collectionIdSource = new CollectionIdSourceImpl(
				collectionIdColumnSource,
				typeSource,
				element.getCollectionId().getGenerator().getClazz()
		);
	}

	@Override
	public PluralAttributeNature getNature() {
		return PluralAttributeNature.ID_BAG;
	}

	@Override
	public JaxbBagElement getPluralAttributeElement() {
		return (JaxbBagElement) super.getPluralAttributeElement();
	}

	@Override
	public boolean isOrdered() {
		return StringHelper.isNotEmpty( getOrder() );
	}

	@Override
	public String getOrder() {
		return getPluralAttributeElement().getOrderBy();
	}

	private static class CollectionIdSourceImpl implements CollectionIdSource {
		private final ColumnSource columnSource;
		private final HibernateTypeSourceImpl typeSource;
		private final String generator;

		public CollectionIdSourceImpl(
				ColumnSource columnSource,
				HibernateTypeSourceImpl typeSource,
				String generator) {
			this.columnSource = columnSource;
			this.typeSource = typeSource;
			this.generator = generator;
		}

		@Override
		public ColumnSource getColumnSource() {
			return columnSource;
		}

		@Override
		public HibernateTypeSourceImpl getTypeInformation() {
			return typeSource;
		}

		@Override
		public String getGeneratorName() {
			return generator;
		}
	}
}
