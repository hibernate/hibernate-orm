/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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

import java.util.List;

import org.hibernate.metamodel.spi.PluralAttributeElementNature;
import org.hibernate.metamodel.spi.relational.Value;

/**
 * Describes plural attributes of {@link org.hibernate.metamodel.spi.PluralAttributeElementNature#BASIC} elements
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class PluralAttributeElementBindingBasic extends AbstractPluralAttributeElementBinding {
	private RelationalValueBindingContainer relationalValueBindingContainer;

	public PluralAttributeElementBindingBasic(AbstractPluralAttributeBinding binding) {
		super( binding );
	}

	@Override
	public PluralAttributeElementNature getNature() {
		return PluralAttributeElementNature.BASIC;
	}

	@Override
	public RelationalValueBindingContainer getRelationalValueContainer() {
		return relationalValueBindingContainer;
	}

	public void setRelationalValueBindings(List<RelationalValueBinding> relationalValueBindings) {
		this.relationalValueBindingContainer =  new RelationalValueBindingContainer( relationalValueBindings );
	}


	public List<Value> getValues() {
		return getRelationalValueContainer().values();
	}

	public List<RelationalValueBinding> getRelationalValueBindings() {
		return getRelationalValueContainer().relationalValueBindings();
	}

	public boolean isNullable() {
		return getRelationalValueContainer().hasNullableRelationalValueBinding();
	}

	public boolean hasDerivedValue() {
		return getRelationalValueContainer().hasDerivedValue();
	}

	public boolean hasNonNullableValue() {
		return getRelationalValueContainer().hasNonNullableRelationalValueBinding();
	}
}
