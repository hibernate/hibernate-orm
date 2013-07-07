/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.metamodel.internal.Binder;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.source.HibernateTypeSource;
import org.hibernate.metamodel.spi.source.SequentialPluralAttributeIndexSource;

/**
 * @author Gail Badner
 */
public class SequentialPluralAttributeIndexSourceImpl
		extends BasicPluralAttributeIndexSourceImpl
		implements SequentialPluralAttributeIndexSource {
	private final int base;

	private final static HibernateTypeSource INTERGER_TYPE = new HibernateTypeSource() {
		@Override
		public String getName() {
			return "integer";
		}

		@Override
		public Map<String, String> getParameters() {
			return Collections.emptyMap();
		}
		@Override
		public Class getJavaType() {
			return null;
		}
	};

	public SequentialPluralAttributeIndexSourceImpl(
			IndexedPluralAttributeSourceImpl indexedPluralAttributeSource,
			PluralAssociationAttribute attribute,
			Binder.DefaultNamingStrategy defaultNamingStrategy) {
		super( indexedPluralAttributeSource, attribute, defaultNamingStrategy );
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
		this.base = columnAnnotation.value( "base" ) != null ? columnAnnotation.value( "base" )
				.asInt() : 0;
	}

	@Override
	public int base() {
		return base;
	}
	@Override
	public HibernateTypeSource getTypeInformation() {
		return INTERGER_TYPE;
	}
}
