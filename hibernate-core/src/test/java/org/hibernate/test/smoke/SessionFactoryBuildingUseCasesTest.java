/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.smoke;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.MetadataSources;

import org.junit.Test;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-7580" )
public class SessionFactoryBuildingUseCasesTest extends BaseUnitTestCase {
	@Test
	public void testTheSimplestUseCase() {
		StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
				.build();
		MetadataSources metadataSources = new MetadataSources( registry ).addAnnotatedClass( MyEntity.class );
		SessionFactory sessionFactory = metadataSources.buildMetadata().buildSessionFactory();

		useSessionFactory( sessionFactory );

		sessionFactory.close();
		StandardServiceRegistryBuilder.destroy( registry );
	}

	private void useSessionFactory(SessionFactory sessionFactory) {
		Session session = sessionFactory.openSession();
		session.beginTransaction();
		session.createQuery( "from MyEntity" ).list();
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testStillSimpleOnePhaseUseCase() {
		BootstrapServiceRegistry bootRegistry = new BootstrapServiceRegistryBuilder().build();
		StandardServiceRegistry registry = new StandardServiceRegistryBuilder( bootRegistry )
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
				.build();
		MetadataSources metadataSources = new MetadataSources( registry ).addAnnotatedClass( MyEntity.class );
		SessionFactory sessionFactory = metadataSources.buildMetadata().buildSessionFactory();

		useSessionFactory( sessionFactory );

		sessionFactory.close();
		StandardServiceRegistryBuilder.destroy( registry );
	}

	@Test
	public void testSimpleTwoPhaseUseCase() {
		BootstrapServiceRegistry bootRegistry = new BootstrapServiceRegistryBuilder().build();
		MetadataSources metadataSources = new MetadataSources( bootRegistry ).addAnnotatedClass( MyEntity.class );

		StandardServiceRegistry registry = new StandardServiceRegistryBuilder( bootRegistry )
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
				.build();
		SessionFactory sessionFactory = metadataSources.buildMetadata( registry ).buildSessionFactory();

		useSessionFactory( sessionFactory );

		sessionFactory.close();
		StandardServiceRegistryBuilder.destroy( registry );
	}

	@Test
	public void testFullTwoPhaseUseCase() {
		BootstrapServiceRegistry bootRegistry = new BootstrapServiceRegistryBuilder().build();
		MetadataSources metadataSources = new MetadataSources( bootRegistry ).addAnnotatedClass( MyEntity.class );

		StandardServiceRegistry registry = new StandardServiceRegistryBuilder( bootRegistry )
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
				.build();
		Metadata metadata = metadataSources.getMetadataBuilder( registry ).build();
		SessionFactory sessionFactory = metadata.getSessionFactoryBuilder().build(); // todo rename

		useSessionFactory( sessionFactory );

		sessionFactory.close();
		StandardServiceRegistryBuilder.destroy( registry );
	}

	@Test
	@FailureExpected( jiraKey = "HHH-7580" )
	public void testInvalidQuasiUseCase() {
		BootstrapServiceRegistry bootRegistry = new BootstrapServiceRegistryBuilder().build();
		MetadataSources metadataSources = new MetadataSources( bootRegistry ).addAnnotatedClass( MyEntity.class );

		StandardServiceRegistry registry = new StandardServiceRegistryBuilder( bootRegistry )
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
				.build();
		SessionFactory sessionFactory = metadataSources.buildMetadata().buildSessionFactory();

		useSessionFactory( sessionFactory );

		sessionFactory.close();
		StandardServiceRegistryBuilder.destroy( registry );
	}

	@Entity( name = "MyEntity" )
	@Table( name = "TST_ENT" )
	public static class MyEntity {
		@Id
		private Long id;
	}
}
