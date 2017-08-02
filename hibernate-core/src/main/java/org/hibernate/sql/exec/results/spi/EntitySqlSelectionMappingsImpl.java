/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.results.spi;

import java.util.Map;

import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.sql.exec.results.internal.EntitySqlSelectionMappings;

/**
 * @author Steve Ebersole
 */
public class EntitySqlSelectionMappingsImpl implements EntitySqlSelectionMappings {
	private final SqlSelection rowIdSqlSelection;
	private final SqlSelectionGroup idSqlSelectionGroup;
	private final SqlSelection discriminatorSqlSelection;
	private final SqlSelection tenantDiscriminatorSqlSelection;
	private final Map<PersistentAttribute, SqlSelectionGroup> attributeSqlSelectionGroupMap;

	public EntitySqlSelectionMappingsImpl(
			SqlSelection rowIdSqlSelection,
			SqlSelectionGroup idSqlSelectionGroup,
			SqlSelection discriminatorSqlSelection,
			SqlSelection tenantDiscriminatorSqlSelection,
			Map<PersistentAttribute, SqlSelectionGroup> attributeSqlSelectionGroupMap) {
		this.rowIdSqlSelection = rowIdSqlSelection;
		this.idSqlSelectionGroup = idSqlSelectionGroup;
		this.discriminatorSqlSelection = discriminatorSqlSelection;
		this.tenantDiscriminatorSqlSelection = tenantDiscriminatorSqlSelection;
		this.attributeSqlSelectionGroupMap = attributeSqlSelectionGroupMap;
	}

	@Override
	public SqlSelection getRowIdSqlSelection() {
		return rowIdSqlSelection;
	}

	@Override
	public SqlSelectionGroup getIdSqlSelectionGroup() {
		return idSqlSelectionGroup;
	}

	@Override
	public SqlSelection getDiscriminatorSqlSelection() {
		return discriminatorSqlSelection;
	}

	@Override
	public SqlSelection getTenantDiscriminatorSqlSelection() {
		return tenantDiscriminatorSqlSelection;
	}

	@Override
	public SqlSelectionGroup getAttributeSqlSelectionGroup(PersistentAttribute attribute) {
		return attributeSqlSelectionGroupMap.get( attribute );
	}
}
