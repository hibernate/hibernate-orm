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

	private Value relationalValue;
	private boolean inserted;
	private boolean forced;

	public EntityDiscriminator() {
	}

	public Value getRelationalValue() {
		return relationalValue;
	}

	public void setRelationalValue(Value relationalValue) {
		this.relationalValue = relationalValue;
	}

	public boolean isInserted() {
		return inserted;
	}

	public void setInserted(boolean inserted) {
		this.inserted = inserted;
	}

	public boolean isForced() {
		return forced;
	}

	public void setForced(boolean forced) {
		this.forced = forced;
	}

	public HibernateTypeDescriptor getExplicitHibernateTypeDescriptor() {
		return explicitHibernateTypeDescriptor;
	}

	@Override
	public String toString() {
		return "EntityDiscriminator{relationalValue=" + relationalValue
				+ ", forced=" + forced
				+ ", inserted=" + inserted + '}';
	}
}
