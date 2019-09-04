/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.type.descriptor.EmptyJdbcValueExtractor;
import org.hibernate.type.descriptor.ValueExtractor;

/**
 * @author Steve Ebersole
 */
public class EmptySqlSelection implements SqlSelection {
	private final int position;

	public EmptySqlSelection(int position) {
		this.position = position;
	}

	@Override
	public ValueExtractor getJdbcValueExtractor() {
		return EmptyJdbcValueExtractor.INSTANCE;
	}

	@Override
	public int getJdbcResultSetIndex() {
		return -1;
	}

	@Override
	public int getValuesArrayPosition() {
		return position;
	}

	@Override
	public void accept(SqlAstWalker sqlAstWalker) {
		throw new UnsupportedOperationException( "Unexpected call to render empty EmptySqlSelection" );
	}
}
