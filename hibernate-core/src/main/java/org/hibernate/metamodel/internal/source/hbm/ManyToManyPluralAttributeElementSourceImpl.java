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
package org.hibernate.metamodel.internal.source.hbm;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbManyToManyElement;
import org.hibernate.internal.jaxb.mapping.hbm.PluralAttributeElement;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.spi.source.ManyToManyPluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.PluralAttributeElementNature;
import org.hibernate.metamodel.spi.source.RelationalValueSource;

/**
 * @author Steve Ebersole
 */
public class ManyToManyPluralAttributeElementSourceImpl
		extends AbstractHbmSourceNode
		implements ManyToManyPluralAttributeElementSource {
	private final PluralAttributeElement pluralAttributeElement;
	private final JaxbManyToManyElement manyToManyElement;

	private final List<RelationalValueSource> valueSources;

	public ManyToManyPluralAttributeElementSourceImpl(
			MappingDocument mappingDocument,
			final PluralAttributeElement pluralAttributeElement,
			final JaxbManyToManyElement manyToManyElement) {
		super( mappingDocument );
		this.pluralAttributeElement = pluralAttributeElement;
		this.manyToManyElement = manyToManyElement;

		this.valueSources = Helper.buildValueSources(
				sourceMappingDocument(),
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
				}
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
				: bindingContext().qualifyClassName( manyToManyElement.getClazz() );
	}

	@Override
	public String getReferencedEntityAttributeName() {
		return manyToManyElement.getPropertyRef();
	}

	@Override
	// used by JPA instead of referenced entity attribute
	public Collection<String> getReferencedColumnNames() {
		return Collections.emptyList();
	}

	@Override
	public List<RelationalValueSource> getValueSources() {
		return valueSources;
	}

	@Override
	public boolean isNotFoundAnException() {
		return manyToManyElement.getNotFound() == null || !"ignore".equals( manyToManyElement.getNotFound().value() );
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
	public Iterable<CascadeStyle> getCascadeStyles() {
		return Helper.interpretCascadeStyles( pluralAttributeElement.getCascade(), bindingContext() );
	}

	@Override
	public boolean fetchImmediately() {
		if ( manyToManyElement.getLazy() != null ) {
			if ( "false".equals( manyToManyElement.getLazy().value() ) ) {
				return true;
			}
		}

		if ( manyToManyElement.getOuterJoin() == null ) {
			return !bindingContext().getMappingDefaults().areAssociationsLazy();
		}
		else {
			final String value = manyToManyElement.getOuterJoin().value();
			if ( "auto".equals( value ) ) {
			}
			return "true".equals( value );
		}
	}
}
