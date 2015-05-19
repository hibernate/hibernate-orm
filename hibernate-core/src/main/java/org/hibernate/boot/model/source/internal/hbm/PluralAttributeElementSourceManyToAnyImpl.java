/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmAnyValueMappingType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToAnyCollectionElementType;
import org.hibernate.boot.model.source.spi.AnyDiscriminatorSource;
import org.hibernate.boot.model.source.spi.AnyKeySource;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.HibernateTypeSource;
import org.hibernate.boot.model.source.spi.PluralAttributeElementNature;
import org.hibernate.boot.model.source.spi.PluralAttributeElementSourceManyToAny;
import org.hibernate.boot.model.source.spi.RelationalValueSource;
import org.hibernate.boot.spi.MetadataBuildingContext;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeElementSourceManyToAnyImpl
		implements PluralAttributeElementSourceManyToAny {
	private final String cascade;

	private final AnyDiscriminatorSource discriminatorSource;
	private final AnyKeySource keySource;

	public PluralAttributeElementSourceManyToAnyImpl(
			final MappingDocument mappingDocument,
			final AbstractPluralAttributeSourceImpl pluralAttributeSource,
			final JaxbHbmManyToAnyCollectionElementType jaxbManyToAnyMapping,
			String cascade) {
		this.cascade = cascade;

		final List<RelationalValueSource> relationalValueSources = RelationalValueSourceHelper.buildValueSources(
				mappingDocument,
				null,
				new RelationalValueSourceHelper.AbstractColumnsAndFormulasSource() {
					@Override
					public XmlElementMetadata getSourceType() {
						return XmlElementMetadata.MANY_TO_ANY;
					}

					@Override
					public String getSourceName() {
						return null;
					}

					@Override
					public List getColumnOrFormulaElements() {
						return jaxbManyToAnyMapping.getColumn();
					}
				}
		);

		// the list of relational values should contain 2 or more values:
		//		* the first represents the discriminator
		//		* the rest represent the fk

		if ( relationalValueSources.size() < 2 ) {
			throw new MappingException(
					String.format(
							Locale.ENGLISH,
							"<many-to-any /> mapping [%s] needs to specify 2 or more columns",
							pluralAttributeSource.getAttributeRole().getFullPath()
					),
					mappingDocument.getOrigin()
			);
		}

		this.discriminatorSource = new AnyDiscriminatorSource() {
			private final HibernateTypeSource discriminatorTypeSource = new HibernateTypeSourceImpl( jaxbManyToAnyMapping.getMetaType() );
			private final RelationalValueSource discriminatorRelationalValueSource = relationalValueSources.get( 0 );
			private final Map<String,String> discriminatorValueMapping = new HashMap<String, String>();
			{
				for ( JaxbHbmAnyValueMappingType valueMapping : jaxbManyToAnyMapping.getMetaValue() ) {
					discriminatorValueMapping.put(
							valueMapping.getValue(),
							mappingDocument.qualifyClassName( valueMapping.getClazz() )
					);
				}
			}

			@Override
			public HibernateTypeSource getTypeSource() {
				return discriminatorTypeSource;
			}

			@Override
			public RelationalValueSource getRelationalValueSource() {
				return discriminatorRelationalValueSource;
			}

			@Override
			public Map<String, String> getValueMappings() {
				return discriminatorValueMapping;
			}

			@Override
			public AttributePath getAttributePath() {
				return pluralAttributeSource.getAttributePath();
			}

			@Override
			public MetadataBuildingContext getBuildingContext() {
				return mappingDocument;
			}
		};

		this.keySource = new AnyKeySource() {
			private final HibernateTypeSource fkTypeSource = new HibernateTypeSourceImpl( jaxbManyToAnyMapping.getIdType() );
			private final List<RelationalValueSource> fkRelationalValueSources = relationalValueSources.subList( 1, relationalValueSources.size() );

			@Override
			public HibernateTypeSource getTypeSource() {
				return fkTypeSource;
			}

			@Override
			public List<RelationalValueSource> getRelationalValueSources() {
				return fkRelationalValueSources;
			}

			@Override
			public AttributePath getAttributePath() {
				return pluralAttributeSource.getAttributePath();
			}

			@Override
			public MetadataBuildingContext getBuildingContext() {
				return mappingDocument;
			}
		};
	}

	@Override
	public AnyDiscriminatorSource getDiscriminatorSource() {
		return discriminatorSource;
	}

	@Override
	public AnyKeySource getKeySource() {
		return keySource;
	}

	@Override
	public PluralAttributeElementNature getNature() {
		return PluralAttributeElementNature.MANY_TO_ANY;
	}
}
