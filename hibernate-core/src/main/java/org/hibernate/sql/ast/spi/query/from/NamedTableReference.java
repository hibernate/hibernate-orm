package org.hibernate.sql.ast.spi.query.from;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.metamodel.mapping.AuxiliaryMapping;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.SelfRenderingSqlFragmentExpression;
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
		if ( mapping instanceof TemporalMapping temporalMapping
				&& temporalMapping.getTableName().equals( getTableExpression() ) ) {
			final var factory = influencers.getSessionFactory();
			final var changesets = factory.getChangesetCoordinator();
			final var dialect = factory.getJdbcServices().getDialect();
			final var temporalTableSupport = dialect.getTemporalTableSupport();
			final var temporalTableStrategy = factory.getSessionFactoryOptions().getTemporalTableStrategy();
			// a temporal entity in an all-revisions query is read as its current state
			final boolean current = influencers.getTemporalIdentifier() == null || influencers.isAllRevisions();
			if ( changesets.isIdentifierTypeInstant()
					&& temporalTableSupport.useAsOfOperator( temporalTableStrategy )
					&& ( !current || temporalTableSupport.useAsOfOperatorForCurrent( temporalTableStrategy ) ) ) {
				asOfTransactionIdentifier = current && changesets.useServerTimestamp( dialect )
						? new SelfRenderingSqlFragmentExpression(
								dialect.getCurrentTemporalSupport().currentTimestamp(),
								temporalMapping.getJdbcMapping()
						)
						: new TemporalJdbcParameter( temporalMapping.getEndingColumnMapping(), current );
			}
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
