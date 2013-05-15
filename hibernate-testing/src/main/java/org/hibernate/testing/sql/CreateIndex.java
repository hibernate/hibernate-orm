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
package org.hibernate.testing.sql;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class CreateIndex extends DdlStatement implements NamedObject {

	public Name name;
	public Reference table;
	public List< Reference > columns = new ArrayList< Reference >();

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.testing.sql.NamedObject#name()
	 */
	@Override
	public Name name() {
		return name;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.testing.sql.NamedObject#setName(org.hibernate.testing.sql.Name)
	 */
	@Override
	public void setName( Name name ) {
		this.name = name;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder( "CREATE INDEX " );
		builder.append( name ).append( " ON " ).append( table ).append( ' ' );
		collectionToStringInParentheses( builder, columns );
		return builder.toString();
	}
}
