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
package org.hibernate.persister.walking.spi;

import java.util.Arrays;

/**
 * Used to uniquely identify a foreign key, so that we don't join it more than once creating circularities.
 * <p/>
 * bit of a misnomer to call this an association attribute.  But this follows the legacy use of AssociationKey
 * from old JoinWalkers to denote circular join detection
 */
public class AssociationKey {
	private final String table;
	private final String[] columns;

	public AssociationKey(String table, String[] columns) {
		this.table = table;
		this.columns = columns;
	}

	@Override
	public boolean equals(Object other) {
		AssociationKey that = (AssociationKey) other;
		return that.table.equals(table) && Arrays.equals( columns, that.columns );
	}

	@Override
	public int hashCode() {
		return table.hashCode(); //TODO: inefficient
	}
}
