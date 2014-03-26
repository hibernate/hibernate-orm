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

import java.util.HashSet;
import java.util.Set;

import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttribute;
import org.hibernate.metamodel.source.spi.AttributeSource;
import org.hibernate.metamodel.source.spi.MappedByAssociationSource;
import org.hibernate.metamodel.source.spi.PluralAttributeElementSourceAssociation;

/**
 * @author Gail Badner
 */
public abstract class AbstractPluralElementSourceAssociationImpl
		extends AbstractPluralAttributeElementSourceImpl
		implements PluralAttributeElementSourceAssociation {

	private final Set<MappedByAssociationSource> ownedAssociationSources = new HashSet<MappedByAssociationSource>(  );

	public AbstractPluralElementSourceAssociationImpl(PluralAttributeSourceImpl pluralAttributeSource) {
		super( pluralAttributeSource );
	}

	@Override
	public String getReferencedEntityName() {
		return pluralAssociationAttribute().getElementDetails().getJavaType().getName().toString();
	}

	@Override
	public boolean isIgnoreNotFound() {
		return pluralAssociationAttribute().isIgnoreNotFound();
	}

	public AttributeSource getAttributeSource() {
		return getPluralAttributeSource();
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
		return getPluralAttributeSource().getUnifiedCascadeStyles();
	}

	protected PluralAttribute pluralAssociationAttribute() {
		return getPluralAttribute();
	}
}
