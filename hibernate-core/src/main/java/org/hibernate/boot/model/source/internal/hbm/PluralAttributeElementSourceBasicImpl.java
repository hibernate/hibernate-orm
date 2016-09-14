/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicCollectionElementType;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.PluralAttributeElementNature;
import org.hibernate.boot.model.source.spi.PluralAttributeElementSourceBasic;
import org.hibernate.boot.model.source.spi.PluralAttributeSource;
import org.hibernate.boot.model.source.spi.RelationalValueSource;
import org.hibernate.boot.model.source.spi.RelationalValueSourceContainer;
import org.hibernate.boot.model.source.spi.SizeSource;
import org.hibernate.boot.spi.MetadataBuildingContext;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeElementSourceBasicImpl
		extends AbstractHbmSourceNode
		implements PluralAttributeElementSourceBasic, RelationalValueSourceContainer {
	private final PluralAttributeSource pluralAttributeSource;
	private final HibernateTypeSourceImpl typeSource;
	private final List<RelationalValueSource> valueSources;

	public PluralAttributeElementSourceBasicImpl(
			MappingDocument sourceMappingDocument,
			PluralAttributeSource pluralAttributeSource,
			final JaxbHbmBasicCollectionElementType jaxbElement) {
		super( sourceMappingDocument );
		this.pluralAttributeSource = pluralAttributeSource;

		this.typeSource = new HibernateTypeSourceImpl( jaxbElement );

		this.valueSources = RelationalValueSourceHelper.buildValueSources(
				sourceMappingDocument(),
				null,
				new RelationalValueSourceHelper.AbstractColumnsAndFormulasSource() {
					@Override
					public XmlElementMetadata getSourceType() {
						return XmlElementMetadata.ELEMENT;
					}

					@Override
					public String getSourceName() {
						return null;
					}

					@Override
					public String getColumnAttribute() {
						return jaxbElement.getColumnAttribute();
					}

					@Override
					public String getFormulaAttribute() {
						return jaxbElement.getFormulaAttribute();
					}

					@Override
					public List getColumnOrFormulaElements() {
						return jaxbElement.getColumnOrFormula();
					}

					@Override
					public Boolean isNullable() {
						return !jaxbElement.isNotNull();
					}

					@Override
					public boolean isUnique() {
						return jaxbElement.isUnique();
					}

					@Override
					public SizeSource getSizeSource() {
						return Helper.interpretSizeSource(
								jaxbElement.getLength(),
								jaxbElement.getScale(),
								jaxbElement.getPrecision()
						);
					}
				}
		);
	}

	@Override
	public PluralAttributeElementNature getNature() {
		return PluralAttributeElementNature.BASIC;
	}

	@Override
	public List<RelationalValueSource> getRelationalValueSources() {
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
	public HibernateTypeSourceImpl getExplicitHibernateTypeSource() {
		return typeSource;
	}

	@Override
	public AttributePath getAttributePath() {
		return pluralAttributeSource.getAttributePath();
	}

	@Override
	public boolean isCollectionElement() {
		return true;
	}

	@Override
	public MetadataBuildingContext getBuildingContext() {
		return metadataBuildingContext();
	}
}
