/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

/**
 * Identifies a mapping model object which may have {@linkplain Property attributes} (fields or properties).
 * Abstracts over {@link PersistentClass} and {@link Join}.
 *
 * @author Steve Ebersole
 */
//NOTE: this unifying contract is currently only used from HBM binding and so only defines the needs of that use case.
public interface AttributeContainer {
	void addProperty(Property attribute);
}
