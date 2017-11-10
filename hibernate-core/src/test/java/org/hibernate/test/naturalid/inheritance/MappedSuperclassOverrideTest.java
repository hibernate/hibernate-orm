/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.naturalid.inheritance;

import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-12085" )
public class MappedSuperclassOverrideTest extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void test() {
		createTestData();

		try {
			inTransaction(
					session -> session.createQuery( "select e from MyEntity e" ).list()
			);
		}
		finally {
			dropTestData();
		}
	}

	private void createTestData() {
		inTransaction(
				session -> {
					session.save( new MyEntity( 1, "first" ) );
				}
		);
	}

	private void dropTestData() {
		inTransaction(
				session -> {
					session.createQuery( "delete MyEntity" ).executeUpdate();
				}
		);
	}

	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );
		settings.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
	}

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( MyMappedSuperclass.class )
				.addAnnotatedClass( MyEntity.class );
	}

	@MappedSuperclass
	public abstract static class MyMappedSuperclass {
		private Integer id;
		private String name;

		public MyMappedSuperclass() {
		}

		public MyMappedSuperclass(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		public Integer getId() {
			return id;
		}

		protected void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity( name = "MyEntity" )
	@Table( name = "the_entity" )
	@NaturalIdCache
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class MyEntity extends MyMappedSuperclass {
		public MyEntity() {
			super();
		}

		public MyEntity(Integer id, String name) {
			super( id, name );
		}

		// this should not be allowed, and supposedly fails anyway...
		@Override
		public String getName() {
			return super.getName();
		}
	}
}
