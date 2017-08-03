/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.Map;

import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.sql.results.spi.SqlSelectionGroup;
import org.hibernate.sql.results.spi.CompositeSqlSelectionMappings;

/**
 * @author Steve Ebersole
 */
public class CompositeSqlSelectionMappingsImpl implements CompositeSqlSelectionMappings {
	private final Map<PersistentAttribute, SqlSelectionGroup> attributeSqlSelectionGroupMap;

	public CompositeSqlSelectionMappingsImpl(Map<PersistentAttribute, SqlSelectionGroup> attributeSqlSelectionGroupMap) {
		this.attributeSqlSelectionGroupMap = attributeSqlSelectionGroupMap;
	}

	@Override
	public SqlSelectionGroup getAttributeSqlSelectionGroup(PersistentAttribute attribute) {
		return attributeSqlSelectionGroupMap.get( attribute );
	}
}
