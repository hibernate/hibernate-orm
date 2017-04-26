/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal;

import org.hibernate.persister.queryable.spi.EntityValuedExpressableType;
import org.hibernate.query.sqm.domain.SqmExpressableTypeEmbedded;
import org.hibernate.query.sqm.domain.SqmExpressableTypeEntity;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.produce.spi.AliasRegistry;
import org.hibernate.query.sqm.produce.spi.ImplicitAliasGenerator;
import org.hibernate.query.sqm.produce.spi.ParsingContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.expression.domain.SqmAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
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
				entityBinding
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
				entityToJoin
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
				entityToJoin,
				joinType
		);
		fromElementSpace.addJoin( join );
		parsingContext.registerFromElementByUniqueId( join );
		registerAlias( join );
		return join;
	}

	public SqmAttributeJoin buildAttributeJoin(
			SqmAttributeReference attributeBinding,
			String alias,
			EntityValuedExpressableType subclassIndicator,
			SqmJoinType joinType,
			boolean fetched,
			boolean canReuseImplicitJoins) {
		assert attributeBinding != null;
		assert joinType != null;
		assert attributeBinding.getSourceReference() != null;

		if ( fetched && canReuseImplicitJoins ) {
			throw new ParsingException( "Illegal combination of [fetched] and [canReuseImplicitJoins=true] passed to #buildAttributeJoin" );
		}

		if ( alias != null && canReuseImplicitJoins ) {
			throw new ParsingException( "Unexpected combination of [non-null alias] and [canReuseImplicitJoins=true] passed to #buildAttributeJoin" );
		}

		// todo : validate alias & fetched?  JPA at least disallows specifying an alias for fetched associations

		if ( alias == null ) {
			alias = parsingContext.getImplicitAliasGenerator().buildUniqueImplicitAlias();
			log.debugf(
					"Generated implicit alias [%s] for attribute join [%s.%s]",
					alias,
					attributeBinding.getSourceReference().getExportedFromElement().getIdentificationVariable(),
					attributeBinding.getReferencedNavigable().getAttributeName()
			);
		}

		SqmAttributeJoin join = null;
		if ( canReuseImplicitJoins ) {
			final SqmNavigableReference navigableBinding = parsingContext.getCachedNavigableBinding( attributeBinding.getSourceReference(), attributeBinding.getReferencedNavigable() );
			join = (SqmAttributeJoin) NavigableBindingHelper.resolveExportedFromElement( navigableBinding );
		}

		if ( join == null ) {
			join = new SqmAttributeJoin(
					attributeBinding.getSourceReference().getExportedFromElement(),
					attributeBinding,
					parsingContext.makeUniqueIdentifier(),
					alias,
					subclassIndicator,
					joinType,
					fetched
			);

			if ( canReuseImplicitJoins ) {
				parsingContext.cacheNavigableBinding( attributeBinding );
			}

			parsingContext.registerFromElementByUniqueId( join );
			registerAlias( join );

			if ( !SqmExpressableTypeEmbedded.class.isInstance( attributeBinding.getReferencedNavigable() ) ) {
				// it's a composite-valued navigable, create a join but do not register it
				//		as

				// unless this is a collection element or index...

				attributeBinding.getSourceReference().getExportedFromElement().getContainingSpace().addJoin( join );
			}
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

		aliasRegistry.registerAlias( sqmFrom.getBinding() );
	}
}
