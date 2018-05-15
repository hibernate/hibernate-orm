/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.domain;

/**
 * Contract for "mapped superclass" java type mappings which is a direct correlary to the JPA
 * term "mapped superclass".
 *
 * @author Chris Cranford
 */
public interface MappedSuperclassJavaTypeMapping<T> extends IdentifiableJavaTypeMapping<T> {
}
