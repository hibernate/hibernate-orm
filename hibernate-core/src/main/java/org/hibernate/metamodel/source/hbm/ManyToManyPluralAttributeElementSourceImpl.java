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

import org.hibernate.FetchMode;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbManyToManyElement;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.LocalBindingContext;
import org.hibernate.metamodel.source.binder.ManyToManyPluralAttributeElementSource;
import org.hibernate.metamodel.source.binder.PluralAttributeElementNature;
import org.hibernate.metamodel.source.binder.RelationalValueSource;

/**
 * @author Steve Ebersole
 */
public class ManyToManyPluralAttributeElementSourceImpl implements ManyToManyPluralAttributeElementSource {
	private final JaxbManyToManyElement manyToManyElement;
	private final LocalBindingContext bindingContext;

	private final List<RelationalValueSource> valueSources;

	public ManyToManyPluralAttributeElementSourceImpl(
			final JaxbManyToManyElement manyToManyElement,
			final LocalBindingContext bindingContext) {
		this.manyToManyElement = manyToManyElement;
		this.bindingContext = bindingContext;

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
						return manyToManyElement.getColumn();
					}

					@Override
					public String getFormulaAttribute() {
						return manyToManyElement.getFormula();
					}

					@Override
					public List getColumnOrFormulaElements() {
						return manyToManyElement.getColumnOrFormula();
					}
				},
				bindingContext
		);
	}

	@Override
	public PluralAttributeElementNature getNature() {
		return PluralAttributeElementNature.MANY_TO_MANY;
	}

	@Override
	public String getReferencedEntityName() {
		return StringHelper.isNotEmpty( manyToManyElement.getEntityName() )
				? manyToManyElement.getEntityName()
				: bindingContext.qualifyClassName( manyToManyElement.getClazz() );
	}

	@Override
	public String getReferencedEntityAttributeName() {
		return manyToManyElement.getPropertyRef();
	}

	@Override
	public List<RelationalValueSource> getValueSources() {
		return valueSources;
	}

	@Override
	public boolean isNotFoundAnException() {
		return manyToManyElement.getNotFound() == null
				|| ! "ignore".equals( manyToManyElement.getNotFound().value() );
	}

	@Override
	public String getExplicitForeignKeyName() {
		return manyToManyElement.getForeignKey();
	}

	@Override
	public boolean isUnique() {
		return manyToManyElement.isUnique();
	}

	@Override
	public String getOrderBy() {
		return manyToManyElement.getOrderBy();
	}

	@Override
	public String getWhere() {
		return manyToManyElement.getWhere();
	}

	@Override
	public FetchMode getFetchMode() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean fetchImmediately() {
		if ( manyToManyElement.getLazy() != null ) {
			if ( "false".equals( manyToManyElement.getLazy().value() ) ) {
				return true;
			}
		}

		if ( manyToManyElement.getOuterJoin() == null ) {
			return ! bindingContext.getMappingDefaults().areAssociationsLazy();
		}
		else {
			final String value = manyToManyElement.getOuterJoin().value();
			if ( "auto".equals( value ) ) {
				return ! bindingContext.getMappingDefaults().areAssociationsLazy();
			}
			return "true".equals( value );
		}
	}
}
