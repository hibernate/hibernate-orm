/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation.component;

import java.util.Map;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.envers.internal.entities.EntityInstantiator;
import org.hibernate.envers.internal.entities.mapper.id.IdMapper;
import org.hibernate.envers.internal.tools.query.Parameters;

/**
 * A component mapper for the @MapKey mapping: the value of the map's key is the id of the entity. This
 * doesn't have an effect on the data stored in the versions tables, so <code>mapToMapFromObject</code> is
 * empty.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public final class MiddleMapKeyIdComponentMapper implements MiddleComponentMapper {
	private final AuditEntitiesConfiguration verEntCfg;
	private final IdMapper relatedIdMapper;

	public MiddleMapKeyIdComponentMapper(AuditEntitiesConfiguration verEntCfg, IdMapper relatedIdMapper) {
		this.verEntCfg = verEntCfg;
		this.relatedIdMapper = relatedIdMapper;
	}

	@Override
	public Object mapToObjectFromFullMap(
			EntityInstantiator entityInstantiator, Map<String, Object> data,
			Object dataObject, Number revision) {
		return relatedIdMapper.mapToIdFromMap( (Map) data.get( verEntCfg.getOriginalIdPropName() ) );
	}

	@Override
	public void mapToMapFromObject(
			SessionImplementor session,
			Map<String, Object> idData,
			Map<String, Object> data,
			Object obj) {
		// Doing nothing.
	}

	@Override
	public void addMiddleEqualToQuery(
			Parameters parameters,
			String idPrefix1,
			String prefix1,
			String idPrefix2,
			String prefix2) {
		// Doing nothing.
	}
}
