/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal;

import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.query.sqm.StrictJpaComplianceViolation;
import org.hibernate.query.sqm.produce.spi.ImplicitAliasGenerator;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReference;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
import org.hibernate.query.sqm.tree.from.SqmFromExporter;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;

/**
 * @author Steve Ebersole
 */
public class SqmFromBuilderFromClauseQualifiedJoin extends AbstractSqmFromBuilderFromClause {
	private final SqmJoinType joinType;
	private final boolean fetched;

	public SqmFromBuilderFromClauseQualifiedJoin(
			SqmJoinType joinType,
			boolean fetched,
			String alias,
			SqmCreationContext sqmCreationContext) {
		super( alias, sqmCreationContext );
		this.joinType = joinType;
		this.fetched = fetched;
	}

	@Override
	public SqmEntityJoin buildEntityJoin(EntityValuedNavigable navigable) {
		final SqmFromElementSpace fromElementSpace = getSqmCreationContext().getCurrentFromElementSpace();
		final SqmEntityJoin join = new SqmEntityJoin(
				fromElementSpace,
				getSqmCreationContext().generateUniqueIdentifier(),
				getAlias(),
				navigable.getEntityDescriptor(),
				joinType,
				getSqmCreationContext()
		);

		fromElementSpace.addJoin( join );
		commonHandling( join );

		return join;
	}

	@Override
	public SqmNavigableJoin buildNavigableJoin(SqmNavigableReference navigableReference) {
		if ( getSqmCreationContext().getSessionFactory().getSessionFactoryOptions().isStrictJpaQueryLanguageCompliance() ) {
			if ( !ImplicitAliasGenerator.isImplicitAlias( getAlias() ) ) {
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

		final SqmNavigableJoin navigableJoin = new SqmNavigableJoin(
				navigableReference.getSourceReference().getExportedFromElement(),
				navigableReference,
				getSqmCreationContext().generateUniqueIdentifier(),
				getAlias(),
				joinType,
				fetched
		);

		getSqmCreationContext().getCurrentFromElementSpace().addJoin( navigableJoin );
		commonHandling( navigableJoin );

		if ( fetched ) {
			getSqmCreationContext().registerFetch( navigableReference.getSourceReference(), navigableJoin );
		}

		return navigableJoin;
	}
}
