/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.spi;

import org.hibernate.sqm.domain.DomainMetamodel;
import org.hibernate.type.mapper.spi.basic.BasicType;
import org.hibernate.type.mapper.spi.basic.TemporalType;

/**
 * @author Steve Ebersole
 */
public interface SqmDomainMetamodelImplementor extends DomainMetamodel {
	@Override
	<T> BasicType<T> getBasicType(Class<T> javaType);

	@Override
	<T> TemporalType<T> getBasicType(Class<T> javaType, javax.persistence.TemporalType temporalType);

	@Override
	BasicType resolveCastTargetType(String name);
}
