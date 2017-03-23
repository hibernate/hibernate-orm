/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.mapping;

/**
 * The common mapping model definition for any "managed type" (entity, mapped-superclass,
 * embeddable/embedded).
 *
 * @author Steve Ebersole
 */
public interface ManagedTypeMapping {
	ManagedTypeMapping getSuperclassMapping();
	java.util.List<ManagedTypeMapping> getSubclassMappings();

	java.util.List<Property> getDeclaredProperties();
}
