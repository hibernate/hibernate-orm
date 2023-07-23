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
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.checkerframework.checker.nullness.qual.Nullable;
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
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.type.descriptor.java.spi.JdbcTypeRecommendationException;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import static org.hibernate.jpamodelgen.validation.ProcessorSessionFactory.getEntityName;
import static org.hibernate.jpamodelgen.validation.ProcessorSessionFactory.isEntity;


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

	public static @Nullable SqmStatement<?> validate(
			String hql,
			@Nullable TypeMirror returnType,
			boolean checkTyping,
			Handler handler,
			SessionFactoryImplementor factory) {
		return validate( hql, returnType, checkTyping, handler, factory, 0 );
	}

	public static @Nullable SqmStatement<?> validate(
			String hql,
			@Nullable TypeMirror returnType,
			boolean checkTyping,
			Handler handler,
			SessionFactoryImplementor factory,
			int errorOffset) {
		try {
			final HqlParser.StatementContext statementContext = parseAndCheckSyntax( hql, handler );
			if ( checkTyping && handler.getErrorCount() == 0 ) {
				return checkTyping( hql, returnType, handler, factory, errorOffset, statementContext );
			}
		}
		catch (Exception e) {
//			e.printStackTrace();
		}
		return null;
	}

	private static @Nullable SqmStatement<?> checkTyping(
			String hql,
			@Nullable TypeMirror returnType,
			Handler handler,
			SessionFactoryImplementor factory,
			int errorOffset,
			HqlParser.StatementContext statementContext) {
		try {
			return createSemanticQueryBuilder( returnType, factory ).visitStatement( statementContext );
		}
		catch ( JdbcTypeRecommendationException ignored ) {
			// just squash these for now
		}
		catch ( QueryException | PathElementException | TerminalPathException | EntityTypeException
				| PropertyNotFoundException se ) { //TODO is this one really thrown by core? It should not be!
			final String message = se.getMessage();
			if ( message != null ) {
				handler.error( -errorOffset +1, -errorOffset + hql.length(), message );
			}
		}
		return null;
	}

	private static SemanticQueryBuilder<?> createSemanticQueryBuilder(
			@Nullable TypeMirror returnType, SessionFactoryImplementor factory) {
		if ( returnType != null && returnType.getKind() == TypeKind.DECLARED ) {
			final DeclaredType declaredType = (DeclaredType) returnType;
			final TypeElement typeElement = (TypeElement) declaredType.asElement();
			if ( isEntity( typeElement ) ) {
				return new SemanticQueryBuilder<>( getEntityName( typeElement ), () -> false, factory );
			}
		}
		return new SemanticQueryBuilder<>( Object[].class, () -> false, factory );
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
}
