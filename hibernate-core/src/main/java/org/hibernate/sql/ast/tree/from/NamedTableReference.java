/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.from;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.metamodel.mapping.AuditMapping;
import org.hibernate.metamodel.mapping.AuxiliaryMapping;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SelfRenderingSqlFragmentExpression;
import org.hibernate.sql.exec.internal.TemporalJdbcParameter;

import static org.hibernate.internal.util.StringHelper.isEmpty;

/**
 * Represents a reference to a "named" table in a query's from clause.
 *
 * @author Steve Ebersole
 */
public class NamedTableReference extends AbstractTableReference {
	private final String tableExpression;

	private String prunedTableExpression;
	private Expression asOfTransactionIdentifier;

	public NamedTableReference(
			String tableExpression,
			String identificationVariable) {
		this( tableExpression, identificationVariable, false );
	}

	public NamedTableReference(
			String tableExpression,
			String identificationVariable,
			boolean isOptional) {
		super( identificationVariable, isOptional );
		assert tableExpression != null;
		this.tableExpression = tableExpression;
	}

	public String getTableExpression() {
		return prunedTableExpression == null ? tableExpression : prunedTableExpression;
	}

	public void applyAuxiliaryTable(AuxiliaryMapping mapping, LoadQueryInfluencers influencers) {
		if ( useAsOfOperator( influencers, mapping )
				&& mapping.getTableName().equals( getTableExpression() ) ) {
			if ( influencers.getTemporalIdentifier() == null
				&& influencers.getSessionFactory().getTransactionIdentifierService().isDisabled()
				&& influencers.getSessionFactory().getJdbcServices().getDialect().isCurrentTimestampStable() ) {
				// we are querying current data,
				// so we can use the server timestamp
				final Dialect dialect = influencers.getSessionFactory().getJdbcServices().getDialect();
				this.asOfTransactionIdentifier =
						new SelfRenderingSqlFragmentExpression( dialect.currentTimestamp(), mapping.getJdbcMapping() );
			}
			else if ( mapping instanceof TemporalMapping temporalMapping ) {
				this.asOfTransactionIdentifier = new TemporalJdbcParameter( temporalMapping.getEndingColumnMapping() );
			}
			else if ( mapping instanceof AuditMapping auditMapping ) {
				this.asOfTransactionIdentifier = new TemporalJdbcParameter( auditMapping.getTransactionIdMapping() );
			}
			else {
				this.asOfTransactionIdentifier = null;
			}
		}
	}

	private boolean useAsOfOperator(LoadQueryInfluencers influencers, AuxiliaryMapping mapping) {
		if ( mapping == null ) {
			return false;
		}
		else {
			final var sessionFactory = influencers.getSessionFactory();
			return sessionFactory.getTransactionIdentifierService().isIdentifierTypeInstant()
				&& sessionFactory.getJdbcServices().getDialect().getTemporalTableSupport()
						.useAsOfOperator( sessionFactory.getSessionFactoryOptions().getTemporalTableStrategy() );
		}
	}

	public Expression getAsOfTransactionIdentifier() {
		return asOfTransactionIdentifier;
	}

	@Override
	public String getTableId() {
		return getTableExpression();
	}

	public void setPrunedTableExpression(String prunedTableExpression) {
		this.prunedTableExpression = prunedTableExpression;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitNamedTableReference( this );
	}

	@Override
	public List<String> getAffectedTableNames() {
		return Collections.singletonList( tableExpression );
	}

	@Override
	public boolean containsAffectedTableName(String requestedName) {
		return isEmpty( requestedName ) || tableExpression.equals( requestedName );
	}

	@Override
	public Boolean visitAffectedTableNames(Function<String, Boolean> nameCollector) {
		return nameCollector.apply( tableExpression );
	}

	@Override
	public TableReference resolveTableReference(
			NavigablePath navigablePath,
			String tableExpression) {
		if ( this.tableExpression.equals( tableExpression ) ) {
			return this;
		}

		throw new UnknownTableReferenceException(
				tableExpression,
				String.format(
						Locale.ROOT,
						"Unable to determine TableReference (`%s`) for `%s`",
						tableExpression,
						navigablePath
				)
		);
	}

	@Override
	public TableReference getTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean resolve) {
		return this.tableExpression.equals( tableExpression ) ? this : null;
	}

	@Override
	public String toString() {
		return getTableExpression() + "(" + getIdentificationVariable() + ')';
	}

}
