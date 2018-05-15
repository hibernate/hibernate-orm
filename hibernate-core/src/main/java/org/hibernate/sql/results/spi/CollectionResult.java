/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

/**
 * Models the a persistent collection as root {@link DomainResult}.  Pertinent to collection initializers only.
 *
 * @author Steve Ebersole
 */
public interface CollectionResult extends CollectionMappingNode, DomainResult {

	// todo (6.0) : do we want to define a `org.hibernate.type.descriptor.java.spi.CollectionJavaType`
	// 		like we do with `org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor`, e.g.
}
