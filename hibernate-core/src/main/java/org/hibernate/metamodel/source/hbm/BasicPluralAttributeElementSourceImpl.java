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
package org.hibernate.metamodel.source.hbm;

import java.util.List;
import java.util.Map;

import org.hibernate.internal.jaxb.mapping.hbm.JaxbElementElement;
import org.hibernate.metamodel.source.LocalBindingContext;
import org.hibernate.metamodel.source.binder.BasicPluralAttributeElementSource;
import org.hibernate.metamodel.source.binder.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.source.binder.PluralAttributeElementNature;
import org.hibernate.metamodel.source.binder.RelationalValueSource;

/**
 * @author Steve Ebersole
 */
public class BasicPluralAttributeElementSourceImpl implements BasicPluralAttributeElementSource {
	private final List<RelationalValueSource> valueSources;
	private final ExplicitHibernateTypeSource typeSource;

	public BasicPluralAttributeElementSourceImpl(
			final JaxbElementElement elementElement,
			LocalBindingContext bindingContext) {
		this.valueSources = Helper.buildValueSources(
				new Helper.ValueSourcesAdapter() {
					@Override
					public String getContainingTableName() {
						return null;
					}

					@Override
					public boolean isIncludedInInsertByDefault() {
						return true;
					}

					@Override
					public boolean isIncludedInUpdateByDefault() {
						return true;
					}

					@Override
					public String getColumnAttribute() {
						return elementElement.getColumn();
					}

					@Override
					public String getFormulaAttribute() {
						return elementElement.getFormula();
					}

					@Override
					public List getColumnOrFormulaElements() {
						return elementElement.getColumnOrFormula();
					}
				},
				bindingContext
		);

		this.typeSource = new ExplicitHibernateTypeSource() {
			@Override
			public String getName() {
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
			public Map<String, String> getParameters() {
				return elementElement.getType() != null
						? Helper.extractParameters( elementElement.getType().getParam() )
						: java.util.Collections.<String, String>emptyMap();
			}
		};
	}

	@Override
	public PluralAttributeElementNature getNature() {
		return PluralAttributeElementNature.BASIC;
	}

	@Override
	public List<RelationalValueSource> getValueSources() {
		return valueSources;
	}

	@Override
	public ExplicitHibernateTypeSource getExplicitHibernateTypeSource() {
		return typeSource;
	}
}
