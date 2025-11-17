/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmIndexType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmMapKeyBasicType;
import org.hibernate.boot.model.source.spi.PluralAttributeIndexNature;
import org.hibernate.boot.model.source.spi.PluralAttributeMapKeySourceBasic;
import org.hibernate.boot.model.source.spi.RelationalValueSource;
import org.hibernate.boot.model.source.spi.SizeSource;

/**
 *
 */
public class PluralAttributeMapKeySourceBasicImpl
		extends AbstractHbmSourceNode
		implements PluralAttributeMapKeySourceBasic {
	private final HibernateTypeSourceImpl typeSource;
	private final List<RelationalValueSource> valueSources;

	private final String xmlNodeName;

	public PluralAttributeMapKeySourceBasicImpl(
			MappingDocument sourceMappingDocument,
			final JaxbHbmMapKeyBasicType jaxbMapKey) {
		super( sourceMappingDocument );
		this.typeSource = new HibernateTypeSourceImpl( jaxbMapKey );
		this.valueSources = RelationalValueSourceHelper.buildValueSources(
				sourceMappingDocument(),
				null,
				new RelationalValueSourceHelper.AbstractColumnsAndFormulasSource() {
					@Override
					public XmlElementMetadata getSourceType() {
						return XmlElementMetadata.MAP_KEY;
					}

					@Override
					public String getSourceName() {
						return null;
					}

					@Override
					public String getFormulaAttribute() {
						return jaxbMapKey.getFormulaAttribute();
					}

					@Override
					public String getColumnAttribute() {
						return jaxbMapKey.getColumnAttribute();
					}

					@Override
					public List getColumnOrFormulaElements() {
						return jaxbMapKey.getColumnOrFormula();
					}

					@Override
					public SizeSource getSizeSource() {
						return Helper.interpretSizeSource(
								jaxbMapKey.getLength(),
								(Integer) null,
								null
						);
					}
				}
		);
		this.xmlNodeName = jaxbMapKey.getNode();
	}

	public PluralAttributeMapKeySourceBasicImpl(MappingDocument sourceMappingDocument, final JaxbHbmIndexType jaxbIndex) {
		super( sourceMappingDocument );
		this.typeSource = new HibernateTypeSourceImpl( jaxbIndex.getType() );
		this.valueSources = RelationalValueSourceHelper.buildValueSources(
				sourceMappingDocument(),
				null,
				new RelationalValueSourceHelper.AbstractColumnsAndFormulasSource() {
					@Override
					public XmlElementMetadata getSourceType() {
						return XmlElementMetadata.MAP_KEY;
					}

					@Override
					public String getSourceName() {
						return null;
					}

					@Override
					public String getColumnAttribute() {
						return jaxbIndex.getColumnAttribute();
					}

					@Override
					public SizeSource getSizeSource() {
						return Helper.interpretSizeSource(
								jaxbIndex.getLength(),
								(Integer) null,
								null
						);
					}

					@Override
					public List getColumnOrFormulaElements() {
						return jaxbIndex.getColumn();
					}
				}
		);
		this.xmlNodeName = null;
	}

	@Override
	public PluralAttributeIndexNature getNature() {
		return PluralAttributeIndexNature.BASIC;
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
		return false;
	}

	@Override
	public HibernateTypeSourceImpl getTypeInformation() {
		return typeSource;
	}

	@Override
	public String getXmlNodeName() {
		return xmlNodeName;
	}
}
