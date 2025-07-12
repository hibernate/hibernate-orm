/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.hql.internal;

import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.hql.spi.DotIdentifierConsumer;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmEnumLiteral;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmFieldLiteral;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEmbeddableType;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEntityType;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddableDomainType;
import org.hibernate.query.sqm.tree.domain.SqmEntityDomainType;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.query.hql.HqlLogging.QUERY_LOGGER;

/**
 * A {@link DotIdentifierConsumer} used to interpret paths outside any
 * specific context. This is the handler used at the root of the handler
 * stack.
 * <p>
 * It can recognize any number of types of paths:
 * <ul>
 * <li>fully-qualified class names (entity or otherwise)
 * <li>static field references, e.g. {@code MyClass.SOME_FIELD}
 * <li>enum value references, e.g. {@code Sex.MALE}
 * <li>navigable-path
 * </ul>
 *
 * @author Steve Ebersole
 */
public class BasicDotIdentifierConsumer implements DotIdentifierConsumer {
	private final SqmCreationState creationState;

	private final StringBuilder pathSoFar = new StringBuilder();
	private SemanticPathPart currentPart;

	public BasicDotIdentifierConsumer(SqmCreationState creationState) {
		this.creationState = creationState;
	}

	public BasicDotIdentifierConsumer(SemanticPathPart initialState, SqmCreationState creationState) {
		this.currentPart = initialState;
		this.creationState = creationState;
	}

	protected SqmCreationState getCreationState() {
		return creationState;
	}

	@Override
	public SemanticPathPart getConsumedPart() {
		return currentPart;
	}

	@Override
	public void consumeIdentifier(String identifier, boolean isBase, boolean isTerminal) {
		if ( isBase ) {
			// each time we start a new sequence we need to reset our state
			reset();
		}

		if ( !pathSoFar.isEmpty() ) {
			pathSoFar.append( '.' );
		}
		pathSoFar.append( identifier );

//		QUERY_LOGGER.tracef(
//				"BasicDotIdentifierHandler#consumeIdentifier( %s, %s, %s ) - %s",
//				identifier,
//				isBase,
//				isTerminal,
//				pathSoFar
//		);

		currentPart = currentPart.resolvePathPart( identifier, isTerminal, creationState );
	}

	@Override
	public void consumeTreat(String importableName, boolean isTerminal) {
		final SqmPath<?> sqmPath = (SqmPath<?>) currentPart;
		currentPart = sqmPath.treatAs( treatTarget( importableName ) );
	}

	private <T> Class<T> treatTarget(String typeName) {
		final ManagedDomainType<T> managedType =
				creationState.getCreationContext().getJpaMetamodel()
						.managedType( typeName );
		return managedType.getJavaType();
	}

	protected void reset() {
		pathSoFar.setLength( 0 );
		currentPart = createBasePart();
	}

	protected SemanticPathPart createBasePart() {
		return new BaseLocalSequencePart();
	}

	public class BaseLocalSequencePart implements SemanticPathPart {
		private boolean isBase = true;

		@Override
		public SemanticPathPart resolvePathPart(
				String identifier,
				boolean isTerminal,
				SqmCreationState creationState) {
//			QUERY_LOGGER.tracef(
//					"BaseLocalSequencePart#consumeIdentifier( %s, %s, %s ) - %s",
//					identifier,
//					isBase,
//					isTerminal,
//					pathSoFar
//			);

			if ( isBase ) {
				isBase = false;
				final SemanticPathPart pathPart =
						resolvePath( identifier, isTerminal, creationState );
				if ( pathPart != null ) {
					return pathPart;
				}
			}

			// Below this point we wait to resolve the sequence until we hit the terminal.
			// We could check for "intermediate resolution", but that comes with a performance hit.
			// Consider:
			//
			//		org.hibernate.test.Sex.MALE
			//
			// We could check 'org', then 'org.hibernate', then 'org.hibernate.test' and so on until
			// we know it's a package, class or entity name. That's more expensive though, and the
			// error message would not be better.

			return isTerminal ? resolveTerminal( creationState ) : this;
		}

		private SemanticPathPart resolveTerminal(SqmCreationState creationState) {
			final SqmCreationContext creationContext = creationState.getCreationContext();
			final NodeBuilder nodeBuilder = creationContext.getNodeBuilder();
			final JpaMetamodel jpaMetamodel = creationContext.getJpaMetamodel();
			final QueryEngine queryEngine = creationContext.getQueryEngine();

			final SemanticPathPart literalType =
					resolveLiteralType( jpaMetamodel, nodeBuilder );
			if ( literalType != null ) {
				return literalType;
			}

			final SqmFunctionDescriptor functionDescriptor =
					resolveFunction( queryEngine );
			if ( functionDescriptor != null ) {
				return functionDescriptor.generateSqmExpression( null, queryEngine );
			}

			final SemanticPathPart literalJava =
					resolveLiteralJavaElement( jpaMetamodel, nodeBuilder );
			if ( literalJava != null ) {
				return literalJava;
			}

			throw new SemanticException( "Could not interpret path expression '" + pathSoFar + "'" );
		}

