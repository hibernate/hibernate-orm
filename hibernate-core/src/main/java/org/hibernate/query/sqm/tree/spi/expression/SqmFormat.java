/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import java.util.regex.Pattern;

import jakarta.annotation.Nonnull;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * Effectively a query-literal but we want to handle it specially in the SQM to SQL AST conversion
 *
 * @author Gavin King
 */
public class SqmFormat extends SqmLiteral<String> {
	// G era
	// y year in era
	// Y week year (ISO)
	// M month in year
	// w week in year (ISO)
	// W week in month
	// E day name in week
	// e day number in week (*very* inconsistent across DBs)
	// d day in month
	// D day in year
	// a AM/PM
	// H hour of day (0-23)
	// h clock hour of am/pm (1-12)
	// m minute of hour
	// s second of minute
	// S fraction of second
	// z time zone name e.g. PST
	// x zone offset e.g. +03, +0300, +03:00
	// Z zone offset e.g. +0300
	// see https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
	private static final Pattern FORMAT = Pattern.compile( "('[^']+'|[:;/,.!@#$^&?~`|()\\[\\]{}<>\\-+*=]|\\s|G{1,2}|[yY]{1,4}|M{1,4}|w{1,2}|W|E{3,4}|e{1,2}|d{1,2}|D{1,3}|a|[Hhms]{1,2}|S{1,6}|[zZx]{1,3})*");

	public SqmFormat(
			String value,
			SqmBindableType<String> inherentType,
			NodeBuilder nodeBuilder) {
		super(value, inherentType, nodeBuilder);
		if (!FORMAT.matcher(value).matches()) {
			throw new SemanticException("Illegal format pattern '" + value + "'");
		}
	}

	@Override
	public @Nonnull SqmBindableType<String> getNodeType() {
		return castNonNull( super.getNodeType() );
	}

	@Override
	public @Nonnull String getLiteralValue() {
		return castNonNull( super.getLiteralValue() );
	}

	@Override
	public SqmFormat copy(SqmCopyContext context) {
		final SqmFormat existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmFormat expression = context.registerCopy(
				this,
				new SqmFormat(
						getLiteralValue(),
						getNodeType(),
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	@Override
	public <R> R accept(SemanticQueryWalker<R> walker) {
		return walker.visitFormat( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( getLiteralValue() );
	}
}
