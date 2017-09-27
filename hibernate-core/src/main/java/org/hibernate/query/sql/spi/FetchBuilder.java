/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.spi;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NativeQuery;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;

/**
 * @author Steve Ebersole
 */
public class FetchBuilder implements NativeQuery.FetchReturn {
	private final String tableAlias;
	private final String parentTableAlias;
	private final String joinPropertyName;

	private LockMode lockMode;

	private Map<String, AttributeMapping> attributeMappingsByName;

	public FetchBuilder(String tableAlias, String parentTableAlias, String joinPropertyName) {
		this.tableAlias = tableAlias;
		this.parentTableAlias = parentTableAlias;
		this.joinPropertyName = joinPropertyName;
	}

	public FetchBuilder(
			String tableAlias,
			String parentTableAlias,
			String joinPropertyName,
			LockMode lockMode) {
		this.tableAlias = tableAlias;
		this.parentTableAlias = parentTableAlias;
		this.joinPropertyName = joinPropertyName;

		this.lockMode = lockMode;
	}

	public Fetch buildFetch(BuilderExecutionState builderExecutionState, NodeResolutionContext resolutionContext) {
		final FetchParent fetchParent = builderExecutionState.getFetchParentByParentAlias( parentTableAlias );
		if ( fetchParent == null ) {
			throw new HibernateException( "FetchParent for table-alias [" + parentTableAlias + "] not yet resolved" );
		}

		// todo (6.0) : how to handle `SqlExpressableQualifier`?
		//		we need such a qualifier here to build EntitySqlSelectionMappings
		//
		//		also - who is responsible for generating it?  it should be "consistent" - either
		//		we generate them in the `ResultSetMappingNode` ctors or we generate them
		//		first and pass them in to the `ResultSetMappingNode` ctors.
		//
		//		given a `FetchParent`, need a way to find/create the `TableGroup` for the fetched
		// 		`Navigable`. that provides the `SqlExpressableQualifier` and fit into the SQL AST
		// 		being built.

		final Navigable joinedNavigable = fetchParent.getFetchContainer().findNavigable( joinPropertyName );
		if ( joinedNavigable == null ) {
			throw new HibernateException(
					String.format(
							Locale.ROOT,
							"Could not locate attribute/navigable for given name join name [%s] relative to container [%s (%s)]",
							joinPropertyName,
							fetchParent.getFetchContainer().asLoggableText(),
							parentTableAlias
					)

			);
		}

		assert joinedNavigable instanceof Fetchable;

		final ColumnReferenceQualifier qualifier = null;


		// todo (6.0) : pass along LockMode.  Anything else?
		final Fetch fetch = ( (Fetchable) joinedNavigable ).generateFetch(
				fetchParent,
				// assume its present in the results since it is explicitly defined
				qualifier,
				FetchStrategy.IMMEDIATE_JOIN,
				tableAlias,
				resolutionContext
		);

		fetchParent.addFetch( fetch );

		return fetch;
	}

	@Override
	public NativeQuery.FetchReturn setLockMode(LockMode lockMode) {
		this.lockMode = lockMode;
		return this;
	}

	@Override
	public FetchBuilder addProperty(String propertyName, String columnAlias) {
		AttributeMapping attributeMapping = addProperty( propertyName );
		attributeMapping.addColumnAlias( columnAlias );
		return this;
	}

	@Override
	public AttributeMapping addProperty(String propertyName) {
		AttributeMapping attributeMapping = null;

		if ( attributeMappingsByName == null ) {
			attributeMappingsByName = new HashMap<>();
		}
		else {
			attributeMapping = attributeMappingsByName.get( propertyName );
		}

		if ( attributeMapping == null ) {
			attributeMapping = new AttributeMapping( propertyName );
			attributeMappingsByName.put( propertyName, attributeMapping );
		}

		return attributeMapping;
	}
}
