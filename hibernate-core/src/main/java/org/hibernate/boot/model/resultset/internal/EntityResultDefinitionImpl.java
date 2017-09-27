/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.resultset.internal;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.boot.model.resultset.spi.ResultSetMappingDefinition;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.query.sql.spi.QueryResultBuilder;
import org.hibernate.query.sql.spi.QueryResultBuilderRootEntity;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class EntityResultDefinitionImpl implements ResultSetMappingDefinition.EntityResult {
	private final String entityName;
	private final String entityClassName;
	private final String tableAlias;
	private LockMode lockMode;

	public EntityResultDefinitionImpl(String entityName, String entityClassName, String tableAlias) {
		if ( StringHelper.isEmpty( entityName ) && StringHelper.isEmpty( entityClassName ) ) {
			throw new HibernateException( "Native-query entity result must specify either rentity class name or entity name" );
		}

		this.entityName = entityName;
		this.entityClassName = entityClassName;
		this.tableAlias = tableAlias;
	}

	@Override
	public String getEntityName() {
		return entityName;
	}

	@Override
	public String getEntityClassName() {
		return entityClassName;
	}

	@Override
	public String getTableAlias() {
		return tableAlias;
	}

	@Override
	public QueryResultBuilder generateQueryResultBuilder(TypeConfiguration typeConfiguration) {
		final EntityDescriptor entityDescriptor;
		if ( StringHelper.isNotEmpty( entityName ) ) {
			entityDescriptor = typeConfiguration.resolveEntityDescriptor( entityName );
		}
		else {
			entityDescriptor = typeConfiguration.resolveEntityDescriptor( entityClassName );
		}

		// todo (6.0) : pass along lockMode, etc
		return new QueryResultBuilderRootEntity( tableAlias, entityDescriptor );
	}

	public void setLockMode(LockMode lockMode) {
		this.lockMode = lockMode;
	}
}
