/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

/**
 * Optional contract implemented by some subtypes of {@link PersistentClass}.
 * <p>
 * Differentiates entity types that map to their own table ({@link RootClass},
 * {@link UnionSubclass}, and {@link JoinedSubclass}) from those which do not
 * ({@link SingleTableSubclass}).
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public interface TableOwner {
	void setTable(Table table);
}
