/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import jakarta.persistence.TemporalType;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.ExtractFunction;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.ExtractUnit;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static org.hibernate.type.spi.TypeConfiguration.getSqlTemporalType;

/**
 * GaussDB-specific {@link ExtractFunction} that casts {@code DATE} sources to
 * {@code timestamp} before extracting temporal units other than
 * {@code year}/{@code month}/{@code day}.
 *
 * <p>GaussDB M mode exposes {@code DATE} as the non-standard {@code datea} type, and its
 * {@code extract()}/{@code date_part()} only recognizes {@code year}, {@code month}, and
 * {@code day} units when the argument is a {@code DATE} — every other unit
 * ({@code week}, {@code doy}, {@code dow}, {@code epoch}, {@code quarter}, {@code hour},
 * {@code minute}, {@code second}, {@code microsecond}, ...) raises
 * {@code "Date units ... cannot be recognized"}. {@code TIMESTAMP} sources recognize the
 * full unit set, so casting a {@code DATE} source to {@code timestamp} makes the remaining
 * units work.
 *
 * <p>The cast is applied only when the source type is {@link TemporalType#DATE}, so
 * {@code timestamp}/{@code timestamptz}/{@code time} sources are left untouched: casting
 * {@code timestamptz} to {@code timestamp} would shift {@code epoch} by the zone offset,
 * and casting {@code time} to {@code timestamp} is unsupported by the database.
 *
 * <p>The cast is injected by rewriting the dialect's {@code extractPattern(unit)}, wrapping
 * the {@code ?2} (source) placeholder in {@code cast(?2 as timestamp)}, so any
 * dialect-specific pattern logic (e.g. the {@code DAY_OF_WEEK} {@code +1} adjustment in
 * {@link GaussDBDialect}) is preserved.
 */
public class GaussDBExtractFunction extends ExtractFunction {
	// ExtractFunction.dialect is package-private, so it is inaccessible from this package;
	// keep our own reference to call the public Dialect#extractPattern(TemporalUnit).
	private final Dialect gaussDialect;

	public GaussDBExtractFunction(Dialect dialect, TypeConfiguration typeConfiguration) {
		super( dialect, typeConfiguration );
		this.gaussDialect = dialect;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final ExtractUnit field = (ExtractUnit) sqlAstArguments.get( 0 );
		final TemporalUnit unit = field.getUnit();
		String pattern = gaussDialect.extractPattern( unit );
		final Expression expression = (Expression) sqlAstArguments.get( 1 );
		final JdbcMappingContainer type = expression.getExpressionType();
		final TemporalType temporalType = type != null ? getSqlTemporalType( type ) : null;
		// A mode DATE (datea) only recognizes year/month/day in extract(); cast DATE sources to
		// timestamp for every other unit. M mode (MySQL-compatible) instead uses MySQL functions via
		// extractPattern (dayofweek/year/...), which accept a DATE source directly — and `cast(?2 as
		// timestamp)` is itself a syntax error in M mode — so skip the cast there.
		final boolean mMode = gaussDialect instanceof GaussDBDialect g && g.isMMode();
		if ( !mMode && temporalType == TemporalType.DATE && needsTimestampSource( unit ) ) {
			pattern = pattern.replace( "?2", "cast(?2 as timestamp)" );
		}
		new PatternRenderer( pattern ).render( sqlAppender, sqlAstArguments, walker );
	}

	/**
	 * Whether a {@code DATE} source must be cast to {@code timestamp} for the given unit.
	 * GaussDB M mode {@code DATE} ({@code datea}) only supports {@code year}/{@code month}/
	 * {@code day} in {@code extract()}; all other units need a {@code timestamp} source.
	 */
	private static boolean needsTimestampSource(TemporalUnit unit) {
		return switch ( unit ) {
			case YEAR, MONTH, DAY -> false;
			default -> true;
		};
	}
}
