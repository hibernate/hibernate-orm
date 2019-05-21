/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.spi.simple;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.AbstractMutationHandler;
import org.hibernate.query.sqm.mutation.spi.HandlerCreationContext;
import org.hibernate.query.sqm.mutation.spi.UpdateHandler;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.exec.spi.ExecutionContext;

/**
 * @author Steve Ebersole
 */
public class SimpleUpdateHandler extends AbstractMutationHandler implements UpdateHandler {
	private final SqmUpdateStatement sqmStatement;
	private final DomainParameterXref domainParameterXref;

	public SimpleUpdateHandler(
			SqmUpdateStatement sqmStatement,
			DomainParameterXref domainParameterXref,
			HandlerCreationContext creationContext) {
		super( sqmStatement, creationContext );
		this.sqmStatement = sqmStatement;
		this.domainParameterXref = domainParameterXref;
	}

	@Override
	public int execute(ExecutionContext executionContext) {
		throw new NotYetImplementedFor6Exception(  );
	}
}
