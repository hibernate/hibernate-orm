/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.model;

import java.sql.Connection;

import org.hibernate.Session;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataBuilderImplementor;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.SkipLog;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;

import jakarta.persistence.EntityNotFoundException;

/**
 * An abstract test for all JPA spec related tests.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractJPATest extends BaseSessionFactoryFunctionalTest {
	@Override
	protected String[] getOrmXmlFiles() {
		return new String[] {
				"org/hibernate/orm/test/jpa/model/Part.hbm.xml",
				"org/hibernate/orm/test/jpa/model/Item.hbm.xml",
				"org/hibernate/orm/test/jpa/model/MyEntity.hbm.xml"
		};
	}

	// mimic specific exception aspects of the JPA environment ~~~~~~~~~~~~~~~~

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builder) {
		builder.applySetting( Environment.JPA_QUERY_COMPLIANCE, "true" );
		builder.applySetting( Environment.USE_SECOND_LEVEL_CACHE, "false" );
	}

	@Override
	protected void configure(SessionFactoryBuilder builder) {
		super.configure( builder );
		builder.applyEntityNotFoundDelegate( (entityName, id) -> {
			throw new EntityNotFoundException( "Unable to find " + entityName + " with id " + id );
		} );
	}

	@Override
	protected void applyMetadataBuilder(MetadataBuilder metadataBuilder) {
		((MetadataBuilderImplementor) metadataBuilder).getBootstrapContext().markAsJpaBootstrap();
	}

	// a useful method that doesn't really belong here ~~~~~~~~~~~~~~~~

	protected boolean readCommittedIsolationMaintained(String scenario) {
		final int isolation;
		try ( Session testSession = sessionFactory().openSession() ) {
			isolation = testSession.doReturningWork(Connection::getTransactionIsolation);
		}
		if ( isolation < Connection.TRANSACTION_READ_COMMITTED ) {
			SkipLog.reportSkip( "environment does not support at least read committed isolation", scenario );
			return false;
		}
		else {
			return true;
		}
	}
}
