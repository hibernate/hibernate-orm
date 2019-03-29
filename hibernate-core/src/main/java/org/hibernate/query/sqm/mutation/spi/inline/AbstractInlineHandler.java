/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.spi.inline;

import java.util.Collections;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper;
import org.hibernate.query.sqm.mutation.spi.Handler;
import org.hibernate.query.sqm.mutation.spi.HandlerCreationContext;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractInlineHandler implements Handler {
	private final SqmDeleteOrUpdateStatement sqmStatement;
	private final DomainParameterXref domainParameterXref;
	private final HandlerCreationContext creationContext;

	public AbstractInlineHandler(
			SqmDeleteOrUpdateStatement sqmStatement,
			DomainParameterXref domainParameterXref,
			HandlerCreationContext creationContext) {
		this.sqmStatement = sqmStatement;
		this.domainParameterXref = domainParameterXref;
		this.creationContext = creationContext;
	}

	protected EntityTypeDescriptor<?> getEntityDescriptor() {
		return sqmStatement.getTarget().getReferencedNavigable();
	}

	public SqmDeleteOrUpdateStatement getSqmStatement() {
		return sqmStatement;
	}

	public DomainParameterXref getDomainParameterXref() {
		return domainParameterXref;
	}

	protected List<Object> selectMatchingIds(ExecutionContext executionContext) {
		return SqmMutationStrategyHelper.selectMatchingIds( domainParameterXref, sqmStatement, executionContext );
	}

}
