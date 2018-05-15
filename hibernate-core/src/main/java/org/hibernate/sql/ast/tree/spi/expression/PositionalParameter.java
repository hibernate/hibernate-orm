/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class PositionalParameter extends AbstractParameter {
	private static final Logger log = Logger.getLogger( PositionalParameter.class );

	private final int position;

	public PositionalParameter(
			int position,
			SqlExpressableType inferredType,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		super( inferredType, clause, typeConfiguration );
		this.position = position;
	}

	public int getPosition() {
		return position;
	}

	@Override
	public void accept(SqlAstWalker  walker) {
		walker.visitPositionalParameter( this );
	}
}
