/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.expression;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.sqm.exec.results.internal.ReturnReaderScalarImpl;
import org.hibernate.sql.sqm.exec.results.spi.ReturnReader;

/**
 * @author Steve Ebersole
 */
public abstract class SelfReadingExpressionSupport implements Expression {
	@Override
	public ReturnReader getReturnReader(int startPosition, boolean shallow, SessionFactoryImplementor sessionFactory) {
		return new ReturnReaderScalarImpl( startPosition, getType() );
	}
}
