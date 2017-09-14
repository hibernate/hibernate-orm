/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.query.NavigablePath;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;

/**
 * Further defines a first-level Return that is a reference to an entity
 *
 * @author Steve Ebersole
 */
public interface EntityQueryResult extends EntityMappingNode, QueryResult {
	@Override
	default EntityJavaDescriptor getJavaTypeDescriptor() {
		return getEntityDescriptor().getJavaTypeDescriptor();
	}

	default NavigablePath getNavigablePath() {
		return new NavigablePath( getEntityDescriptor().getEntityName() );
	}

}
