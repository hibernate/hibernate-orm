/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.List;
import java.util.Locale;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmIdBagCollectionType;
import org.hibernate.boot.model.source.spi.AttributeSourceContainer;
import org.hibernate.boot.model.source.spi.CollectionIdSource;
import org.hibernate.boot.model.source.spi.ColumnSource;
import org.hibernate.boot.model.source.spi.Orderable;
import org.hibernate.boot.model.source.spi.PluralAttributeNature;
import org.hibernate.boot.model.source.spi.RelationalValueSource;
import org.hibernate.boot.model.source.spi.SizeSource;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeSourceIdBagImpl extends AbstractPluralAttributeSourceImpl implements Orderable {
	private final JaxbHbmIdBagCollectionType idBagMapping;
	private final CollectionIdSource collectionIdSource;

	public PluralAttributeSourceIdBagImpl(
			MappingDocument mappingDocument,
			final JaxbHbmIdBagCollectionType idBagMapping,
			AttributeSourceContainer container) {
		super( mappingDocument, idBagMapping, container );
		this.idBagMapping = idBagMapping;

		final RelationalValueSource collectionIdRelationalValueSource = RelationalValueSourceHelper.buildValueSource(
				sourceMappingDocument(),
				null,
				new RelationalValueSourceHelper.AbstractColumnsAndFormulasSource() {
					@Override
					public XmlElementMetadata getSourceType() {
						return XmlElementMetadata.COLLECTION_ID;
					}

					@Override
					public String getSourceName() {
						return null;
					}

					@Override
					public String getColumnAttribute() {
						return idBagMapping.getCollectionId().getColumnAttribute();
					}

					@Override
					public SizeSource getSizeSource() {
						return Helper.interpretSizeSource(
								idBagMapping.getCollectionId().getLength(),
								(Integer) null,
								null
						);
					}

					@Override
					public List getColumnOrFormulaElements() {
						return idBagMapping.getCollectionId().getColumn();
					}
				}
		);

		if ( !ColumnSource.class.isInstance( collectionIdRelationalValueSource ) ) {
			throw new MappingException(
					String.format(
							Locale.ENGLISH,
							"Expecting column for collection id (idbag), but found formula [%s.%s]",
							container.getAttributeRoleBase().getFullPath(),
							idBagMapping.getName()
					),
					sourceMappingDocument().getOrigin()
			);
		}

		this.collectionIdSource = new CollectionIdSourceImpl(
				(ColumnSource) collectionIdRelationalValueSource,
				new HibernateTypeSourceImpl( idBagMapping.getCollectionId().getType() ),
				idBagMapping.getCollectionId().getGenerator().getClazz()
		);
	}

	@Override
	public PluralAttributeNature getNature() {
		return PluralAttributeNature.ID_BAG;
	}

	@Override
	public CollectionIdSource getCollectionIdSource() {
		return collectionIdSource;
	}

	@Override
	public boolean isOrdered() {
		return StringHelper.isNotEmpty( getOrder() );
	}

	@Override
	public String getOrder() {
		return idBagMapping.getOrderBy();
	}

	@Override
	public XmlElementMetadata getSourceType() {
		return XmlElementMetadata.ID_BAG;
	}

	@Override
	public String getXmlNodeName() {
		return idBagMapping.getNode();
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
