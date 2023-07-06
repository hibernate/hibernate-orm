/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.validation;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.QueryException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.grammars.hql.HqlLexer;
import org.hibernate.grammars.hql.HqlParser;
import org.hibernate.query.hql.internal.HqlParseTreeBuilder;
import org.hibernate.query.hql.internal.SemanticQueryBuilder;
import org.hibernate.query.sqm.EntityTypeException;
import org.hibernate.query.sqm.PathElementException;
import org.hibernate.query.sqm.TerminalPathException;
import org.hibernate.type.descriptor.java.spi.JdbcTypeRecommendationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.lang.Character.isJavaIdentifierStart;
import static java.lang.Integer.parseInt;
import static java.util.stream.Stream.concat;

/**
 * The entry point for HQL validation.
 *
 * @author Gavin King
 */
public class Validation {

	public interface Handler extends ANTLRErrorListener {
		void error(int start, int end, String message);
		void warn(int start, int end, String message);

		int getErrorCount();
	}

	public static void validate(
			String hql,
			boolean checkParams, boolean checkTyping,
			Set<Integer> setParameterLabels,
			Set<String> setParameterNames,
			Handler handler,
			SessionFactoryImplementor factory) {
		validate( hql, checkParams, checkTyping, setParameterLabels, setParameterNames, handler, factory, 0 );
	}

	public static void validate(
			String hql,
			boolean checkParams, boolean checkTyping,
			Set<Integer> setParameterLabels,
			Set<String> setParameterNames,
			Handler handler,
			SessionFactoryImplementor factory,
			int errorOffset) {
//		handler = new Filter(handler, errorOffset);

		try {
			final HqlParser.StatementContext statementContext = parseAndCheckSyntax( hql, handler );
			if ( checkTyping && handler.getErrorCount() == 0 ) {
				checkTyping( hql, handler, factory, errorOffset, statementContext );
			}
			if ( checkParams ) {
				checkParameterBinding( hql, setParameterLabels, setParameterNames, handler, errorOffset );
			}
		}
		catch (Exception e) {
//			e.printStackTrace();
		}
	}

	private static void checkTyping(
			String hql,
			Handler handler,
			SessionFactoryImplementor factory,
			int errorOffset,
			HqlParser.StatementContext statementContext) {
		try {
			new SemanticQueryBuilder<>( Object[].class, () -> false, factory )
					.visitStatement( statementContext );
		}
		catch ( JdbcTypeRecommendationException ignored ) {
			// just squash these for now
		}
		catch ( QueryException | PathElementException | TerminalPathException | EntityTypeException
				| PropertyNotFoundException se ) { //TODO is this one really thrown by core? It should not be!
			String message = se.getMessage();
			if ( message != null ) {
				handler.error( -errorOffset +1, -errorOffset + hql.length(), message );
			}
		}
	}

	private static HqlParser.StatementContext parseAndCheckSyntax(String hql, Handler handler) {
		final HqlLexer hqlLexer = HqlParseTreeBuilder.INSTANCE.buildHqlLexer( hql );
		final HqlParser hqlParser = HqlParseTreeBuilder.INSTANCE.buildHqlParser( hql, hqlLexer );
		hqlLexer.addErrorListener( handler );
		hqlParser.getInterpreter().setPredictionMode( PredictionMode.SLL );
		hqlParser.removeErrorListeners();
		hqlParser.addErrorListener( handler );
		hqlParser.setErrorHandler( new BailErrorStrategy() );

		try {
			return hqlParser.statement();
		}
		catch ( ParseCancellationException e) {
			// reset the input token stream and parser state
			hqlLexer.reset();
			hqlParser.reset();

			// fall back to LL(k)-based parsing
			hqlParser.getInterpreter().setPredictionMode( PredictionMode.LL );
			hqlParser.setErrorHandler( new DefaultErrorStrategy() );

			return hqlParser.statement();
		}
	}

	private static void checkParameterBinding(
			String hql,
			Set<Integer> setParameterLabels,
			Set<String> setParameterNames,
			Handler handler,
			int errorOffset) {
		try {
			String unsetParams = null;
			String notSet = null;
			String parameters = null;
			int start = -1;
			int end = -1;
			List<String> names = new ArrayList<>();
			List<Integer> labels = new ArrayList<>();
			final HqlLexer hqlLexer = HqlParseTreeBuilder.INSTANCE.buildHqlLexer( hql );
			loop:
			while (true) {
				Token token = hqlLexer.nextToken();
				int tokenType = token.getType();
				switch (tokenType) {
					case HqlLexer.EOF:
						break loop;
					case HqlLexer.QUESTION_MARK:
					case HqlLexer.COLON:
						Token next = hqlLexer.nextToken();
						String text = next.getText();
						switch (tokenType) {
							case HqlLexer.COLON:
								if ( !text.isEmpty()
										&& isJavaIdentifierStart( text.codePointAt(0) ) ) {
									names.add(text);
									if ( setParameterNames.contains(text) ) {
										continue;
									}
								}
								else {
									continue;
								}
								break;
							case HqlLexer.QUESTION_MARK:
								if ( next.getType() == HqlLexer.INTEGER_LITERAL ) {
									int label;
									try {
										label = parseInt(text);
									}
									catch (NumberFormatException nfe) {
										continue;
									}
									labels.add(label);
									if ( setParameterLabels.contains(label) ) {
										continue;
									}
								}
								else {
									continue;
								}
								break;
							default:
								continue;
						}
						parameters = unsetParams == null ? "Parameter " : "Parameters ";
						notSet = unsetParams == null ? " is not set" : " are not set";
						unsetParams = unsetParams == null ? "" : unsetParams + ", ";
						unsetParams += token.getText() + text;
						if (start == -1) {
							start = token.getCharPositionInLine(); //TODO: wrong for multiline query strings!
						}
						end = token.getCharPositionInLine() + text.length() + 1;
						break;
				}
			}
			if ( unsetParams != null ) {
				handler.warn( start-errorOffset+1, end-errorOffset, parameters + unsetParams + notSet );
			}

			setParameterNames.removeAll(names);
			setParameterLabels.removeAll(labels);

			reportMissingParams( setParameterLabels, setParameterNames, handler );
		}
		finally {
			setParameterNames.clear();
			setParameterLabels.clear();
		}
	}

	private static void reportMissingParams(Set<Integer> setParameterLabels, Set<String> setParameterNames, Handler handler) {
		final int count = setParameterNames.size() + setParameterLabels.size();
		if (count > 0) {
			final String missingParams =
					concat( setParameterNames.stream().map(name -> ":" + name),
							setParameterLabels.stream().map(label -> "?" + label) )
							.reduce((x, y) -> x + ", " + y)
							.orElse(null);
			final String params =
					count == 1 ?
							"Parameter " :
							"Parameters ";
			final String notOccur =
					count == 1 ?
							" does not occur in the query" :
							" do not occur in the query";
			handler.warn(0, 0, params + missingParams + notOccur);
		}
	}
}
