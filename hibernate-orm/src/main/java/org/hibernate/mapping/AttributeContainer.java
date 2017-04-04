/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

/**
 * Defines an additional contract for PersistentClass/Join in terms of being able to
 * contain attributes (Property).
 * <p/>
 * NOTE : this unifying contract is only used atm from HBM binding and so only defines the
 * needs of that use-case.
 *
 * @author Steve Ebersole
 */
public interface AttributeContainer {
	void addProperty(Property attribute);
}
