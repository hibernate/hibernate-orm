/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.query.sql.spi.QueryResultBuilder;

/**
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
public class ResultSetMappingDescriptor {

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
	//
	// temporary so I can call externally
	public interface QueryResultDefinition {
		// what args?
		QueryResultBuilder resolve();
	}

	public List<QueryResultDefinition> getQueryResultDefinitions() {
		return Collections.emptyList();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private final String name;

	private List<QueryResultBuilder> resultBuilders;

	/**
	 * Constructs a ResultSetMappingDefinition with name
	 */
	public ResultSetMappingDescriptor(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	/**
	 * Adds a return.
	 *
	 * @param queryReturn The return
	 */
	public void addResultBuilder(QueryResultBuilder queryReturn) {
		if ( resultBuilders == null ) {
			resultBuilders = new ArrayList<>();
		}
		resultBuilders.add( queryReturn );
	}

	public List<QueryResultBuilder> getResultBuilders() {
		return resultBuilders == null
				? Collections.emptyList()
				: Collections.unmodifiableList( resultBuilders );
	}

}
