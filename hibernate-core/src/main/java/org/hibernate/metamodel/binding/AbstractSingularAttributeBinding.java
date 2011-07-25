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
package org.hibernate.metamodel.binding;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.domain.SingularAttribute;
import org.hibernate.metamodel.relational.SimpleValue;
import org.hibernate.metamodel.relational.Tuple;
import org.hibernate.metamodel.relational.Value;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSingularAttributeBinding
		extends AbstractAttributeBinding
		implements SingularAttributeBinding {

	private Value value;
	private List<SimpleValueBinding> simpleValueBindings = new ArrayList<SimpleValueBinding>();

	private boolean hasDerivedValue;
	private boolean isNullable = true;

	protected AbstractSingularAttributeBinding(AttributeBindingContainer container, SingularAttribute attribute) {
		super( container, attribute );
	}

	@Override
	public SingularAttribute getAttribute() {
		return (SingularAttribute) super.getAttribute();
	}

	public Value getValue() {
		return value;
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
			final Tuple tuple = values.get( 0 ).getTable().createTuple( getRole() );
			for ( SimpleValue value : values ) {
				tuple.addValue( value );
			}
			this.value = tuple;
		}
	}

	private String getRole() {
		return getContainer().getPathBase() + '.' + getAttribute().getName();
	}

	@Override
	public int getSimpleValueSpan() {
		checkValueBinding();
		return simpleValueBindings.size();
	}

	protected void checkValueBinding() {
		if ( value == null ) {
			throw new AssertionFailure( "No values yet bound!" );
		}
	}

	@Override
	public Iterable<SimpleValueBinding> getSimpleValueBindings() {
		return simpleValueBindings;
	}

	@Override
	public boolean hasDerivedValue() {
		checkValueBinding();
		return hasDerivedValue;
	}

	@Override
	public boolean isNullable() {
		checkValueBinding();
		return isNullable;
	}
}
