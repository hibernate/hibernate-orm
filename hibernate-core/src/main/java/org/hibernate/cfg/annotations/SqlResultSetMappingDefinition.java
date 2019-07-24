/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cfg.annotations;

import javax.persistence.SqlResultSetMapping;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.NamedResultSetMappingDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.internal.NamedResultSetMappingMementoImpl;
import org.hibernate.query.spi.NamedResultSetMappingMemento;

/**
 * @author Steve Ebersole
 */
public class SqlResultSetMappingDefinition implements NamedResultSetMappingDefinition {
	public static SqlResultSetMappingDefinition from(
			SqlResultSetMapping mappingAnnotation,
			MetadataBuildingContext context) {
		return new SqlResultSetMappingDefinition( mappingAnnotation.name(), context );
	}

	private final String mappingName;

	private SqlResultSetMappingDefinition(String mappingName, MetadataBuildingContext context) {
		this.mappingName = mappingName;
	}

	@Override
	public String getRegistrationName() {
		return mappingName;
	}

	@Override
	public NamedResultSetMappingMemento resolve(SessionFactoryImplementor factory) {
		return new NamedResultSetMappingMementoImpl( mappingName, factory );
	}
}
