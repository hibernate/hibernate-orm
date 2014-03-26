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

import org.hibernate.metamodel.internal.binder.Binder;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttributeIndexDetailsMapKeyEmbedded;
import org.hibernate.metamodel.source.spi.EmbeddableSource;
import org.hibernate.metamodel.source.spi.PluralAttributeMapKeySourceEmbedded;
import org.hibernate.metamodel.source.spi.RelationalValueSource;
import org.hibernate.metamodel.spi.PluralAttributeIndexNature;

/**
 * @author Gail Badner
 */
public class PluralAttributeMapKeySourceEmbeddedImpl
		extends AbstractPluralAttributeIndexSourceImpl
		implements PluralAttributeMapKeySourceEmbedded {

	private final EmbeddableSourceImpl embeddableSource;
	private final Binder.DefaultNamingStrategy defaultNamingStrategy;

	public PluralAttributeMapKeySourceEmbeddedImpl(
			PluralAttribute attribute,
			PluralAttributeIndexDetailsMapKeyEmbedded mapKeyDetails) {
		super( attribute );

		this.embeddableSource = new EmbeddableSourceImpl(
				mapKeyDetails.getEmbeddableTypeMetadata(),
				SourceHelper.IdentifierPathAttributeBuilder.INSTANCE
		);
		this.defaultNamingStrategy = new PluralAttributeMapSourceImpl.MapKeyColumnDefaultNaming( attribute );
	}

	@Override
	public PluralAttributeIndexNature getNature() {
		return PluralAttributeIndexNature.AGGREGATE;
	}

	@Override
	public EmbeddableSource getEmbeddableSource() {
		return embeddableSource;
	}

	@Override
	public List<Binder.DefaultNamingStrategy> getDefaultNamingStrategies() {
		return Collections.singletonList( defaultNamingStrategy );
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		return null;
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

	public JavaTypeDescriptor getTypeDescriptor() {
		return embeddableSource.getTypeDescriptor();
	}
}
