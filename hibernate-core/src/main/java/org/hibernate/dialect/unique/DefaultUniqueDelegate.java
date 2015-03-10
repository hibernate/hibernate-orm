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

import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.UniqueKey;

/**
 * The default UniqueDelegate implementation for most dialects.  Uses
 * separate create/alter statements to apply uniqueness to a column.
 * 
 * @author Brett Meyer
 */
public class DefaultUniqueDelegate implements UniqueDelegate {
	protected final Dialect dialect;

	/**
	 * Constructs DefaultUniqueDelegate
	 *
	 * @param dialect The dialect for which we are handling unique constraints
	 */
	public DefaultUniqueDelegate( Dialect dialect ) {
		this.dialect = dialect;
	}

	// legacy model ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public String getColumnDefinitionUniquenessFragment(org.hibernate.mapping.Column column) {
		return "";
	}

	@Override
	public String getTableCreationUniqueConstraintsFragment(org.hibernate.mapping.Table table) {
		return "";
	}

	@Override
	public String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata) {
		final JdbcEnvironment jdbcEnvironment = metadata.getDatabase().getJdbcEnvironment();

		final String tableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
				uniqueKey.getTable().getQualifiedTableName(),
				dialect
		);

		final String constraintName = dialect.quote( uniqueKey.getName() );
		return "alter table " + tableName + " add constraint " + constraintName + " " + uniqueConstraintSql( uniqueKey );
	}

	protected String uniqueConstraintSql(UniqueKey uniqueKey) {
		final StringBuilder sb = new StringBuilder();
		sb.append( " unique (" );
		final Iterator<org.hibernate.mapping.Column> columnIterator = uniqueKey.columnIterator();
		while ( columnIterator.hasNext() ) {
			final org.hibernate.mapping.Column column = columnIterator.next();
			sb.append( column.getQuotedName( dialect ) );
			if ( uniqueKey.getColumnOrderMap().containsKey( column ) ) {
				sb.append( " " ).append( uniqueKey.getColumnOrderMap().get( column ) );
			}
			if ( columnIterator.hasNext() ) {
				sb.append( ", " );
			}
		}

		return sb.append( ')' ).toString();
	}

	@Override
	public String getAlterTableToDropUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata) {
		final JdbcEnvironment jdbcEnvironment = metadata.getDatabase().getJdbcEnvironment();

		final String tableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
				uniqueKey.getTable().getQualifiedTableName(),
				dialect
		);

		final StringBuilder buf = new StringBuilder( "alter table " );
		buf.append( tableName );
		buf.append(" drop constraint " );
		if ( dialect.supportsIfExistsBeforeConstraintName() ) {
			buf.append( "if exists " );
		}
		buf.append( dialect.quote( uniqueKey.getName() ) );
		if ( dialect.supportsIfExistsAfterConstraintName() ) {
			buf.append( " if exists" );
		}
		return buf.toString();
	}

}
