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

import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class ParentheticalOptionallyOrderedSet extends Operation {

	ParentheticalOptionallyOrderedSet( SqlObject parent, Operation parentheses ) {
		super( parent, parentheses.operator, parentheses.precedence );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.testing.sql.UnaryOperation#constructOperandList()
	 */
	@Override
	protected List< SqlObject > constructOperandList() {
		return new OptionallyOrderedSet< SqlObject >();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder( operator );
		builder.append( ' ' );
		if ( !operands.isEmpty() ) {
			Iterator< SqlObject > iter = operands.iterator();
			builder.append( iter.next() );
			while ( iter.hasNext() ) {
				builder.append( ", " ).append( iter.next() );
			}
		}
		builder.append( " )" );
		return builder.toString();
	}
}
