/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import org.hibernate.QueryException;
import org.hibernate.query.sqm.InterpretationException;
import org.hibernate.query.hql.SemanticQueryProducer;
import org.hibernate.query.sqm.internal.SqmTreePrinter;
import org.hibernate.query.sqm.spi.SqmCreationContext;
import org.hibernate.query.hql.spi.SqmCreationOptions;
import org.hibernate.query.sqm.tree.SqmStatement;

/**
 * Standard implementation of SemanticQueryInterpreter
 *
 * @author Steve Ebersole
 */
public class SemanticQueryProducerImpl implements SemanticQueryProducer {
	private final SqmCreationContext sqmCreationContext;
	private final SqmCreationOptions sqmCreationOptions;

	public SemanticQueryProducerImpl(
			SqmCreationContext sqmCreationContext,
			SqmCreationOptions sqmCreationOptions) {
		this.sqmCreationContext = sqmCreationContext;
		this.sqmCreationOptions = sqmCreationOptions;
	}

	@Override
	public SqmStatement interpret(String query) {
//		final ParsingContext parsingContext = ;

		// first, ask Antlr to build the parse tree
		final HqlParser parser = HqlParseTreeBuilder.INSTANCE.parseHql( query );

		// Log the parse tree (if enabled)
		HqlParseTreePrinter.logStatementParseTree( parser );

		// then we perform semantic analysis and build the semantic representation...
		try {
			final SqmStatement sqmStatement = SemanticQueryBuilder.buildSemanticModel(
					parser.statement(),
					sqmCreationOptions,
					sqmCreationContext
			);

			SqmTreePrinter.logTree( sqmStatement );

			return sqmStatement;
		}
		catch (QueryException e) {
			throw e;
		}
		catch (Exception e) {
			throw new InterpretationException( query, e );
		}
	}
}
