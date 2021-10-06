/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
import org.hibernate.query.EntityIdentifierNavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.basic.BasicFetch;

/**
 * @author Steve Ebersole
 */
public class ResultsHelper {
	public static int jdbcPositionToValuesArrayPosition(int jdbcPosition) {
		return jdbcPosition - 1;
	}

	public static int valuesArrayPositionToJdbcPosition(int valuesArrayPosition) {
		return valuesArrayPosition + 1;
	}

	public static DomainResultCreationStateImpl impl(DomainResultCreationState creationState) {
		return unwrap( creationState );
	}

	private static DomainResultCreationStateImpl unwrap(DomainResultCreationState creationState) {
		if ( creationState instanceof DomainResultCreationStateImpl ) {
			return ( (DomainResultCreationStateImpl) creationState );
		}

		throw new IllegalArgumentException(
				"Passed DomainResultCreationState not an instance of org.hibernate.query.results.DomainResultCreationStateImpl"
		);
	}

	private ResultsHelper() {
	}

	public static boolean isIdentifier(EntityIdentifierMapping identifierDescriptor, String... names) {
		final String identifierAttributeName = identifierDescriptor instanceof SingleAttributeIdentifierMapping
				? ( (SingleAttributeIdentifierMapping) identifierDescriptor ).getAttributeName()
				: EntityIdentifierMapping.ROLE_LOCAL_NAME;

		for ( int i = 0; i < names.length; i++ ) {
			final String name = names[ i ];
			if ( EntityIdentifierMapping.ROLE_LOCAL_NAME.equals( name ) ) {
				return true;
			}

			if ( identifierAttributeName.equals( name ) ) {
				return true;
			}
		}

		return false;
	}

//	public static ResultMemento implicitIdentifierResult(
//			EntityIdentifierMapping identifierMapping,
//			EntityIdentifierNavigablePath idPath,
//			ResultSetMappingResolutionContext resolutionContext) {
//		return new ImplicitModelPartResultMemento( idPath, identifierMapping );
//	}

	public static DomainResult implicitIdentifierResult(
			EntityIdentifierMapping identifierMapping,
			EntityIdentifierNavigablePath idPath,
			DomainResultCreationState creationState) {
		final DomainResultCreationStateImpl creationStateImpl = impl( creationState );
		final TableGroup tableGroup = creationStateImpl.getFromClauseAccess().getTableGroup( idPath.getParent() );

		return identifierMapping.createDomainResult(
				idPath,
				tableGroup,
				null,
				creationState
		);
	}

	public static String attributeName(ModelPart identifierMapping) {
		if ( identifierMapping instanceof EntityIdentifierMapping ) {
			return identifierMapping instanceof SingleAttributeIdentifierMapping
					? ( (SingleAttributeIdentifierMapping) identifierMapping ).getAttributeName()
					: null;
		}
		else {
			return identifierMapping.getPartName();
		}

	}

	public static DomainResult convertIdFetchToResult(Fetch fetch, DomainResultCreationState creationState) {
		final EntityIdentifierMapping idMapping = (EntityIdentifierMapping) fetch.getFetchedMapping();
		if ( fetch instanceof BasicFetch ) {
			final BasicFetch<?> basicFetch = (BasicFetch<?>) fetch;

		}
		return null;
	}
}
