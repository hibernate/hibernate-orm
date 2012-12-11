/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.dialect.unique;

import java.util.Iterator;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;

/**
 * The default UniqueDelegate implementation for most dialects.  Uses
 * separate create/alter statements to apply uniqueness to a column.
 * 
 * @author Brett Meyer
 */
public class DefaultUniqueDelegate implements UniqueDelegate {
	
	private final Dialect dialect;
	
	public DefaultUniqueDelegate( Dialect dialect ) {
		this.dialect = dialect;
	}

	@Override
	public void applyUnique( Table table, Column column, StringBuilder sb ) {
//		if ( column.isUnique()
//				&& ( column.isNullable()
//						|| dialect.supportsNotNullUnique() ) ) {
//			if ( dialect.supportsUniqueConstraintInCreateAlterTable() ) {
//				// If the constraint is supported, do not add to the column syntax.
//				UniqueKey uk = getOrCreateUniqueKey( column.getQuotedName( dialect ) + '_' );
//				uk.addColumn( column );
//			}
//			else if ( dialect.supportsUnique() ) {
//				// Otherwise, add to the column syntax if supported.
//				sb.append( " unique" );
//			}
//		}
		
		UniqueKey uk = table.getOrCreateUniqueKey(
				column.getQuotedName( dialect ) + '_' );
		uk.addColumn( column );
	}

	@Override
	public void createUniqueConstraint( Table table, StringBuilder sb ) {
//		if ( dialect.supportsUniqueConstraintInCreateAlterTable() ) {
//			Iterator ukiter = getUniqueKeyIterator();
//			while ( ukiter.hasNext() ) {
//				UniqueKey uk = (UniqueKey) ukiter.next();
//				String constraint = uk.sqlConstraintString( dialect );
//				if ( constraint != null ) {
//					buf.append( ", " ).append( constraint );
//				}
//			}
//		}
		
		Iterator ukiter = table.getUniqueKeyIterator();
		while ( ukiter.hasNext() ) {
			UniqueKey uk = (UniqueKey) ukiter.next();
			String constraint = uk.sqlConstraintString( dialect );
			if ( constraint != null ) {
				sb.append( ", " ).append( constraint );
			}
		}
	}

}
