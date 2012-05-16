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
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.relational.Column;

/**
 * @author Gail Badner
 */
public class BackRefAttributeBinding
		extends BasicAttributeBinding
		implements SingularNonAssociationAttributeBinding {

	PluralAttributeBinding pluralAttributeBinding;

	BackRefAttributeBinding(
			EntityBinding entityBinding,
			SingularAttribute attribute,
			PluralAttributeBinding pluralAttributeBinding) {
		super(
				entityBinding,
				attribute,
				createRelationalValueBindings( pluralAttributeBinding ),
				null,
				false,
				false,
				null,
				PropertyGeneration.NEVER
		);
		this.pluralAttributeBinding = pluralAttributeBinding;
	}

	private static List<RelationalValueBinding> createRelationalValueBindings(PluralAttributeBinding pluralAttributeBinding) {
		List<RelationalValueBinding> relationalValueBindings = new ArrayList<RelationalValueBinding>( );
		for ( Column column : pluralAttributeBinding.getPluralAttributeKeyBinding().getForeignKey().getSourceColumns() ) {
			relationalValueBindings.add( new RelationalValueBinding( column, true, false ) );
		}
		return relationalValueBindings;
	}

	public String getCollectionRole() {
		return pluralAttributeBinding.getAttribute().getRole();
	}

	public String getEntityName() {
		return pluralAttributeBinding.getContainer().seekEntityBinding().getEntity().getName();
	}

	@Override
	public boolean isAssociation() {
		return false;
	}

	@Override
	public boolean isBackRef() {
		return true;
	}

	@Override
	public boolean hasDerivedValue() {
		return false;
	}

	@Override
	public boolean isNullable() {
		return false;
	}
}
