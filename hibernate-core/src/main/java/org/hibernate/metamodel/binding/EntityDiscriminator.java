/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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

import org.hibernate.metamodel.binding.state.DiscriminatorBindingState;
import org.hibernate.metamodel.relational.state.ValueRelationalState;

/**
 * Binding of the discriminator in a entity hierarchy
 *
 * @author Steve Ebersole
 */
public class EntityDiscriminator {
	private SimpleAttributeBinding valueBinding;
	private boolean forced;
	private boolean inserted = true;

	public EntityDiscriminator() {
	}

	public SimpleAttributeBinding getValueBinding() {
		return valueBinding;
	}

	/* package-protected */
	void setValueBinding(SimpleAttributeBinding valueBinding) {
		this.valueBinding = valueBinding;
	}

	public EntityDiscriminator initialize(DiscriminatorBindingState state) {
		if ( valueBinding == null ) {
			throw new IllegalStateException( "Cannot bind state because the value binding has not been initialized." );
		}
		this.valueBinding.initialize( state );
		this.forced = state.isForced();
		this.inserted = state.isInsertable();
		return this;
	}

	public EntityDiscriminator initialize(ValueRelationalState state) {
		valueBinding.initialize( state );
		return this;
	}

	public boolean isForced() {
		return forced;
	}

	public void setForced(boolean forced) {
		this.forced = forced;
	}

	public boolean isInserted() {
		return inserted;
	}

	public void setInserted(boolean inserted) {
		this.inserted = inserted;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "EntityDiscriminator" );
		sb.append( "{valueBinding=" ).append( valueBinding );
		sb.append( ", forced=" ).append( forced );
		sb.append( ", inserted=" ).append( inserted );
		sb.append( '}' );
		return sb.toString();
	}
}
