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

	// legacy model ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public String getAlterTableToAddUniqueKeyCommand(
			org.hibernate.mapping.UniqueKey uniqueKey,
			String defaultCatalog,
			String defaultSchema) {
		// Do this here, rather than allowing UniqueKey/Constraint to do it.
		// We need full, simplified control over whether or not it happens.
		final String tableName = uniqueKey.getTable().getQualifiedName( dialect, defaultCatalog, defaultSchema );
		final String constraintName = dialect.quote( uniqueKey.getName() );
		return "alter table " + tableName + " add constraint " + uniqueConstraintSql( uniqueKey ) + " constraint " + constraintName;
	}

	// new model ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey) {
		// Do this here, rather than allowing UniqueKey/Constraint to do it.
		// We need full, simplified control over whether or not it happens.
		final String tableName = uniqueKey.getTable().getQualifiedName( dialect );
		final String constraintName = dialect.quote( uniqueKey.getName() );

		return "alter table " + tableName + " add constraint " + uniqueConstraintSql( uniqueKey ) + " constraint " + constraintName;
	}
}
