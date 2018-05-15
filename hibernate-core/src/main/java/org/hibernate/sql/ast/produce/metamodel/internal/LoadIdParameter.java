/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.internal;

import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.spi.expression.AbstractParameter;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class LoadIdParameter extends AbstractParameter {
	// todo (6.0) : this is really a "domain query" concern.  The SQL AST should have one per atomic id value (column)

	private final int idValueIndex;

	public LoadIdParameter(AllowableParameterType type, TypeConfiguration typeConfiguration) {
		this( 0, type, typeConfiguration );

	}

	public LoadIdParameter(int idValueIndex, AllowableParameterType restrictedNavigable, TypeConfiguration typeConfiguration) {
		super( null, Clause.WHERE, typeConfiguration );
//		super( restrictedNavigable, Clause.WHERE, typeConfiguration );
		this.idValueIndex = idValueIndex;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitGenericParameter( this );
	}
}
