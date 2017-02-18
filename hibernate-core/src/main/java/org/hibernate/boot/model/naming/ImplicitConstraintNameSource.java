/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.naming;

import java.util.List;

/**
 * Common implicit name source traits for all constraint naming: FK, UK, index
 *
 * @author Steve Ebersole
 */
public interface ImplicitConstraintNameSource extends ImplicitNameSource {
	public Identifier getTableName();
	public List<Identifier> getColumnNames();
	public Identifier getUserProvidedIdentifier();
}
