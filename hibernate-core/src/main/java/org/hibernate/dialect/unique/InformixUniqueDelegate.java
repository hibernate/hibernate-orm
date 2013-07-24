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

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.relational.UniqueKey;

/**
 * Informix requires the constraint name to come last on the alter table.
 * 
 * @author Brett Meyer
 */
public class InformixUniqueDelegate extends DefaultUniqueDelegate {
	
	public InformixUniqueDelegate( Dialect dialect ) {
		super( dialect );
	}

	@Override
	public String applyUniquesOnAlter( org.hibernate.mapping.UniqueKey uniqueKey,
			String defaultCatalog, String defaultSchema ) {
		return new StringBuilder( "alter table " )
				.append( uniqueKey.getTable().getQualifiedName(
						dialect, defaultCatalog, defaultSchema ) )
				.append( " add constraint " )
				.append( uniqueConstraintSql( uniqueKey ) )
				.append( " constraint " )
				.append( uniqueKey.getName() )
				.toString();
	}

	@Override
	public String applyUniquesOnAlter( UniqueKey uniqueKey  ) {
		return new StringBuilder( "alter table " )
				.append( uniqueKey.getTable().getQualifiedName( dialect ) )
				.append( " add constraint " )
				.append( uniqueConstraintSql( uniqueKey ) )
				.append( " constraint " )
				.append( uniqueKey.getName() )
				.toString();
	}
}
