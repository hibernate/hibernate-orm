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
 * Represents a named parameter coming from the query.
 *
 * @author Steve Ebersole
 */
public class NamedParameter extends AbstractParameter {
	private static final Logger log = Logger.getLogger( NamedParameter.class );

	private final String name;

	public NamedParameter(
			String name,
			SqlExpressableType inferredType,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		super( inferredType, clause, typeConfiguration );
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public void accept(SqlAstWalker  walker) {
		walker.visitNamedParameter( this );
	}
}
