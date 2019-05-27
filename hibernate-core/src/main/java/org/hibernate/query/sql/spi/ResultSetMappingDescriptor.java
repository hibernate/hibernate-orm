/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.QueryResultBuilder;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;


/**
 * todo (6.0) : this will ultimately be a builder for DomainResultProducer instances
 * 		- though personally I hate the name DomainResultProducerBuilder.
 * 		- Another option here is to have this be the thing that acts as the DomainResultProducer
 * 			in cases where a ResultSet mapping is used.
 * 		- DomainResultProducer is the thing that is the uniform contract for creating
 * 			DomainResult instances from HQL, persisters, etc.  There
 *
 * Keep a description of the {@link javax.persistence.SqlResultSetMapping}
 *
 * Note that we do not track joins/fetches here as we do in the manual approach
 * as this feature is defined by JPA and we simply support the JPA feature here.
 *
 * Note also that we track the result builders here (as opposed to the
 * QueryResult) to better fit with {@link org.hibernate.query.NativeQuery}
 * which is where this is ultimately used.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public interface ResultSetMappingDescriptor {

	// todo (6.0) : delay resolution of these query result definitions until "later"
	//		ultimately we need access to a fully resolved TypeConfiguration
	//		to properly resolve these mapping definitions into QueryResult or
	// 		QueryResultBuilder instances.
	//
	//		the idea would be to keep references here as a type like `QueryResultDefinition`
	//		which we could resolve via a method - what args?
	//
	//		essentially the flow would be:
	//			1) `@SqlResultSetMapping` -> `ResultSetMappingDefinition` + `QueryResultDefinition`*
	//			2) `QueryResultDefinition` (for each) -> `QueryResultBuilder`
	//			3) `QueryResultBuilder`* -> `QueryResult`* -> `ResultSetMappingDescriptor`
	//			4) `ResultSetMappingDescriptor` -> `ResultSetMapping`
	//
	//		may seem a little convoluted, but each phase has a distinct reason:
	//			1) happens in the hbm/annotation binders as we build the boot model (and
	//				in fact we ought to consider moving `ResultSetMappingDefinition` into
	//				a boot model package).  we really cannot resolve TypeConfiguration
	//				information consistently at this point, which is kind of the whole
	//				point to this phase
	//			2) `QueryResultBuilder` is the thing used inside `NativeQuery` to
	// 				represent its results, which are potentially in-flight (hence its a
	//				builder).  These NativeQuery results can be defined either by a
	//				`@SqlResultSetMapping` or via the NativeQuery contract.  Either
	//				approach yields one or more `QueryResultBuilder` instances.
	//			3) Ultimately each of the `QueryResultBuilder`s is asked to build
	// 				its corresponding `QueryResult` which are collected together
	//				and used to create a `ResultSetMappingDescriptor`.
	//			4)  The main purpose of this phase is to allow the `ResultSetMapping`
	//				and `QueryResult` instances to "prepare" themselves which is needed
	//				in the case of NativeQuery to:
	//					a) determine the ResultSet position for all specified column aliases
	//					b) for scalar results, determine the proper sql/java types of reading them

	// todo (6.0) : ? have a specific impl for "consuming" JPA SqlResultSetMapping?
	//		there are a few different cases for defining result mappings:
	//			1) JPA SqlResultSetMapping
	//			2) JPA Class-based mapping
	//			3) Hibernate's legacy XML-defined mapping
	//			4) Hibernate's legacy Query-specific mapping (`NativeQuery#addScalar`, etc).
	//			5)
	//
	// (1), (2) and (3) can really all be handled by the same impl - they are all
	//		known/resolved up-front.  These cases can all be represented as
	//		`ResultSetMappingDescriptorDefined`
	//
	// (4) is unique though in that it is not know up
	// 		front and needs to wait until there is a ResultSet available to complete
	//		its "resolution".  This case is represented as `ResultSetMappingDescriptorUndefined`
	//
	// Both `ResultSetMappingDescriptorDefined` and `ResultSetMappingDescriptorUndefined` could
	// 		definitely use better names

	/**
	 * temporary so I can call externally
	 */
	public interface QueryResultDefinition {
		// what args?
		QueryResultBuilder resolve();
	}
	/**
	 * Access to information about the underlying JDBC values
	 * such as type, position, column name, etc
	 */
	interface JdbcValuesMetadata {
		/**
		 * Number of values in the underlying result
		 */
		int getColumnCount();

		/**
		 * Position of a particular result value by name
		 */
		int resolveColumnPosition(String columnName);

		/**
		 * Name of a particular result value by position
		 */
		String resolveColumnName(int position);

		/**
		 * Descriptor of the JDBC/SQL type of a particular result value by
		 * position
		 */
		SqlTypeDescriptor resolveSqlTypeDescriptor(int position);

	}

	/**
	 * Resolve the selections (both at the JDBC and object level) for this
	 * mapping.  Acts as delayed access to this resolution process to support
	 * "auto discovery" as needed for "undefined scalar" results as defined by
	 * JPA.
	 *
	 * @param jdbcResultsMetadata Access to information about the underlying results
	 * @param sessionFactory
	 * @return The resolved result references
	 */
	ResultSetMapping resolve(JdbcValuesMetadata jdbcResultsMetadata, SessionFactoryImplementor sessionFactory);
}
