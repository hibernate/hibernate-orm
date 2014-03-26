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
package org.hibernate.metamodel.spi.binding;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.spi.NaturalIdMutability;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.relational.Column;

/**
 * @author Gail Badner
 */
public class BackRefAttributeBinding extends BasicAttributeBinding {

	private final PluralAttributeBinding pluralAttributeBinding;
	private final boolean isIndexBackRef;

	BackRefAttributeBinding(
			EntityBinding entityBinding,
			SingularAttribute attribute,
			PluralAttributeBinding pluralAttributeBinding,
			boolean isIndexBackRef) {
		super(
				entityBinding,
				attribute,
				createRelationalValueBindings( pluralAttributeBinding, isIndexBackRef ),
				null,
				false,
				false,
				NaturalIdMutability.NOT_NATURAL_ID,
				null,
				pluralAttributeBinding.getAttributeRole().append( "backRef" ),
				pluralAttributeBinding.getAttributePath().append( "backRef" ),
				PropertyGeneration.NEVER
		);
		this.pluralAttributeBinding = pluralAttributeBinding;
		this.isIndexBackRef = isIndexBackRef;
	}

	private static List<RelationalValueBinding> createRelationalValueBindings(
			PluralAttributeBinding pluralAttributeBinding,
			boolean isIndexBackRef) {
		List<RelationalValueBinding> relationalValueBindings;
		if ( isIndexBackRef ) {
			PluralAttributeIndexBinding indexBinding =
					( (IndexedPluralAttributeBinding) pluralAttributeBinding).getPluralAttributeIndexBinding();
			relationalValueBindings = indexBinding.getRelationalValueBindings();

		}
		else {
			relationalValueBindings = new ArrayList<RelationalValueBinding>( );
			for ( RelationalValueBinding keyRelationalValueBinding : pluralAttributeBinding.getPluralAttributeKeyBinding().getRelationalValueBindings() ) {
				Column keyColumn = (Column) keyRelationalValueBinding.getValue();
				relationalValueBindings.add( new RelationalValueBinding( keyRelationalValueBinding.getTable(), keyColumn, true, false ) );
			}
		}
		return relationalValueBindings;
	}

	public String getCollectionRole() {
		return pluralAttributeBinding.getAttribute().getRole();
	}

	public String getEntityName() {
		return pluralAttributeBinding.getContainer().seekEntityBinding().getEntityName();
	}

	@Override
	public boolean isAssociation() {
		return false;
	}

	@Override
	public boolean isBackRef() {
		return true;
	}

	public boolean isIndexBackRef() {
		return isIndexBackRef;
	}

	@Override
	public boolean isIncludedInUpdate() {
		//TODO: should be able to rely on super method, but that seems broken currently.
		return false;
	}
}
