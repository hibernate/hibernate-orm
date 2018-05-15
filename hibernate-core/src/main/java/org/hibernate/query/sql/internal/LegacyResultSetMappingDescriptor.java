/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.internal;

import java.util.List;
import java.util.Map;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.query.NativeQuery;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.results.spi.DynamicInstantiationResult;
import org.hibernate.sql.results.spi.EntityResult;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.CollectionResult;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.ResultSetMapping;
import org.hibernate.sql.results.spi.ResultSetMappingDescriptor;
import org.hibernate.sql.results.spi.ScalarResult;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * ResultSetMappingDescriptor implementation implementing support
 * for Hibernate's native legacy support for defining the mapping
 * dynamically via {@link org.hibernate.query.NativeQuery#addScalar},
 * {@link org.hibernate.query.NativeQuery#addRoot},
 * {@link org.hibernate.query.NativeQuery#addEntity},
 * ect
 *
 * @author Steve Ebersole
 */
public class LegacyResultSetMappingDescriptor implements ResultSetMappingDescriptor {
	private final TypeConfiguration typeConfiguration;

	private TableReference scalarTableReference;

	private List<ResultRootNode> roots;
	private List<ResultFetchNode> fetches;
	private Map<String,FetchParentNode> fetchParentsByAlias;

	public LegacyResultSetMappingDescriptor(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
	}

	public RootNodeScalar makeScalarRoot(String alias, JavaTypeDescriptor javaTypeDescriptor) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	public RootNodeDynamicInstantiation makeDynamicInstantiationRoot(String instantiationTarget) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	public RootNodeEntity makeEntityRoot(String entityName, String alias) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	public RootNodeCollection makeCollectionRoot(String collectionRole, String alias) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	public ResultFetchNode makeFetch(String parentAlias, String relativePath, String fetchAlias) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ResultSetMappingDescriptor


	@Override
	public ResultSetMapping resolve(
			JdbcValuesMetadata jdbcResultsMetadata, SessionFactoryImplementor sessionFactory) {
		if ( roots == null || roots.isEmpty() ) {

		}

		throw new NotYetImplementedFor6Exception( getClass() );
//		final int columnCount = jdbcResultsMetadata.getColumnCount();
//		final List<SqlSelection> sqlSelections = CollectionHelper.arrayList( columnCount );
//
//		for ( int i = 0; i < jdbcResultsMetadata.getColumnCount(); i++ ) {
//
//		}
//
//		if ( roots == null || roots.isEmpty() ) {
//
//		}
//		final List<QueryResult> queryResults;
//		for ( ResultRootNode root : roots ) {
//
//		}

	}

	/**
	 * Simple unification interface for all returns from the various `#addXYZ` methods .
	 * Allows control over the "shape" of that particular part of the fetch graph.
	 *
	 * Some GraphNodes can be query results, while others simply describe a part
	 * of one of the results, while still others define fetches.
	 */
	public interface ResultNode extends ColumnReferenceQualifier {
		JavaTypeDescriptor<?> getJavaTypeDescriptor();
	}

	/**
	 * Allows access to further control how properties within a root or join
	 * fetch are mapped back from the result set.   Generally used in composite
	 * value scenarios.
	 */
	public interface ReturnProperty extends ResultNode {
		/**
		 * Add a column alias to this property mapping.
		 *
		 * @param columnAlias The column alias.
		 */
		void addColumnAlias(String columnAlias);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Specializations

	public interface ScalarNode extends ResultNode {
	}

	public interface FetchParentNode extends ResultNode {
		String getAlias();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ResultRoot

	/**
	 * ResultNode which can be a query result
	 */
	public interface ResultRootNode extends ResultNode {
		DomainResult makeQueryResult();
	}

	public interface RootNodeScalar extends ResultRootNode, ScalarNode {
		@Override
		ScalarResult makeQueryResult();
	}

	public interface RootNodeDynamicInstantiation extends ResultRootNode {
		@Override
		DynamicInstantiationResult makeQueryResult();
	}

	public interface RootNodeEntity extends ResultRootNode, FetchParentNode, NativeQuery.RootReturn {
		EntityTypeDescriptor<?> getEntityDescriptor();

		@Override
		default EntityJavaDescriptor<?> getJavaTypeDescriptor() {
			return getEntityDescriptor().getJavaTypeDescriptor();
		}

		@Override
		EntityResult makeQueryResult();
	}

	public interface RootNodeCollection extends ResultRootNode, FetchParentNode {
		PersistentCollectionDescriptor getCollectionDescriptor();

		@Override
		default JavaTypeDescriptor<?> getJavaTypeDescriptor() {
			return getCollectionDescriptor().getJavaTypeDescriptor();
		}

		@Override
		CollectionResult makeQueryResult();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// FetchResult

	public interface ResultFetchNode extends ResultNode, FetchParentNode, NativeQuery.FetchReturn {
		Fetch makeFetch(FetchParent parent);
	}
}
