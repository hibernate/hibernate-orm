/**
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
package org.hibernate.metamodel.internal.source.hbm;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.metamodel.spi.source.AssociationSource;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.MappedByAssociationSource;
import org.hibernate.metamodel.spi.source.PluralAttributeSource;

/**
 + * @author Gail Badner
 + */
public abstract class AbstractPluralAssociationElementSourceImpl
		extends AbstractHbmSourceNode implements AssociationSource {

	private final PluralAttributeSource pluralAttributeSource;
	private final Set<MappedByAssociationSource> ownedAssociationSources = new HashSet<MappedByAssociationSource>(  );

	public AbstractPluralAssociationElementSourceImpl(
			MappingDocument mappingDocument,
			PluralAttributeSource pluralAttributeSource) {
		super( mappingDocument );
		this.pluralAttributeSource = pluralAttributeSource;
	}

	@Override
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
}
