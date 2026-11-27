package org.hibernate.sql.ast.spi.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;

import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.ast.spi.query.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.spi.query.predicate.InListPredicate;
import org.hibernate.sql.ast.spi.query.predicate.Junction;

import static org.hibernate.query.sqm.ComparisonOperator.DISTINCT_FROM;
import static org.hibernate.sql.ast.spi.query.predicate.Junction.Nature.DISJUNCTION;

/// An insert stores the proposed version. An update stores it only if a versioned column changed.
public final class ConditionalVersionAssignment extends ColumnValueBinding {
	private final EntityVersionMapping version;
	private final List<SelectableMapping> comparedColumns;

	public ConditionalVersionAssignment(ColumnReference column, EntityVersionMapping version, List<SelectableMapping> comparedColumns) {
		super( column, new ColumnWriteFragment( "?", new ColumnValueParameter( column, ParameterUsage.SET ),
				version.getVersionAttribute() ) );
		this.version = version;
		this.comparedColumns = comparedColumns;
	}

	@Override
	public boolean isUpdateConditional() {
		return true;
	}

	@Override
	public ColumnValueBinding forUpdate(@Nullable String targetAlias) {
		final var versionColumn = new ColumnReference( targetAlias, version.getVersionAttribute() );
		final var changed = new Junction( DISJUNCTION );
		final List<ColumnValueParameter> parameters = new ArrayList<>();
		for ( var selectable : comparedColumns ) {
			final var column = new ColumnReference( targetAlias, selectable );
			final var parameter = new ColumnValueParameter( column, ParameterUsage.RESTRICT );
			parameters.add( parameter );
			changed.add( new ComparisonPredicate( column, DISTINCT_FROM, parameter ) );
		}
		if ( changed.isEmpty() ) {
			// An empty IN list renders as false when every property is excluded.
			changed.add( new InListPredicate( versionColumn ) );
		}
		final var nextVersion = new ColumnValueParameter( versionColumn, ParameterUsage.SET );
		parameters.add( nextVersion );
		final var expression = new CaseSearchedExpression( version );
		expression.when( changed, nextVersion );
		expression.otherwise( versionColumn );
		return new ColumnValueBinding( getColumnReference(),
				new ColumnWriteFragment( parameters, version.getVersionAttribute(), expression ) );
	}
}
