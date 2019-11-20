/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.cte;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.AbstractMutationHandler;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.sql.ast.tree.cte.CteTable;

/**
 * Defines how identifier values are selected from the updatable/deletable tables.
 *
 * @author Evandro Pires da Silva
 * @author Vlad Mihalcea
 * @author Steve Ebersole
 */
public abstract class AbstractCteMutationHandler extends AbstractMutationHandler {
	private final CteTable cteTable;
	private final DomainParameterXref domainParameterXref;
	private final CteStrategy strategy;

	public AbstractCteMutationHandler(
			CteTable cteTable,
			SqmDeleteOrUpdateStatement sqmStatement,
			DomainParameterXref domainParameterXref,
			CteStrategy strategy,
			SessionFactoryImplementor sessionFactory) {
		super( sqmStatement, sessionFactory );
		this.cteTable = cteTable;
		this.domainParameterXref = domainParameterXref;

		this.strategy = strategy;
	}

	public CteTable getCteTable() {
		return cteTable;
	}

	public DomainParameterXref getDomainParameterXref() {
		return domainParameterXref;
	}

	public CteStrategy getStrategy() {
		return strategy;
	}
}
