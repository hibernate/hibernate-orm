/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.projection;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.entities.EntityInstantiator;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public interface AuditProjection {
	/**
	 * @param enversService The EnversService
	 *
	 * @return the projection data
	 */
	ProjectionData getData(EnversService enversService);

	/**
	 * @param enversService the Envers service
	 * @param entityInstantiator the entity instantiator
	 * @param entityName the name of the entity for which the projection has been added
	 * @param revision the revision
	 * @param value the value to convert
	 * @return the converted value
	 */
	Object convertQueryResult(
			final EnversService enversService,
			final EntityInstantiator entityInstantiator,
			final String entityName,
			final Number revision,
			final Object value
	);

	class ProjectionData {

		private final String function;
		private final String alias;
		private final String propertyName;
		private final boolean distinct;

		public ProjectionData(String function, String alias, String propertyName, boolean distinct) {
			this.function = function;
			this.alias = alias;
			this.propertyName = propertyName;
			this.distinct = distinct;
		}

		public String getFunction() {
			return function;
		}

		public String getAlias(String baseAlias) {
			return alias == null ? baseAlias : alias;
		}

		public String getPropertyName() {
			return propertyName;
		}

		public boolean isDistinct() {
			return distinct;
		}

	}
}
