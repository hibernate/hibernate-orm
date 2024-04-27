/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.jpa.boot;

import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.tool.schema.Action;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;

/**
 * @author Steve Ebersole
 */
public class PersistenceConfigurationTests {
	@Test
	void test1() {
		final PersistenceConfiguration configuration = new PersistenceConfiguration( "tst1" )
				.property( SchemaToolingSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, Action.CREATE_DROP );
		for ( Class<?> annotatedClass : StandardDomainModel.CONTACTS.getDescriptor().getAnnotatedClasses() ) {
			configuration.managedClass( annotatedClass );
		}
		try (EntityManagerFactory entityManagerFactory = new HibernatePersistenceProvider().createEntityManagerFactory( configuration )) {
			entityManagerFactory.runInTransaction( entityManager -> {
				entityManager.createQuery( "from Contact" ).getResultList();
			} );
		}
	}
}
