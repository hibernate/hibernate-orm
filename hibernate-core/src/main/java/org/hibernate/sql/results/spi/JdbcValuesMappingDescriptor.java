/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Descriptor for the mapping of a JDBC ResultSet providing
 * support for delayed resolution if needed (mainly in the
 * case of {@link org.hibernate.query.NativeQuery}).
 *
 * @author Steve Ebersole
 */
public interface JdbcValuesMappingDescriptor {

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
	 * Resolve the selections (both at the JDBC and object level) for this
	 * mapping.  Acts as delayed access to this resolution process to support
	 * "auto discovery" as needed for "undefined scalar" results as defined by
	 * JPA.
	 *
	 * @param jdbcResultsMetadata Access to information about the underlying results
	 * @param sessionFactory
	 * @return The resolved result references
	 */
	JdbcValuesMapping resolve(JdbcValuesMetadata jdbcResultsMetadata, SessionFactoryImplementor sessionFactory);
}
