/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.sql.results.graph.Fetchable;

/**
 * @author Steve Ebersole
 */
public interface BasicValuedModelPart extends BasicValuedMapping, ModelPart, Fetchable, SelectableMapping {

	@Override
	default MappingType getPartMappingType() {
		return this::getJavaType;
	}
}
