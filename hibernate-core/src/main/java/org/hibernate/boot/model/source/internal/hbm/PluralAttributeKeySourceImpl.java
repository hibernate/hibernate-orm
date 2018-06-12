/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
			final JaxbHbmManyToOneType jaxbKey,
			final AttributeSourceContainer container) {
		super( mappingDocument );

		this.explicitFkName = StringHelper.nullIfEmpty( jaxbKey.getForeignKey() );
		this.referencedPropertyName = StringHelper.nullIfEmpty( jaxbKey.getPropertyRef() );
		this.cascadeDeletesAtFkLevel = jaxbKey.getOnDelete() != null
				&& "cascade".equals( jaxbKey.getOnDelete().value() );
		this.nullable = jaxbKey.isNotNull() == null || !jaxbKey.isNotNull();
		this.updateable = jaxbKey.isUpdate();

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
						return jaxbKey.getColumnOrFormula();
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
