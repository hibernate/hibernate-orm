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

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.relational.Table;
import org.hibernate.metamodel.relational.UniqueKey;

/**
 * The default UniqueDelegate implementation for most dialects.  Uses
 * separate create/alter statements to apply uniqueness to a column.
 * 
 * @author Brett Meyer
 */
public class DefaultUniqueDelegate implements UniqueDelegate {
	
	protected final Dialect dialect;
	
	public DefaultUniqueDelegate( Dialect dialect ) {
		this.dialect = dialect;
	}
	
	@Override
	public String applyUniqueToColumn( org.hibernate.mapping.Column column ) {
		return "";
	}
	
	@Override
	public String applyUniqueToColumn( Column column ) {
		return "";
	}

	@Override
	public String applyUniquesToTable( org.hibernate.mapping.Table table ) {
		return "";
	}

	@Override
	public String applyUniquesToTable( Table table ) {
		return "";
	}
	
	@Override
	public String applyUniquesOnAlter( org.hibernate.mapping.UniqueKey uniqueKey,
			String defaultCatalog, String defaultSchema ) {
		// Do this here, rather than allowing UniqueKey/Constraint to do it.
		// We need full, simplified control over whether or not it happens.
		return new StringBuilder( "alter table " )
				.append( uniqueKey.getTable().getQualifiedName(
						dialect, defaultCatalog, defaultSchema ) )
				.append( " add constraint " )
				.append( uniqueKey.getName() )
				.append( uniqueConstraintSql( uniqueKey ) )
				.toString();
	}
	
	@Override
	public String applyUniquesOnAlter( UniqueKey uniqueKey  ) {
		// Do this here, rather than allowing UniqueKey/Constraint to do it.
		// We need full, simplified control over whether or not it happens.
		return new StringBuilder( "alter table " )
				.append( uniqueKey.getTable().getQualifiedName( dialect ) )
				.append( " add constraint " )
				.append( uniqueKey.getName() )
				.append( uniqueConstraintSql( uniqueKey ) )
				.toString();
	}
	
	@Override
	public String dropUniquesOnAlter( org.hibernate.mapping.UniqueKey uniqueKey,
			String defaultCatalog, String defaultSchema ) {
		// Do this here, rather than allowing UniqueKey/Constraint to do it.
		// We need full, simplified control over whether or not it happens.
		return new StringBuilder( "alter table " )
				.append( uniqueKey.getTable().getQualifiedName(
						dialect, defaultCatalog, defaultSchema ) )
				.append( " drop constraint " )
				.append( dialect.quote( uniqueKey.getName() ) )
				.toString();
	}
	
	@Override
	public String dropUniquesOnAlter( UniqueKey uniqueKey  ) {
		// Do this here, rather than allowing UniqueKey/Constraint to do it.
		// We need full, simplified control over whether or not it happens.
		return new StringBuilder( "alter table " )
				.append( uniqueKey.getTable().getQualifiedName( dialect ) )
				.append( " drop constraint " )
				.append( dialect.quote( uniqueKey.getName() ) )
				.toString();
	}
	
	@Override
	public String uniqueConstraintSql( org.hibernate.mapping.UniqueKey uniqueKey ) {
		StringBuilder sb = new StringBuilder();
		sb.append( " unique (" );
		Iterator columnIterator = uniqueKey.getColumnIterator();
		while ( columnIterator.hasNext() ) {
			org.hibernate.mapping.Column column
					= (org.hibernate.mapping.Column) columnIterator.next();
			sb.append( column.getQuotedName( dialect ) );
			if ( columnIterator.hasNext() ) {
				sb.append( ", " );
			}
		}
		
		return sb.append( ')' ).toString();
	}
	
	@Override
	public String uniqueConstraintSql( UniqueKey uniqueKey ) {
		StringBuilder sb = new StringBuilder();
		sb.append( " unique (" );
		Iterator columnIterator = uniqueKey.getColumns().iterator();
		while ( columnIterator.hasNext() ) {
			org.hibernate.mapping.Column column
					= (org.hibernate.mapping.Column) columnIterator.next();
			sb.append( column.getQuotedName( dialect ) );
			if ( columnIterator.hasNext() ) {
				sb.append( ", " );
			}
		}
		
		return sb.append( ')' ).toString();
	}

}
