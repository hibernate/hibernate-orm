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
package org.hibernate.metamodel.source.internal.annotations;

import java.util.Collections;
import java.util.List;

import org.hibernate.metamodel.internal.binder.Binder;
import org.hibernate.metamodel.source.internal.annotations.attribute.Column;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttributeIndexDetailsMapKeyBasic;
import org.hibernate.metamodel.source.spi.PluralAttributeMapKeySourceBasic;
import org.hibernate.metamodel.source.spi.RelationalValueSource;
import org.hibernate.metamodel.spi.PluralAttributeIndexNature;

/**
 * @author Steve Ebersole
 * @author Strong Liu
 */
public class PluralAttributeMapKeySourceBasicImpl
		extends AbstractPluralAttributeIndexSourceImpl
		implements PluralAttributeMapKeySourceBasic {
	private final List<RelationalValueSource> relationalValueSources;
	private final Binder.DefaultNamingStrategy defaultNamingStrategy;

	public PluralAttributeMapKeySourceBasicImpl(
			PluralAttribute attribute,
			PluralAttributeIndexDetailsMapKeyBasic mapKeyDetails) {
		super( attribute );
		this.defaultNamingStrategy = new PluralAttributeMapSourceImpl.MapKeyColumnDefaultNaming( attribute );
		this.relationalValueSources = Collections.singletonList(
				(RelationalValueSource) new ColumnSourceImpl( new Column( mapKeyDetails.getMapKeyColumnAnnotation() ) )
		);
	}

	@Override
	public PluralAttributeIndexNature getNature() {
		return PluralAttributeIndexNature.BASIC;
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