		private SemanticPathPart resolvePath(String identifier, boolean isTerminal, SqmCreationState creationState) {
			final SqmPathRegistry sqmPathRegistry =
					creationState.getProcessingStateStack().getCurrent()
							.getPathRegistry();

			final SqmFrom<?,?> pathRootByAlias =
					sqmPathRegistry.findFromByAlias( identifier, true );
			if ( pathRootByAlias != null ) {
				// identifier is an alias (identification variable)
				validateAsRoot( pathRootByAlias );
				return isTerminal ? pathRootByAlias : new DomainPathPart( pathRootByAlias );
			}

			final SqmFrom<?, ?> pathRootByExposedNavigable =
					sqmPathRegistry.findFromExposing( identifier );
			if ( pathRootByExposedNavigable != null ) {
				// identifier is an "unqualified attribute reference"
				validateAsRoot( pathRootByExposedNavigable );
				final SqmPath<?> sqmPath =
						pathRootByExposedNavigable.get( identifier, true );
				return isTerminal ? sqmPath : new DomainPathPart( sqmPath );
			}

			return null;
		}

		private SqmFunctionDescriptor resolveFunction(QueryEngine queryEngine) {
			return queryEngine.getSqmFunctionRegistry().findFunctionDescriptor( pathSoFar.toString() );
		}

		private SemanticPathPart resolveLiteralType(JpaMetamodel jpaMetamodel, NodeBuilder nodeBuilder) {
			final String importableName = jpaMetamodel.qualifyImportableName( pathSoFar.toString() );
			if ( importableName == null ) {
				return null;
			}
			else {
				final ManagedDomainType<?> managedType = jpaMetamodel.managedType( importableName );
				if ( managedType instanceof SqmEntityDomainType<?> entityDomainType ) {
					return new SqmLiteralEntityType<>( entityDomainType, nodeBuilder );
				}
				else if ( managedType instanceof SqmEmbeddableDomainType<?> embeddableDomainType ) {
					return new SqmLiteralEmbeddableType<>( embeddableDomainType, nodeBuilder );
				}
				else {
					return null;
				}
			}
		}

		private SemanticPathPart resolveLiteralJavaElement(JpaMetamodel metamodel, NodeBuilder nodeBuilder) {
			final String path = pathSoFar.toString();
			// see if it is a named field/enum reference
			final int splitPosition = path.lastIndexOf( '.' );
			if ( splitPosition > 0 ) {
				final String prefix = path.substring( 0, splitPosition );
				final String terminal = path.substring( splitPosition + 1 );
				try {
					final EnumJavaType<?> enumType = metamodel.getEnumType( prefix );
					if ( enumType != null ) {
						return sqmEnumLiteral( metamodel, enumType, terminal, nodeBuilder );
					}

					final JavaType<?> fieldJtdTest = metamodel.getJavaConstantType( prefix, terminal );
					if ( fieldJtdTest != null ) {
						return sqmFieldLiteral( metamodel, prefix, terminal, fieldJtdTest, nodeBuilder );
					}
				}
				catch (Exception ignore) {
				}
			}
			return null;
		}

		private static <E> SqmFieldLiteral<E> sqmFieldLiteral(
				JpaMetamodel jpaMetamodel,
				String prefix,
				String terminal,
				JavaType<E> fieldJtdTest,
				NodeBuilder nodeBuilder) {
			return new SqmFieldLiteral<>(
					jpaMetamodel.getJavaConstant( prefix, terminal ),
					fieldJtdTest,
					terminal,
					nodeBuilder
			);
		}

		private static <E extends Enum<E>> SqmEnumLiteral<E> sqmEnumLiteral(
				JpaMetamodel jpaMetamodel,
				EnumJavaType<E> enumType,
				String terminal,
				NodeBuilder nodeBuilder) {
			return new SqmEnumLiteral<>(
					jpaMetamodel.enumValue( enumType, terminal ),
					enumType,
					terminal,
					nodeBuilder
			);
		}

		protected void validateAsRoot(SqmFrom<?, ?> pathRoot) {
		}

		@Override
		public SqmPath<?> resolveIndexedAccess(
				SqmExpression<?> selector,
				boolean isTerminal,
				SqmCreationState processingState) {
			return currentPart.resolveIndexedAccess( selector, isTerminal, processingState );
		}
	}
}
