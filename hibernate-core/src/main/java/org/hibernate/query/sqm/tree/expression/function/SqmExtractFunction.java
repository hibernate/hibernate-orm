/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import java.util.Locale;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class SqmExtractFunction<T> extends AbstractSqmFunction<T> {
	public static final String NAME = "extract";

	private final SqmExtractUnit<T> unitToExtract;
	private final SqmExpression<?> extractionSource;

	public SqmExtractFunction(
			SqmExtractUnit<T> unitToExtract,
			SqmExpression<?> extractionSource) {
		super( unitToExtract.getType(), extractionSource.nodeBuilder() );
		this.unitToExtract = unitToExtract;
		this.extractionSource = extractionSource;
	}

	public SqmExtractUnit<T> getUnitToExtract() {
		return unitToExtract;
	}

	public SqmExpression<?> getExtractionSource() {
		return extractionSource;
	}

	@Override
	public String getFunctionName() {
		return NAME;
	}

	@Override
	public boolean hasArguments() {
		return true;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitExtractFunction( this );
	}

	@Override
	public String asLoggableText() {
		return String.format(
				Locale.ROOT,
				"%s( %s from %s )",
				NAME,
				getUnitToExtract().asLoggableText(),
				getExtractionSource().asLoggableText()
		);
	}
}
