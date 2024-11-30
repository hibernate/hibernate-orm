/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.hql.internal;

import java.util.Locale;
import java.util.Set;

import org.hibernate.grammars.hql.HqlParser;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.sqm.SqmTreeCreationLogger;
import org.hibernate.query.sqm.StrictJpaComplianceViolation;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Helper for dealing with SQM tree creation
 *
 * @author Steve Ebersole
 */
public class SqmTreeCreationHelper {

	// The list is from the spec section 4.4.1
	private static final Set<String> RESERVED_WORDS = Set.of(
			"abs",
			"all",
			"and",
			"any",
			"as",
			"asc",
			"avg",
			"between",
			"bit_length",
			"both",
			"by",
			"case",
			"ceiling",
			"char_length",
			"character_length",
			"class",
			"coalesce",
			"concat",
			"count",
			"current_date",
			"current_time",
			"current_timestamp",
			"delete",
			"desc",
			"distinct",
			"else",
			"empty",
			"end",
			"entry",
			"escape",
			"exists",
			"exp",
			"extract",
			"false",
			"fetch",
			"first",
			"floor",
			"from",
			"function",
			"group",
			"having",
			"in",
			"index",
			"inner",
			"is",
			"join",
			"key",
			"leading",
			"last",
			"left",
			"length",
			"like",
			"local",
			"ln",
			"locate",
			"lower",
			"max",
			"member",
			"min",
			"mod",
			"new",
			"not",
			"null",
			"nulls",
			"nullif",
			"object",
			"of",
			"on",
			"or",
			"order",
			"outer",
			"position",
			"power",
			"replace",
			"right",
			"round",
			"select",
			"set",
			"sign",
			"size",
			"some",
			"sqrt",
			"substring",
			"sum",
			"then",
			"trailing",
			"treat",
			"trim",
			"true",
			"type",
			"unknown",
			"update",
			"upper",
			"value",
			"when",
			"where"
	);

	/**
	 * Handle secondary query roots using cross-join semantics.
	 *
	 * @apiNote Used when {@linkplain JpaCompliance#isJpaQueryComplianceEnabled() JPA compliance} is enabled
	 */
	public static <E> void handleRootAsCrossJoin(
			HqlParser.EntityWithJoinsContext entityWithJoinsContext,
			SqmRoot<?> sqmPrimaryRoot,
			SemanticQueryBuilder<?> sqmBuilder) {
		final HqlParser.RootEntityContext fromRootContext = (HqlParser.RootEntityContext) entityWithJoinsContext.fromRoot();

		//noinspection unchecked
		final SqmRoot<E> sqmRoot = (SqmRoot<E>) fromRootContext.accept( sqmBuilder );
		SqmTreeCreationLogger.LOGGER.debugf( "Handling secondary root path as cross-join - %s", sqmRoot.getEntityName() );

		final String alias = extractAlias( fromRootContext.variable(), sqmBuilder );
		final SqmEntityJoin<?,E> pseudoCrossJoin = new SqmEntityJoin<>(
				sqmRoot.getManagedType(),
				alias,
				SqmJoinType.CROSS,
				sqmPrimaryRoot
		);

		//noinspection unchecked,rawtypes
		sqmPrimaryRoot.addSqmJoin( (SqmEntityJoin) pseudoCrossJoin );

		final SqmCreationProcessingState processingState = sqmBuilder.getProcessingStateStack().getCurrent();
		final SqmPathRegistry pathRegistry = processingState.getPathRegistry();
		pathRegistry.replace( pseudoCrossJoin, sqmRoot );

		final int size = entityWithJoinsContext.getChildCount();
		for ( int i = 1; i < size; i++ ) {
			final ParseTree parseTree = entityWithJoinsContext.getChild( i );
			if ( parseTree instanceof HqlParser.CrossJoinContext ) {
				sqmBuilder.consumeCrossJoin( (HqlParser.CrossJoinContext) parseTree, sqmPrimaryRoot );
			}
			else if ( parseTree instanceof HqlParser.JoinContext ) {
				sqmBuilder.consumeJoin( (HqlParser.JoinContext) parseTree, sqmPrimaryRoot );
			}
			else if ( parseTree instanceof HqlParser.JpaCollectionJoinContext ) {
				sqmBuilder.consumeJpaCollectionJoin( (HqlParser.JpaCollectionJoinContext) parseTree, sqmPrimaryRoot );
			}
		}
	}

