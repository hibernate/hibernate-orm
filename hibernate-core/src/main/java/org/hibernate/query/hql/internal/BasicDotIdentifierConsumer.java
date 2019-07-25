/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import java.lang.reflect.Field;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.query.hql.spi.DotIdentifierConsumer;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmEnumLiteral;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmFieldLiteral;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.type.descriptor.java.EnumJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;

import org.jboss.logging.Logger;

/**
 * @asciidoc
 *
 * DotIdentifierHandler used to interpret paths outside of any specific
 * context.  This is the handler used at the root of the handler stack.
 *
 * It can recognize any number of types of paths -
 *
 * 		* fully-qualified class names (entity or otherwise)
 * 		* static field references, e.g. `MyClass.SOME_FIELD`
 * 		* enum value references, e.g. `Sex.MALE`
 * 		* navigable-path
 * 		* others?
 *
 * @author Steve Ebersole
 */
public class BasicDotIdentifierConsumer implements DotIdentifierConsumer {
	private static final Logger log = Logger.getLogger( BasicDotIdentifierConsumer.class );

	private final SqmCreationState creationState;

	private String pathSoFar;
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

		if ( pathSoFar == null ) {
			pathSoFar = identifier;
		}
		else {
			pathSoFar += ( '.' + identifier );
		}

		log.tracef(
				"BasicDotIdentifierHandler#consumeIdentifier( %s, %s, %s ) - %s",
				identifier,
				isBase,
				isTerminal,
				pathSoFar
		);

		currentPart = currentPart.resolvePathPart( identifier, isTerminal, creationState );
	}

	protected void reset() {
		pathSoFar = null;
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
			if ( isBase ) {
				isBase = false;

				final SqmPathRegistry sqmPathRegistry = creationState.getProcessingStateStack()
						.getCurrent()
						.getPathRegistry();

				final SqmFrom pathRootByAlias = sqmPathRegistry.findFromByAlias( identifier );
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

				final SqmFrom pathRootByExposedNavigable = sqmPathRegistry.findFromExposing( identifier );
				if ( pathRootByExposedNavigable != null ) {
					// identifier is an "unqualified attribute reference"
					validateAsRoot( pathRootByExposedNavigable );

					final SqmPathSource subPathSource = pathRootByExposedNavigable.getReferencedPathSource().findSubPathSource( identifier );
					final SqmPath sqmPath = subPathSource.createSqmPath( pathRootByExposedNavigable, creationState );
					if ( isTerminal ) {
						return sqmPath;
					}
					else {
						pathRootByExposedNavigable.registerImplicitJoinPath( sqmPath );
						return new DomainPathPart( pathRootByAlias );
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

			if ( !isTerminal ) {
				return this;
			}

//			// see if it is a Class name...
//			try {
//				final Class<?> namedClass = creationState.getCreationContext()
//						.getServiceRegistry()
//						.getService( ClassLoaderService.class )
//						.classForName( pathSoFar );
//				if ( namedClass != null ) {
//					return new
//				}
//			}
//			catch (Exception ignore) {
//			}

			// see if it is a named field/enum reference
			final int splitPosition = pathSoFar.lastIndexOf( '.' );
			if ( splitPosition > 0 ) {
				final String prefix = pathSoFar.substring( 0, splitPosition );
				final String terminal = pathSoFar.substring( splitPosition + 1 );

				try {
					final SqmCreationContext creationContext = creationState.getCreationContext();
					final Class<?> namedClass = creationContext
							.getServiceRegistry()
							.getService( ClassLoaderService.class )
							.classForName( prefix );
					if ( namedClass != null ) {
						final JavaTypeDescriptorRegistry javaTypeDescriptorRegistry = creationContext.getJpaMetamodel()
								.getTypeConfiguration()
								.getJavaTypeDescriptorRegistry();
						try {
							final Field referencedField = namedClass.getDeclaredField( terminal );
							if ( referencedField != null ) {
								final JavaTypeDescriptor<?> fieldJtd = javaTypeDescriptorRegistry
										.getDescriptor( referencedField.getType() );
								//noinspection unchecked
								return new SqmFieldLiteral( referencedField, fieldJtd, creationContext.getNodeBuilder() );
							}
						}
						catch (Exception ignore) {
						}

						if ( namedClass.isEnum() ) {
							//noinspection unchecked
							final Enum referencedEnum = Enum.valueOf( (Class) namedClass, terminal );
							//noinspection unchecked
							return new SqmEnumLiteral(
									referencedEnum,
									(EnumJavaTypeDescriptor) javaTypeDescriptorRegistry.resolveDescriptor( namedClass ),
									terminal,
									creationContext.getNodeBuilder()
							);
						}
					}
				}
				catch (Exception ignore) {
				}
			}

			throw new ParsingException( "Could not interpret dot-ident : " + pathSoFar );
		}

		protected void validateAsRoot(SqmFrom pathRoot) {

		}

		@Override
		public SqmPath resolveIndexedAccess(
				SqmExpression selector,
				boolean isTerminal,
				SqmCreationState processingState) {
			return currentPart.resolveIndexedAccess( selector, isTerminal, processingState );
		}
	}
}
