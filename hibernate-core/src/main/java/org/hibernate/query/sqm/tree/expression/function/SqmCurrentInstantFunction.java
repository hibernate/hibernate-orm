/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;

/**
 * Form of CURRENT_TIMESTAMP returning {@link java.time.Instant} rather than
 * {@link java.sql.Timestamp}
 *
 * @author Steve Ebersole
 */
public class SqmCurrentInstantFunction extends AbstractSqmFunction {
	public static final String NAME = "current_instant";

	public SqmCurrentInstantFunction() {
		super( null );
	}

	@Override
	public String getFunctionName() {
		return NAME;
	}

	@Override
	public boolean hasArguments() {
		return false;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitCurrentInstantFunction( this );
	}

	@Override
	public String asLoggableText() {
		return NAME;
	}
}
