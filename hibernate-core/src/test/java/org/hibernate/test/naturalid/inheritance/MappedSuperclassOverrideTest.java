/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.naturalid.inheritance;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;

import org.hibernate.annotations.NaturalId;
import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
@FailureExpected( jiraKey = "HHH-12085" )
public class MappedSuperclassOverrideTest extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void testModel() {
		assertTrue(
				sessionFactory().getMetamodel().entityPersister( MyEntity.class )
						.hasNaturalIdentifier()
		);
	}

	@Override
	protected boolean createSchema() {
		return false;
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( MyMappedSuperclass.class )
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
	public static class MyEntity extends MyMappedSuperclass {
		public MyEntity() {
			super();
		}

		public MyEntity(Integer id, String name) {
			super( id, name );
		}

		// this should not be allowed, and supposedly fails anyway...
		@Override
		@NaturalId
		public String getName() {
			return super.getName();
		}
	}
}
