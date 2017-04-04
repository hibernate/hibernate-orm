/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.id;

import java.util.List;
import java.util.Map;

import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public interface IdMapper {
	ServiceRegistry getServiceRegistry();

	void mapToMapFromId(Map<String, Object> data, Object obj);

	void mapToMapFromEntity(Map<String, Object> data, Object obj);

	/**
	 * @param obj Object to map to.
	 * @param data Data to map.
	 *
	 * @return True if data was mapped; false otherwise (when the id is {@code null}).
	 */
	boolean mapToEntityFromMap(Object obj, Map data);

	Object mapToIdFromEntity(Object data);

	Object mapToIdFromMap(Map data);

	/**
	 * Creates a mapper with all mapped properties prefixed. A mapped property is a property which
	 * is directly mapped to values (not composite).
	 *
	 * @param prefix Prefix to add to mapped properties
	 *
	 * @return A copy of the current property mapper, with mapped properties prefixed.
	 */
	IdMapper prefixMappedProperties(String prefix);

	/**
	 * @param obj Id from which to map.
	 *
	 * @return A set parameter data, needed to build a query basing on the given id.
	 */
	List<QueryParameterData> mapToQueryParametersFromId(Object obj);

	/**
	 * Adds query statements, which contains restrictions, which express the property that the id of the entity
	 * with alias prefix1, is equal to the id of the entity with alias prefix2 (the entity is the same).
	 *
	 * @param parameters Parameters, to which to add the statements.
	 * @param prefix1 First alias of the entity + prefix to add to the properties.
	 * @param prefix2 Second alias of the entity + prefix to add to the properties.
	 */
	void addIdsEqualToQuery(Parameters parameters, String prefix1, String prefix2);

	/**
	 * Adds query statements, which contains restrictions, which express the property that the id of the entity
	 * with alias prefix1, is equal to the id of the entity with alias prefix2 mapped by the second mapper
	 * (the second mapper must be for the same entity, but it can have, for example, prefixed properties).
	 *
	 * @param parameters Parameters, to which to add the statements.
	 * @param prefix1 First alias of the entity + prefix to add to the properties.
	 * @param mapper2 Second mapper for the same entity, which will be used to get properties for the right side
	 * of the equation.
	 * @param prefix2 Second alias of the entity + prefix to add to the properties.
	 */
	void addIdsEqualToQuery(Parameters parameters, String prefix1, IdMapper mapper2, String prefix2);

	/**
	 * Adds query statements, which contains restrictions, which express the property that the id of the entity
	 * with alias prefix, is equal to the given object.
	 *
	 * @param parameters Parameters, to which to add the statements.
	 * @param id Value of id.
	 * @param alias the alias to use in the specified parameters (may be null).
	 * @param prefix Prefix to add to the properties (may be null).
	 * @param equals Should this query express the "=" relation or the "<>" relation.
	 */
	void addIdEqualsToQuery(Parameters parameters, Object id, String alias, String prefix, boolean equals);

	/**
	 * Adds query statements, which contains named parameters, which express the property that the id of the entity
	 * with alias prefix, is equal to the given object. It is the responsibility of the using method to read
	 * parameter values from the id and specify them on the final query object.
	 *
	 * @param parameters Parameters, to which to add the statements.
	 * @param prefix Prefix to add to the properties (may be null).
	 * @param equals Should this query express the "=" relation or the "<>" relation.
	 */
	void addNamedIdEqualsToQuery(Parameters parameters, String prefix, boolean equals);

	/**
	 * Adds query statements, which contains named parameters that express the property that the id of the entity
	 * with alias prefix is equal to the given object using the specified mapper.
	 *
	 * @param parameters Parameters, to which to add the statements.
	 * @param prefix Prefix to add to the properties (may be null).
	 * @param mapper The identifier mapper to use
	 * @param equals Should this query express the "=" relation or the "<>" relation.
	 *
	 * @since 5.2.2
	 */
	void addNamedIdEqualsToQuery(Parameters parameters, String prefix, IdMapper mapper, boolean equals);
}
