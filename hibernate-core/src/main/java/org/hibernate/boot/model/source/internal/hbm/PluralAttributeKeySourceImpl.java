/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmKeyType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToOneType;
import org.hibernate.boot.model.source.spi.AttributeSourceContainer;
import org.hibernate.boot.model.source.spi.PluralAttributeKeySource;
import org.hibernate.boot.model.source.spi.RelationalValueSource;
import org.hibernate.boot.model.source.spi.RelationalValueSourceContainer;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeKeySourceImpl
		extends AbstractHbmSourceNode
		implements PluralAttributeKeySource, RelationalValueSourceContainer {

	private final String explicitFkName;
	private final String referencedPropertyName;
	private final boolean cascadeDeletesAtFkLevel;
	private final boolean nullable;
	private final boolean updateable;

	private final List<RelationalValueSource> valueSources;

	public PluralAttributeKeySourceImpl(
			MappingDocument mappingDocument,
			final JaxbHbmKeyType jaxbKey,
			final AttributeSourceContainer container) {
		super( mappingDocument );

		this.explicitFkName = StringHelper.nullIfEmpty( jaxbKey.getForeignKey() );
		this.referencedPropertyName = StringHelper.nullIfEmpty( jaxbKey.getPropertyRef() );
		this.cascadeDeletesAtFkLevel = jaxbKey.getOnDelete() != null
				&& "cascade".equals( jaxbKey.getOnDelete().value() );
		this.nullable = jaxbKey.isNotNull() == null || !jaxbKey.isNotNull();
		this.updateable = jaxbKey.isUpdate() == null || jaxbKey.isUpdate();

		this.valueSources = RelationalValueSourceHelper.buildValueSources(
				sourceMappingDocument(),
				null, // todo : collection table name
				new RelationalValueSourceHelper.AbstractColumnsAndFormulasSource() {
					@Override
					public XmlElementMetadata getSourceType() {
						return XmlElementMetadata.KEY;
					}

					@Override
					public String getSourceName() {
						return null;
					}

					@Override
					public String getColumnAttribute() {
						return StringHelper.nullIfEmpty( jaxbKey.getColumnAttribute() );
					}

					@Override
					public List getColumnOrFormulaElements() {
						return jaxbKey.getColumn();
					}

				}
		);
	}

	public PluralAttributeKeySourceImpl(
			MappingDocument mappingDocument,
			final JaxbHbmKeyType jaxbKey,
			final JaxbHbmManyToOneType jaxbManyToOne,
			final AttributeSourceContainer container) {
		super( mappingDocument );

		this.explicitFkName = StringHelper.nullIfEmpty( jaxbManyToOne.getForeignKey() );
		this.referencedPropertyName = StringHelper.nullIfEmpty( jaxbManyToOne.getPropertyRef() );
		if ( jaxbKey.getOnDelete() == null ) {
			this.cascadeDeletesAtFkLevel = jaxbManyToOne.getOnDelete() != null && "cascade".equals( jaxbManyToOne.getOnDelete().value() );
		}
		else {
			this.cascadeDeletesAtFkLevel = "cascade".equals( jaxbKey.getOnDelete().value() );
		}
		if ( jaxbKey.isNotNull() == null ) {
			this.nullable = jaxbManyToOne.isNotNull() == null || !jaxbManyToOne.isNotNull();
		}
		else {
			this.nullable = !jaxbKey.isNotNull();
		}
		if ( jaxbKey.isUpdate() == null ) {
			this.updateable = jaxbManyToOne.isUpdate();
		}
		else {
			this.updateable = jaxbKey.isUpdate();
		}

		this.valueSources = RelationalValueSourceHelper.buildValueSources(
				sourceMappingDocument(),
				null, // todo : collection table name
				new RelationalValueSourceHelper.AbstractColumnsAndFormulasSource() {
					@Override
					public XmlElementMetadata getSourceType() {
						return XmlElementMetadata.KEY;
					}

					@Override
					public String getSourceName() {
						return null;
					}

					@Override
					public String getColumnAttribute() {
						return StringHelper.nullIfEmpty( jaxbKey.getColumnAttribute() );
					}

					@Override
					public List getColumnOrFormulaElements() {
						return jaxbKey.getColumn();
					}

				}
		);
	}

	@Override
	public String getExplicitForeignKeyName() {
		return explicitFkName;
	}

	@Override
	public boolean createForeignKeyConstraint() {
		// HBM has not corollary to JPA's @ForeignKey(NO_CONSTRAINT)
		return true;
	}

	@Override
	public String getReferencedPropertyName() {
		return referencedPropertyName;
	}

	@Override
	public boolean isCascadeDeleteEnabled() {
		return cascadeDeletesAtFkLevel;
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
		return updateable;
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return nullable;
	}
}
