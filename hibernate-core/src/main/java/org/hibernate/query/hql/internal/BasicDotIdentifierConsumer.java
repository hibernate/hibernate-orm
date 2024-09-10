/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.hql.HqlLogging;
import org.hibernate.query.hql.spi.DotIdentifierConsumer;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmEnumLiteral;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmFieldLiteral;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEmbeddableType;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEntityType;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;

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

		HqlLogging.QUERY_LOGGER.tracef(
				"BasicDotIdentifierHandler#consumeIdentifier( %s, %s, %s ) - %s",
				identifier,
				isBase,
				isTerminal,
				pathSoFar
		);

		currentPart = currentPart.resolvePathPart( identifier, isTerminal, creationState );
	}

	@Override
	public void consumeTreat(String importableName, boolean isTerminal) {
		final SqmPath<?> sqmPath = (SqmPath<?>) currentPart;
		currentPart = sqmPath.treatAs( treatTarget( importableName ) );
	}

	private <T> Class<T> treatTarget(String typeName) {
		final ManagedDomainType<T> managedType = creationState.getCreationContext()
				.getJpaMetamodel()
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
			HqlLogging.QUERY_LOGGER.tracef(
					"BaseLocalSequencePart#consumeIdentifier( %s, %s, %s ) - %s",
					identifier,
					isBase,
					isTerminal,
					pathSoFar
			);

			if ( isBase ) {
				isBase = false;

				final SqmPathRegistry sqmPathRegistry = creationState.getProcessingStateStack()
						.getCurrent()
						.getPathRegistry();

				final SqmFrom<?,?> pathRootByAlias = sqmPathRegistry.findFromByAlias( identifier, true );
				if ( pathRootByAlias != null ) {
					// identifier is an alias (identification variable)
					validateAsRoot( pathRootByAlias );

					if ( isTerminal ) {
						return pathRootByAlias;
					}
					else {
						return new DomainPathPart( pathRootByAlias );
					}
				}

				final SqmFrom<?, ?> pathRootByExposedNavigable = sqmPathRegistry.findFromExposing( identifier );
				if ( pathRootByExposedNavigable != null ) {
					// identifier is an "unqualified attribute reference"
					validateAsRoot( pathRootByExposedNavigable );

					final SqmPath<?> sqmPath = pathRootByExposedNavigable.get( identifier );
					if ( isTerminal ) {
						return sqmPath;
					}
					else {
						return new DomainPathPart( sqmPath );
					}
				}
			}

			// at the moment, below this point we wait to resolve the sequence until we hit the terminal
			//
			// we could check for "intermediate resolution", but that comes with a performance hit.  E.g., consider
			//
			//		`org.hibernate.test.Sex.MALE`
			//
			// we could check `org` and then `org.hibernate` and then `org.hibernate.test` and then ... until
			// we know it is a package, class or entity name.  That gets expensive though.  For now, plan on
			// resolving these at the terminal
			//
			// todo (6.0) : finish this logic.  and see above note in `! isTerminal` block

			final SqmCreationContext creationContext = creationState.getCreationContext();

			if ( ! isTerminal ) {
				return this;
			}

			final String path = pathSoFar.toString();
			final JpaMetamodel jpaMetamodel = creationContext.getJpaMetamodel();
			final String importableName = jpaMetamodel.qualifyImportableName( path );
			final NodeBuilder nodeBuilder = creationContext.getNodeBuilder();
			if ( importableName != null ) {
				final ManagedDomainType<?> managedType = jpaMetamodel.managedType( importableName );
				if ( managedType instanceof EntityDomainType<?> entityDomainType ) {
					return new SqmLiteralEntityType<>( entityDomainType, nodeBuilder );
				}
				else if ( managedType instanceof EmbeddableDomainType<?> embeddableDomainType ) {
					return new SqmLiteralEmbeddableType<>( embeddableDomainType, nodeBuilder );
				}
			}

			final SqmFunctionDescriptor functionDescriptor =
					creationContext.getQueryEngine()
							.getSqmFunctionRegistry()
							.findFunctionDescriptor( path );
			if ( functionDescriptor != null ) {
				return functionDescriptor.generateSqmExpression(
						null,
						creationContext.getQueryEngine()
				);
			}

			// see if it is a named field/enum reference
			final int splitPosition = path.lastIndexOf( '.' );
			if ( splitPosition > 0 ) {
				final String prefix = path.substring( 0, splitPosition );
				final String terminal = path.substring( splitPosition + 1 );

				try {
					final EnumJavaType<?> enumType = jpaMetamodel.getEnumType( prefix );
					if ( enumType != null ) {
						return sqmEnumLiteral( jpaMetamodel, enumType, terminal, nodeBuilder );
					}

					final JavaType<?> fieldJtdTest = jpaMetamodel.getJavaConstantType( prefix, terminal );
					if ( fieldJtdTest != null ) {
						return sqmFieldLiteral( jpaMetamodel, prefix, terminal, fieldJtdTest, nodeBuilder );
					}
				}
				catch (Exception ignore) {
				}
			}

			throw new SemanticException( "Could not interpret path expression '" + path + "'" );
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
