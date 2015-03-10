/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
					public SizeSource getSizeSource() {
						return Helper.interpretSizeSource(
								jaxbElement.getLength(),
								jaxbElement.getPrecision(),
								jaxbElement.getScale()
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
