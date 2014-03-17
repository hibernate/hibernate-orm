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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.metamodel.internal.binder.Binder;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttribute;
import org.hibernate.metamodel.source.spi.AttributeSource;
import org.hibernate.metamodel.source.spi.CompositePluralAttributeIndexSource;
import org.hibernate.metamodel.source.spi.RelationalValueSource;
import org.hibernate.metamodel.spi.LocalBindingContext;
import org.hibernate.metamodel.spi.binding.PluralAttributeIndexBinding;

/**
 * @author Gail Badner
 */
public class CompositePluralAttributeIndexSourceImpl extends AbstractPluralAttributeIndexSourceImpl implements CompositePluralAttributeIndexSource {
	private final List<RelationalValueSource> relationalValueSources =  new ArrayList<RelationalValueSource>( 1 );
	private final Binder.DefaultNamingStrategy defaultNamingStrategy;
	private final List<AttributeSource> attributeSources;

	public CompositePluralAttributeIndexSourceImpl(
			PluralAttribute attribute,
			List<AttributeSource> attributeSources,
			Binder.DefaultNamingStrategy defaultNamingStrategy) {
		super( attribute );
		this.attributeSources = attributeSources;
		this.defaultNamingStrategy = defaultNamingStrategy;
	}

	@Override
	public PluralAttributeIndexBinding.Nature getNature() {
		return PluralAttributeIndexBinding.Nature.AGGREGATE;
	}

	@Override
	public List<Binder.DefaultNamingStrategy> getDefaultNamingStrategies() {
		return Collections.singletonList( defaultNamingStrategy );
	}

	@Override
	public boolean isReferencedEntityAttribute() {
		return false;
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

	public JavaTypeDescriptor getTypeDescriptor() {
		return pluralAssociationAttribute().getIndexDetails().getJavaType();
	}

	@Override
	public String getPath() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public List<AttributeSource> attributeSources() {
		return attributeSources;
	}

	@Override
	public LocalBindingContext getLocalBindingContext() {
		return pluralAssociationAttribute().getContext();
	}
}
