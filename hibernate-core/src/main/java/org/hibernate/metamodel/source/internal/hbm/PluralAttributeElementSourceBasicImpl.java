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
package org.hibernate.metamodel.source.internal.hbm;

import java.util.List;
import java.util.Map;

import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbColumnElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbElementElement;
import org.hibernate.metamodel.source.spi.PluralAttributeElementSourceBasic;
import org.hibernate.metamodel.source.spi.RelationalValueSource;
import org.hibernate.metamodel.source.spi.SizeSource;
import org.hibernate.metamodel.spi.PluralAttributeElementNature;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeElementSourceBasicImpl
		extends AbstractHbmSourceNode
		implements PluralAttributeElementSourceBasic {
	private final HibernateTypeSourceImpl typeSource;
	private final List<RelationalValueSource> valueSources;

	public PluralAttributeElementSourceBasicImpl(
			MappingDocument sourceMappingDocument,
			final JaxbElementElement elementElement) {
		super( sourceMappingDocument );

		final String typeName = extractTypeName( elementElement );
		final Map<String, String> typeParams = elementElement.getType() != null
				? Helper.extractParameters( elementElement.getType().getParam() )
				: java.util.Collections.<String, String>emptyMap();
		this.typeSource = new HibernateTypeSourceImpl( typeName, typeParams );

		this.valueSources = Helper.buildValueSources(
				sourceMappingDocument(),
				new Helper.ValueSourcesAdapter() {
					@Override
					public boolean isIncludedInInsertByDefault() {
						return PluralAttributeElementSourceBasicImpl.this.areValuesIncludedInInsertByDefault();
					}

					@Override
					public boolean isIncludedInUpdateByDefault() {
						return PluralAttributeElementSourceBasicImpl.this.areValuesIncludedInUpdateByDefault();
					}

					@Override
					public String getColumnAttribute() {
						return elementElement.getColumnAttribute();
					}

					@Override
					public String getFormulaAttribute() {
						return elementElement.getFormulaAttribute();
					}

					@Override
					public SizeSource getSizeSource() {
						return Helper.createSizeSourceIfMapped(
								elementElement.getLength(),
								elementElement.getPrecision(),
								elementElement.getScale()
						);
					}

					@Override
					public List<JaxbColumnElement> getColumn() {
						return elementElement.getColumn();
					}

					@Override
					public List<String> getFormula() {
						return elementElement.getFormula();
					}

					@Override
					public boolean isForceNotNull() {
						return elementElement.isNotNull();
					}
				}
		);
	}

	private String extractTypeName(JaxbElementElement elementElement) {
		if ( elementElement.getTypeAttribute() != null ) {
			return elementElement.getTypeAttribute();
		}
		else if ( elementElement.getType() != null ) {
			return elementElement.getType().getName();
		}
		else {
			return null;
		}
	}

	@Override
	public PluralAttributeElementNature getNature() {
		return PluralAttributeElementNature.BASIC;
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
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
}
