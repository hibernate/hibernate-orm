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
package org.hibernate.metamodel.spi.binding;

import org.hibernate.metamodel.spi.relational.Value;

/**
 * Binding of the discriminator in a entity hierarchy
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class EntityDiscriminator {
	private final HibernateTypeDescriptor explicitHibernateTypeDescriptor = new HibernateTypeDescriptor();

	private final Value relationalValue;
	private final boolean inserted;
	private final boolean forced;

	public EntityDiscriminator(Value relationalValue, boolean inserted, boolean forced) {
		this.relationalValue = relationalValue;
		this.inserted = inserted;
		this.forced = forced;
	}

	public Value getRelationalValue() {
		return relationalValue;
	}

	public HibernateTypeDescriptor getExplicitHibernateTypeDescriptor() {
		return explicitHibernateTypeDescriptor;
	}

	public boolean isForced() {
		return forced;
	}

	public boolean isInserted() {
		return inserted;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "EntityDiscriminator" );
		sb.append( "{relationalValue=" ).append( relationalValue );
		sb.append( ", forced=" ).append( forced );
		sb.append( ", inserted=" ).append( inserted );
		sb.append( '}' );
		return sb.toString();
	}
}
