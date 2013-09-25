/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.test.ops;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.event.spi.JpaIntegrator;
import org.hibernate.jpa.test.PersistenceUnitDescriptorAdapter;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * @author Steve Ebersole
 */
public class RemoveOrderingTest extends BaseUnitTestCase {
	@Test
	@TestForIssue( jiraKey="HHH-8550" )
	public void testManyToOne() {
		Configuration cfg = new Configuration()
				.addAnnotatedClass( Company.class )
				.addAnnotatedClass( Person.class )
				.setProperty( AvailableSettings.CHECK_NULLABILITY, "true" )
				.setProperty( AvailableSettings.URL, "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;MVCC=TRUE;LOCK_TIMEOUT=10000" )
				.setProperty( AvailableSettings.USER, "sa" )
				.setProperty( AvailableSettings.DRIVER, org.h2.Driver.class.getName() )
				.setProperty( AvailableSettings.HBM2DDL_AUTO, "create-drop" );

		cfg.buildMappings();

//		Iterator<PersistentClass> classMappings = cfg.getClassMappings();
//		while ( classMappings.hasNext() ) {
//			PersistentClass pc = classMappings.next();
//			if ( pc.getMappedClass().equals( Person.class ) ) {
//				Property prop = pc.getProperty( "employer" );
//				Iterator<Selectable> selectables = prop.getValue().getColumnIterator();
//				while ( selectables.hasNext() ) {
//					Column column = (Column) selectables.next();
//					column.setNullable( true );
//				}
//			}
//		}

		BootstrapServiceRegistry bootstrapRegistry = new BootstrapServiceRegistryBuilder().with( new JpaIntegrator() ).build();
		StandardServiceRegistry registry = new StandardServiceRegistryBuilder( bootstrapRegistry )
				.applySettings( cfg.getProperties() )
				.build();
		SessionFactory sf = cfg.buildSessionFactory( registry );

		try {
			Session session = sf.openSession();
			session.beginTransaction();
			Company company = new Company( 1, "acme" );
			Person person = new Person( 1, "joe", company );
			session.persist( person );
			session.flush();

			Company company2 = person.employer;

			session.delete( company2 );
			session.delete( person );
			session.flush();

			session.persist( person );
			session.flush();

			session.getTransaction().commit();
			session.close();
		}
		finally {
			sf.close();
		}
	}

	@Entity( name="Company" )
	@Table( name = "COMPANY" )
	public static class Company {
		@Id
		public Integer id;
		public String name;

		public Company() {
		}

		public Company(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity( name="Person" )
	@Table( name = "PERSON" )
	public static class Person {
		@Id
		public Integer id;
		public String name;
		@ManyToOne( cascade= CascadeType.ALL, optional = false )
		@JoinColumn( name = "EMPLOYER_FK" )
		public Company employer;

		public Person() {
		}

		public Person(Integer id, String name, Company employer) {
			this.id = id;
			this.name = name;
			this.employer = employer;
		}
	}
}
