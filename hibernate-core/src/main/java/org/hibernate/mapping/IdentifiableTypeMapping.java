/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.mapping;

/**
 * @author Steve Ebersole
 * @author Chris Cranford
 */
public interface IdentifiableTypeMapping extends ManagedTypeMapping {

	Property getDeclaredIdentifierProperty();

	Property getIdentifierProperty();

	Component getIdentifierMapper();

	Component getDeclaredIdentifierMapper();

	java.util.List<Property> getIdClassProperties();

	boolean hasIdentifierProperty();

	boolean hasIdentifierMapper();

	boolean hasEmbeddedIdentifier();

	Property getDeclaredVersion();

	Property getVersion();

	boolean isVersioned();
}
