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

import java.util.HashSet;
import java.util.Set;

import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.util.EnumConversionHelper;
import org.hibernate.metamodel.spi.source.AssociationPluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.MappedByAssociationSource;

/**
 + * @author Gail Badner
 + */
public abstract class AbstractPluralAssociationElementSourceImpl
		extends AbstractPluralAttributeElementSourceImpl implements AssociationPluralAttributeElementSource {
	private final PluralAttributeSourceImpl pluralAttributeSource;
	private final Set<MappedByAssociationSource> ownedAssociationSources = new HashSet<MappedByAssociationSource>(  );

	public AbstractPluralAssociationElementSourceImpl(
			final PluralAttributeSourceImpl pluralAttributeSource,
			final String relativePath) {
		super( pluralAttributeSource.pluralAssociationAttribute(), relativePath );
		this.pluralAttributeSource = pluralAttributeSource;
	}

	@Override
	public String getReferencedEntityName() {
		return pluralAssociationAttribute().getReferencedEntityType();
	}

	@Override
	public boolean isNotFoundAnException() {
		return !pluralAssociationAttribute().isIgnoreNotFound();
	}

	public AttributeSource getAttributeSource() {
		return pluralAttributeSource;
	}

	@Override
	public Set<MappedByAssociationSource> getOwnedAssociationSources() {
		return ownedAssociationSources;
	}

	@Override
	public void addMappedByAssociationSource(MappedByAssociationSource attributeSource) {
		if ( attributeSource == null ) {
			throw new IllegalArgumentException( "attributeSource must be non-null." );
		}
		ownedAssociationSources.add( attributeSource );
	}

	@Override
	public boolean isMappedBy() {
		return false;
	}

	@Override
	public Set<CascadeStyle> getCascadeStyles() {
		final Set<CascadeStyle> cascadeStyles =  EnumConversionHelper.cascadeTypeToCascadeStyleSet(
				pluralAssociationAttribute().getCascadeTypes(),
				pluralAssociationAttribute().getHibernateCascadeTypes(),
				pluralAssociationAttribute().getContext()
		);
		if ( getNature() == Nature.ONE_TO_MANY && pluralAssociationAttribute().isOrphanRemoval() ) {
			cascadeStyles.add( CascadeStyles.DELETE_ORPHAN );
		}
		return cascadeStyles;
	}

	protected PluralAssociationAttribute pluralAssociationAttribute() {
		return pluralAttributeSource.pluralAssociationAttribute();
	}
}
