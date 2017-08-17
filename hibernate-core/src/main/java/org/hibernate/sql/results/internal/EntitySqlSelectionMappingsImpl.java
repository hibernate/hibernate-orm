/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.sql.results.spi.EntitySqlSelectionMappings;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.sql.results.spi.SqlSelectionGroup;

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

	public static class Builder {
		private SqlSelection rowIdSqlSelection;
		private SqlSelectionGroup idSqlSelectionGroup;
		private SqlSelection discriminatorSqlSelection;
		private SqlSelection tenantDiscriminatorSqlSelection;
		private Map<PersistentAttribute, SqlSelectionGroup> attributeSqlSelectionGroupMap;

		public Builder applyRowIdSqlSelection(SqlSelection rowIdSqlSelection) {
			this.rowIdSqlSelection = rowIdSqlSelection;
			return this;
		}

		public Builder applyIdSqlSelectionGroup(SqlSelectionGroup idSqlSelectionGroup) {
			if ( this.idSqlSelectionGroup != null ) {
				throw new HibernateException( "Multiple calls to set entity id SqlSelections" );
			}
			this.idSqlSelectionGroup = idSqlSelectionGroup;

			return this;
		}

		public Builder applyDiscriminatorSqlSelection(SqlSelection discriminatorSqlSelection) {
			if ( this.discriminatorSqlSelection != null ) {
				throw new HibernateException( "Multiple calls to set entity discriminator SqlSelection" );
			}
			this.discriminatorSqlSelection = discriminatorSqlSelection;

			return this;
		}

		public Builder applyTenantDiscriminatorSqlSelection(SqlSelection tenantDiscriminatorSqlSelection) {
			if ( this.tenantDiscriminatorSqlSelection != null ) {
				throw new HibernateException( "Multiple calls to set entity tenant-discriminator SqlSelection" );
			}
			this.tenantDiscriminatorSqlSelection = tenantDiscriminatorSqlSelection;

			return this;
		}

		public Builder applyAttributeSqlSelectionGroup(PersistentAttribute attribute, SqlSelectionGroup sqlSelectionGroup) {
			if ( attributeSqlSelectionGroupMap == null ) {
				attributeSqlSelectionGroupMap = new HashMap<>();
			}
			attributeSqlSelectionGroupMap.put( attribute, sqlSelectionGroup );
			return this;
		}

		public Builder applyAttributeSqlSelectionGroupMap(Map<PersistentAttribute, SqlSelectionGroup> map) {
			if ( attributeSqlSelectionGroupMap == null ) {
				attributeSqlSelectionGroupMap = new HashMap<>();
			}
			attributeSqlSelectionGroupMap.putAll( map );
			return this;
		}

		public EntitySqlSelectionMappingsImpl create() {
			return new EntitySqlSelectionMappingsImpl(
					rowIdSqlSelection,
					idSqlSelectionGroup,
					discriminatorSqlSelection,
					tenantDiscriminatorSqlSelection,
					attributeSqlSelectionGroupMap
			);
		}
	}
}
