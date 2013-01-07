/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.annotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.metamodel.internal.source.annotations.attribute.Column;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.PluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.source.BasicPluralAttributeIndexSource;
import org.hibernate.metamodel.spi.source.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class BasicPluralAttributeIndexSourceImpl implements BasicPluralAttributeIndexSource {
	private final PluralAssociationAttribute attribute;
	private final IndexedPluralAttributeSourceImpl indexedPluralAttributeSource;
	private final List<RelationalValueSource> relationalValueSources =  new ArrayList<RelationalValueSource>( 1 );
	public BasicPluralAttributeIndexSourceImpl(IndexedPluralAttributeSourceImpl indexedPluralAttributeSource, PluralAssociationAttribute attribute) {
		this.attribute = attribute;
		this.indexedPluralAttributeSource = indexedPluralAttributeSource;
		AnnotationInstance columnAnnotation = JandexHelper.getSingleAnnotation(
				attribute.annotations(),
				HibernateDotNames.INDEX_COLUMN
		);
		if(columnAnnotation == null){
			columnAnnotation   = JandexHelper.getSingleAnnotation(
					attribute.annotations(),
					JPADotNames.ORDER_COLUMN
			);
		}
		Column indexColumn = new Column( columnAnnotation );
		relationalValueSources.add( new ColumnValuesSourceImpl( indexColumn ) );

	}

	@Override
	public PluralAttributeIndexBinding.Nature getNature() {
		return PluralAttributeIndexBinding.Nature.BASIC;
	}

	@Override
	public ExplicitHibernateTypeSource explicitHibernateTypeSource() {
		return new ExplicitHibernateTypeSource() {
			@Override
			public String getName() {
				return "integer";
			}

			@Override
			public Map<String, String> getParameters() {
				return null;
			}
		};
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		return relationalValueSources;
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return false;
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return false;
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return false;
	}
}
