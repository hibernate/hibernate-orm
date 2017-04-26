/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.internal;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmQuerySpec;
import org.hibernate.query.sqm.tree.SqmSelectStatement;

/**
 * @author Steve Ebersole
 */
public class SqmSelectStatementImpl extends AbstractSqmStatement implements SqmSelectStatement {
	private SqmQuerySpec querySpec;

	public SqmSelectStatementImpl() {
	}

	@Override
	public SqmQuerySpec getQuerySpec() {
		return querySpec;
	}

	public void applyQuerySpec(SqmQuerySpec querySpec) {
		if ( this.querySpec != null ) {
			throw new IllegalStateException( "SqmQuerySpec was already defined for select-statement" );
		}
		this.querySpec = querySpec;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitSelectStatement( this );
	}
}
