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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.spi.relational.SimpleValue;
import org.hibernate.metamodel.spi.relational.Tuple;
import org.hibernate.metamodel.spi.relational.Value;

/**
 * Describes plural attributes of {@link PluralAttributeElementNature#BASIC} elements
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class BasicPluralAttributeElementBinding extends AbstractPluralAttributeElementBinding {
	private Value value;
	private List<SimpleValueBinding> simpleValueBindings = new ArrayList<SimpleValueBinding>();

	private boolean hasDerivedValue;
	private boolean isNullable = true;

	public BasicPluralAttributeElementBinding(AbstractPluralAttributeBinding binding) {
		super( binding );
	}
	
	@Override
	public PluralAttributeElementNature getPluralAttributeElementNature() {
		return PluralAttributeElementNature.BASIC;
	}

	public void setSimpleValueBindings(Iterable<SimpleValueBinding> simpleValueBindings) {
		List<SimpleValue> values = new ArrayList<SimpleValue>();
		for ( SimpleValueBinding simpleValueBinding : simpleValueBindings ) {
			this.simpleValueBindings.add( simpleValueBinding );
			values.add( simpleValueBinding.getSimpleValue() );
			this.hasDerivedValue = this.hasDerivedValue || simpleValueBinding.isDerived();
			this.isNullable = this.isNullable && simpleValueBinding.isNullable();
		}
		if ( values.size() == 1 ) {
			this.value = values.get( 0 );
		}
		else {
			final Tuple tuple = values.get( 0 ).getTable().createTuple( getPluralAttributeBinding().getRole() );
			for ( SimpleValue value : values ) {
				tuple.addValue( value );
			}
			this.value = tuple;
		}
	}
}
