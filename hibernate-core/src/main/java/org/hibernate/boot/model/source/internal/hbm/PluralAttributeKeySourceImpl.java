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

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmKeyType;
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
