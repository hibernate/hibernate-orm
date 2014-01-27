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

import java.util.List;

import org.hibernate.metamodel.internal.Binder;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.spi.binding.PluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.source.EntityAttributePluralAttributeIndexSource;
import org.hibernate.metamodel.spi.source.PluralAttributeIndexSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;

/**
 * @author Gail Badner
 */
public class MapKeyPluralAttributeIndexSourceImpl extends AbstractPluralAttributeIndexSourceImpl implements EntityAttributePluralAttributeIndexSource {
	private final PluralAttributeIndexSource pluralAttributeIndexSource;
	private final String attributeName;

	public MapKeyPluralAttributeIndexSourceImpl(
			PluralAssociationAttribute attribute,
			PluralAttributeIndexSource pluralAttributeIndexSource,
			String attributeName) {
		super( attribute );
		this.pluralAttributeIndexSource = pluralAttributeIndexSource;
		this.attributeName = attributeName;
	}

	@Override
	public String getAttributeName() {
		return attributeName;
	}

	@Override
	public PluralAttributeIndexBinding.Nature getNature() {
		return pluralAttributeIndexSource.getNature();
	}

	@Override
	public List<Binder.DefaultNamingStrategy> getDefaultNamingStrategies() {
		return pluralAttributeIndexSource.getDefaultNamingStrategies();
	}

	@Override
	public boolean isReferencedEntityAttribute() {
		return true;
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		return pluralAttributeIndexSource.relationalValueSources();
	}
}
