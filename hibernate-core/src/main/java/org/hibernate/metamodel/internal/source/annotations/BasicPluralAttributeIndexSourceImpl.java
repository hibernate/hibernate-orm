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

import java.util.Collections;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.metamodel.internal.Binder;
import org.hibernate.metamodel.internal.source.annotations.attribute.Column;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.PluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.source.BasicPluralAttributeIndexSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class BasicPluralAttributeIndexSourceImpl extends AbstractPluralAttributeIndexSourceImpl implements BasicPluralAttributeIndexSource {
	private final IndexedPluralAttributeSourceImpl indexedPluralAttributeSource;
	private final List<RelationalValueSource> relationalValueSources;
	private final Binder.DefaultNamingStrategy defaultNamingStrategy;
	public BasicPluralAttributeIndexSourceImpl(
			IndexedPluralAttributeSourceImpl indexedPluralAttributeSource,
			PluralAssociationAttribute attribute,
			Binder.DefaultNamingStrategy defaultNamingStrategy) {
		this( indexedPluralAttributeSource, attribute, defaultNamingStrategy, createRelationalValueSources( attribute ) );
	}

	public BasicPluralAttributeIndexSourceImpl(
			IndexedPluralAttributeSourceImpl indexedPluralAttributeSource,
			PluralAssociationAttribute attribute,
			Binder.DefaultNamingStrategy defaultNamingStrategy,
			List<RelationalValueSource> relationalValueSources) {
		super( attribute );
		this.indexedPluralAttributeSource = indexedPluralAttributeSource;
		this.relationalValueSources = relationalValueSources;
		this.defaultNamingStrategy = defaultNamingStrategy;
	}

	private static List<RelationalValueSource> createRelationalValueSources(PluralAssociationAttribute attribute) {
		AnnotationInstance columnAnnotation = JandexHelper.getSingleAnnotation(
				attribute.annotations(),
				HibernateDotNames.INDEX_COLUMN
		);
		if ( columnAnnotation == null ) {
			columnAnnotation = JandexHelper.getSingleAnnotation(
					attribute.annotations(),
					JPADotNames.ORDER_COLUMN
			);
		}
		if ( columnAnnotation == null ) {
			columnAnnotation = JandexHelper.getSingleAnnotation(
					attribute.annotations(),
					JPADotNames.MAP_KEY_COLUMN
			);
		}
		Column indexColumn = new Column( columnAnnotation );
		return Collections.singletonList( (RelationalValueSource) new ColumnSourceImpl( indexColumn ) );
	}
	@Override
	public PluralAttributeIndexBinding.Nature getNature() {
		return PluralAttributeIndexBinding.Nature.BASIC;
	}

	@Override
	public List<Binder.DefaultNamingStrategy> getDefaultNamingStrategies() {
		return Collections.singletonList( defaultNamingStrategy );
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		return relationalValueSources;
	}

}
