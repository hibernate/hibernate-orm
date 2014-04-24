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
package org.hibernate.metamodel.source.internal.annotations;

import java.util.Collections;
import java.util.List;

import org.hibernate.cfg.NamingStrategy;
import org.hibernate.metamodel.internal.binder.Binder;
import org.hibernate.metamodel.source.internal.annotations.attribute.Column;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttribute;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.spi.PluralAttributeSequentialIndexSource;
import org.hibernate.metamodel.source.spi.RelationalValueSource;
import org.hibernate.metamodel.spi.PluralAttributeIndexNature;

import org.jboss.jandex.AnnotationInstance;

/**
 * @author Gail Badner
 */
public class PluralAttributeSequentialIndexSourceImpl
		extends AbstractPluralAttributeIndexSourceImpl
		implements PluralAttributeSequentialIndexSource {

	private final int base;
	private final RelationalValueSource relationalValueSource;
	private final Binder.DefaultNamingStrategy defaultNamingStrategy;

	private static HibernateTypeSourceImpl INTEGER_TYPE;

	public PluralAttributeSequentialIndexSourceImpl(final PluralAttribute attribute) {
		super( attribute );
		final AnnotationInstance columnAnnotation = attribute.getBackingMember().getAnnotations()
				.get( JPADotNames.ORDER_COLUMN );
		this.base = columnAnnotation.value( "base" ) != null
				? columnAnnotation.value( "base" ).asInt()
				: 0;
		this.relationalValueSource = new ColumnSourceImpl( new Column( columnAnnotation ) );
		this.defaultNamingStrategy = new Binder.DefaultNamingStrategy() {
			@Override
			public String defaultName(NamingStrategy namingStrategy) {
				return attribute.getName() + "_ORDER";
			}
		};

		if ( INTEGER_TYPE == null ) {
			INTEGER_TYPE = new HibernateTypeSourceImpl(
					"integer",
					null,
					attribute.getContext().typeDescriptor( "int" )
			);
		}
	}

	@Override
	public int base() {
		return base;
	}

	@Override
	public PluralAttributeIndexNature getNature() {
		return PluralAttributeIndexNature.SEQUENTIAL;
	}

	@Override
	public List<Binder.DefaultNamingStrategy> getDefaultNamingStrategies() {
		return Collections.singletonList( defaultNamingStrategy );
	}

	@Override
	public HibernateTypeSourceImpl getTypeInformation() {
		return INTEGER_TYPE;
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		return Collections.singletonList( relationalValueSource );
	}
}
