/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.util.List;

import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;

/**
 * Used in {@link EntityInitializer} implementations
 *
 * @author Steve Ebersole
 */
public interface EntitySqlSelectionMappings {
	SqlSelection getRowIdSqlSelection();

	// todo (6.0) : as discussed elsewhere, drop SqlSelectionGroup and just use List<SqlSelection>

	List<SqlSelection> getIdSqlSelectionGroup();

	SqlSelection getDiscriminatorSqlSelection();

	SqlSelection getTenantDiscriminatorSqlSelection();

	List<SqlSelection> getAttributeSqlSelectionGroup(PersistentAttribute attribute);
}
