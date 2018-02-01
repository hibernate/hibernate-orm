/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal;

import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.query.sqm.StrictJpaComplianceViolation;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReference;
import org.hibernate.query.sqm.tree.from.SqmFromExporter;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.produce.spi.AliasRegistry;
import org.hibernate.query.sqm.produce.spi.ImplicitAliasGenerator;
import org.hibernate.query.sqm.produce.spi.ParsingContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmRoot;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class FromElementBuilder {
	// todo : I am pretty sure the uses of `{LHS-from-element}.getContainingSpace()` is incorrect when building SqmFrom elements below
	//		instead we should be passing along the FromElementSpace to use.  the big scenario I can
	//		think of is correlated sub-queries where the "LHS" is actually part of the outer query - aside
	// 		from hoisting that is not the FromElementSpace we should be using.

	// todo : make AliasRegistry part of QuerySpecProcessingState - pass that reference in here too
	//		but its odd to externally get the AliasRegistry from the FromElementBuilder when
	//		we are dealing with result-variables (selection aliases)

	private static final Logger log = Logger.getLogger( FromElementBuilder.class );

	private final ParsingContext parsingContext;
	private final AliasRegistry aliasRegistry;

	public FromElementBuilder(ParsingContext parsingContext, AliasRegistry aliasRegistry) {
		this.parsingContext = parsingContext;
		this.aliasRegistry = aliasRegistry;
	}

	public AliasRegistry getAliasRegistry(){
		return aliasRegistry;
	}

	/**
	 * Make the root entity reference for the FromElementSpace
	 */
	public SqmRoot makeRootEntityFromElement(
			SqmFromElementSpace fromElementSpace,
			EntityValuedExpressableType entityBinding,
			String alias) {
		if ( alias == null ) {
			alias = parsingContext.getImplicitAliasGenerator().buildUniqueImplicitAlias();
			log.debugf(
					"Generated implicit alias [%s] for root entity reference [%s]",
					alias,
					entityBinding.getEntityName()
			);
		}
		final SqmRoot root = new SqmRoot(
				fromElementSpace,
				parsingContext.makeUniqueIdentifier(),
				alias,
				entityBinding.getEntityDescriptor()
		);
		fromElementSpace.setRoot( root );
		parsingContext.registerFromElementByUniqueId( root );
		registerAlias( root );
		return root;
	}


	/**
	 * Make the root entity reference for the FromElementSpace
	 */
	public SqmCrossJoin makeCrossJoinedFromElement(
			SqmFromElementSpace fromElementSpace,
			String uid,
			EntityValuedExpressableType entityToJoin,
			String alias) {
		if ( alias == null ) {
			alias = parsingContext.getImplicitAliasGenerator().buildUniqueImplicitAlias();
			log.debugf(
					"Generated implicit alias [%s] for cross joined entity reference [%s]",
					alias,
					entityToJoin.getEntityName()
			);
		}

		final SqmCrossJoin join = new SqmCrossJoin(
				fromElementSpace,
				uid,
				alias,
				entityToJoin.getEntityDescriptor()
		);
		fromElementSpace.addJoin( join );
		parsingContext.registerFromElementByUniqueId( join );
		registerAlias( join );
		return join;
	}

	public SqmEntityJoin buildEntityJoin(
			SqmFromElementSpace fromElementSpace,
			String alias,
			EntityValuedExpressableType entityToJoin,
			SqmJoinType joinType) {
		if ( alias == null ) {
			alias = parsingContext.getImplicitAliasGenerator().buildUniqueImplicitAlias();
			log.debugf(
					"Generated implicit alias [%s] for entity join [%s]",
					alias,
					entityToJoin.getEntityName()
			);
		}

		final SqmEntityJoin join = new SqmEntityJoin(
				fromElementSpace,
				parsingContext.makeUniqueIdentifier(),
				alias,
				entityToJoin.getEntityDescriptor(),
				joinType
		);
		fromElementSpace.addJoin( join );
		parsingContext.registerFromElementByUniqueId( join );
		registerAlias( join );
		return join;
	}

	public SqmNavigableJoin buildNavigableJoin(
			SqmNavigableReference navigableReference,
			String alias,
			EntityValuedExpressableType subclassIndicator,
			SqmJoinType joinType,
			boolean fetched,
			boolean canReuseImplicitJoins) {
		log.tracef( getClass().getSimpleName() + "#buildNavigableJoin : " + navigableReference );

		assert navigableReference != null;
		assert joinType != null;
		assert navigableReference.getSourceReference() != null;

		if ( parsingContext.getSessionFactory().getSessionFactoryOptions().isStrictJpaQueryLanguageCompliance() ) {
			if ( !ImplicitAliasGenerator.isImplicitAlias( alias ) ) {
				if ( SqmSingularAttributeReference.class.isInstance( navigableReference )
						&& SqmFromExporter.class.isInstance( navigableReference ) ) {
					if ( fetched ) {
						throw new StrictJpaComplianceViolation(
								"Encountered aliased fetch join, but strict JPQL compliance was requested",
								StrictJpaComplianceViolation.Type.ALIASED_FETCH_JOIN
						);
					}
				}
			}
		}

		boolean reuseImplicitJoins = canReuseImplicitJoins;

		if ( reuseImplicitJoins ) {
			if ( fetched ) {
				log.debugf( "Bypassing implicit join re-use due to reference being fetched : " + navigableReference.getNavigablePath().getFullPath() );
				reuseImplicitJoins = false;
			}
			if ( !ImplicitAliasGenerator.isImplicitAlias( alias ) ) {
				log.debugf( "Bypassing implicit join re-use due to reference being aliased (" + alias + ") : " + navigableReference
						.getNavigablePath().getFullPath() );
				reuseImplicitJoins = false;
			}
		}

		if ( alias == null ) {
			alias = parsingContext.getImplicitAliasGenerator().buildUniqueImplicitAlias();
			log.debugf(
					"Generated implicit alias [%s] for attribute join [%s.%s]",
					alias,
					navigableReference.getSourceReference().getExportedFromElement().getIdentificationVariable(),
					navigableReference.getReferencedNavigable().getNavigableName()
			);
		}

		SqmNavigableJoin join = null;
		SqmNavigableReference cachedNavigableReference = null;
		if ( reuseImplicitJoins ) {
			cachedNavigableReference = parsingContext.getCachedNavigableBinding(
					navigableReference.getSourceReference(),
					navigableReference.getReferencedNavigable()
			);
			if ( cachedNavigableReference != null ) {
				join = (SqmNavigableJoin) cachedNavigableReference.getExportedFromElement();
				log.debugf( "Found re-usable join [%s] : %s", cachedNavigableReference, join );
			}
		}

		final EntityDescriptor indicatedSubclassDescriptor = subclassIndicator == null
				? null
				: subclassIndicator.getEntityDescriptor();

		if ( join == null ) {
			join = new SqmNavigableJoin(
					navigableReference.getSourceReference().getExportedFromElement(),
					navigableReference,
					parsingContext.makeUniqueIdentifier(),
					alias,
					indicatedSubclassDescriptor,
					joinType,
					fetched
			);

			if ( reuseImplicitJoins ) {
				parsingContext.cacheNavigableBinding( navigableReference );
				if ( cachedNavigableReference != null ) {
					cachedNavigableReference.injectExportedFromElement( join );
				}
			}

			parsingContext.registerFromElementByUniqueId( join );
			registerAlias( join );

			if ( !EmbeddedValuedNavigable.class.isInstance( navigableReference.getReferencedNavigable() ) ) {
				// it's a composite-valued navigable, create a join but do not register it
				//		as

				// unless this is a collection element or index...

				navigableReference.getSourceReference().getExportedFromElement().getContainingSpace().addJoin( join );
			}
		}
		else {
			navigableReference.injectExportedFromElement( join );
		}

		return join;
	}

	private void registerAlias(SqmFrom sqmFrom) {
		final String alias = sqmFrom.getIdentificationVariable();

		if ( alias == null ) {
			throw new ParsingException( "FromElement alias was null" );
		}

		if ( ImplicitAliasGenerator.isImplicitAlias( alias ) ) {
			log.debug( "Alias registration for implicit FromElement alias : " + alias );
		}

		aliasRegistry.registerAlias( sqmFrom.getNavigableReference() );
	}
}
