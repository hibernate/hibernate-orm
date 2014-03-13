/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.dialect.pagination;

import org.hibernate.engine.spi.RowSelection;


/**
 * @author Brett Meyer
 */
public class FirstLimitHandler extends AbstractLimitHandler {
	
	public FirstLimitHandler(String sql, RowSelection selection) {
		super(sql, selection);
	}
	
	@Override
	public String getProcessedSql() {
		boolean hasOffset = LimitHelper.hasFirstRow(selection);
		if ( hasOffset ) {
			throw new UnsupportedOperationException( "query result offset is not supported" );
		}
		return new StringBuilder( sql.length() + 16 )
				.append( sql )
				.insert( sql.toLowerCase().indexOf( "select" ) + 6, " first ?" )
				.toString();
	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public boolean useMaxForLimit() {
		return true;
	}

	@Override
	public boolean supportsLimitOffset() {
		return false;
	}

	@Override
	public boolean supportsVariableLimit() {
		return false;
	}
}
