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
public class TopLimitHandler extends AbstractLimitHandler {
	
	private final boolean supportsVariableLimit;
	
	private final boolean bindLimitParametersFirst;
	
	public TopLimitHandler(String sql, RowSelection selection,
			boolean supportsVariableLimit, boolean bindLimitParametersFirst) {
		super(sql, selection);
		this.supportsVariableLimit = supportsVariableLimit;
		this.bindLimitParametersFirst = bindLimitParametersFirst;
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
		return supportsVariableLimit;
	}
	
	public boolean bindLimitParametersFirst() {
		return bindLimitParametersFirst;
	}

	@Override
	public String getProcessedSql() {
		if ( LimitHelper.hasFirstRow(selection) ) {
			throw new UnsupportedOperationException( "query result offset is not supported" );
		}

		final int selectIndex = sql.toLowerCase().indexOf( "select" );
		final int selectDistinctIndex = sql.toLowerCase().indexOf( "select distinct" );
		final int insertionPoint = selectIndex + (selectDistinctIndex == selectIndex ? 15 : 6);

		return new StringBuilder( sql.length() + 8 )
				.append( sql )
				.insert( insertionPoint, " TOP ? " )
				.toString();
	}
}
