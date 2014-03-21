/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.metamodel.internal.binder;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.UniqueKey;

/**
 * @author Brett Meyer
 */
public class NaturalIdUniqueKeyHelper {
	
	private final BinderRootContext helperContext;

	private Map<TableSpecification, UniqueKey> naturalIdUniqueKeys
			= new HashMap<TableSpecification, UniqueKey>();
	
	public NaturalIdUniqueKeyHelper(BinderRootContext helperContext) {
		this.helperContext = helperContext;
	}

	/**
	 * Natural ID columns must reside in one single UniqueKey within the Table.
	 * To prevent separate UniqueKeys from being created, this keeps track of
	 * them in a HashMap.
	 * 
	 * @param table
	 * @param column
	 */
	public void addUniqueConstraintForNaturalIdColumn(
			final TableSpecification table, final Column column) {
		UniqueKey uniqueKey;
		if ( naturalIdUniqueKeys.containsKey( table ) ) {
			uniqueKey = naturalIdUniqueKeys.get( table );
		}
		else {
			// TODO: For now, leave this out of the naming strategy.  It has nothing to do with the columns.
			String keyName = "UK_" + HashedNameUtil.hashedName( table.getLogicalName().getText() + "_NaturalID" );
			uniqueKey = new UniqueKey();
			uniqueKey.setTable( table );
			uniqueKey.setName( Identifier.toIdentifier( keyName ) );
			table.addUniqueKey( uniqueKey );
			naturalIdUniqueKeys.put( table, uniqueKey );
		}
		uniqueKey.addColumn( column );
	}
}