	/**
	 * Extracts an alias ("identification variable")
	 *
	 * @return The specified alias, or {@code null}
	 */
	public static String extractAlias(HqlParser.VariableContext ctx, SemanticQueryBuilder<?> sqmBuilder) {
		return extractVariable( ctx, sqmBuilder );
	}

	/**
	 * Extracts an alias ("identification variable"), applying "JPA compliance" by lower-casing
	 *
	 * @return The specified alias, or {@code null}
	 */
	public static String extractJpaCompliantAlias(HqlParser.VariableContext ctx, SemanticQueryBuilder<?> sqmBuilder) {
		return applyJpaCompliance( extractVariable( ctx, sqmBuilder ), sqmBuilder );
	}

	/**
	 * Extracts an "identification variable" (alias)
	 *
	 * @return The specified alias, or {@code null}
	 */
	public static String extractVariable(HqlParser.VariableContext ctx, SemanticQueryBuilder<?> sqmBuilder) {
		if ( ctx == null ) {
			return null;
		}

		final ParseTree lastChild = ctx.getChild( ctx.getChildCount() - 1 );
		if ( lastChild instanceof HqlParser.IdentifierContext ) {
			final HqlParser.IdentifierContext identifierContext = (HqlParser.IdentifierContext) lastChild;
			// in this branch, the alias could be a reserved word ("keyword as identifier")
			// which JPA disallows...
			if ( sqmBuilder.getCreationOptions().useStrictJpaCompliance() ) {
				final Token identificationVariableToken = identifierContext.getStart();
				if ( RESERVED_WORDS.contains( identificationVariableToken.getText().toLowerCase( Locale.ENGLISH ) ) ) {
					throw new StrictJpaComplianceViolation(
							String.format(
									Locale.ROOT,
									"Strict JPQL compliance was violated : %s [%s]",
									StrictJpaComplianceViolation.Type.RESERVED_WORD_USED_AS_ALIAS.description(),
									identificationVariableToken.getText()
							),
							StrictJpaComplianceViolation.Type.RESERVED_WORD_USED_AS_ALIAS
					);
				}
			}
			return sqmBuilder.visitIdentifier( identifierContext );
		}
		else {
			final HqlParser.NakedIdentifierContext identifierContext = (HqlParser.NakedIdentifierContext) lastChild;
			// in this branch, the alias could be a reserved word ("keyword as identifier")
			// which JPA disallows...
			if ( sqmBuilder.getCreationOptions().useStrictJpaCompliance() ) {
				final Token identificationVariableToken = identifierContext.getStart();
				if ( RESERVED_WORDS.contains( identificationVariableToken.getText().toLowerCase( Locale.ENGLISH ) ) ) {
					throw new StrictJpaComplianceViolation(
							String.format(
									Locale.ROOT,
									"Strict JPQL compliance was violated : %s [%s]",
									StrictJpaComplianceViolation.Type.RESERVED_WORD_USED_AS_ALIAS.description(),
									identificationVariableToken.getText()
							),
							StrictJpaComplianceViolation.Type.RESERVED_WORD_USED_AS_ALIAS
					);
				}
			}
			return sqmBuilder.visitNakedIdentifier( identifierContext );
		}
	}

	/**
	 * Handle JPA requirement that variables (aliases) be case-insensitive
	 */
	public static String applyJpaCompliance(String text, SemanticQueryBuilder<?> sqmBuilder) {
		if ( text == null ) {
			return null;
		}

		if ( sqmBuilder.getCreationOptions().useStrictJpaCompliance() ) {
			return text.toLowerCase( Locale.getDefault() );
		}

		return text;
	}
}
