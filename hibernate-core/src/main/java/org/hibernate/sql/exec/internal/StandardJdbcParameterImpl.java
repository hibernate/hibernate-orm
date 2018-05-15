/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import java.util.Objects;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.spi.expression.AbstractParameter;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class StandardJdbcParameterImpl extends AbstractParameter {
	private final int position;

	public StandardJdbcParameterImpl(
			int position,
			SqlExpressableType type,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		super( type, clause, typeConfiguration );
		this.position = position;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitGenericParameter( this );
	}

	@Override
	public int hashCode() {
		return Objects.hash( position );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		StandardJdbcParameterImpl that = (StandardJdbcParameterImpl) o;
		return position == that.position;
	}
}
