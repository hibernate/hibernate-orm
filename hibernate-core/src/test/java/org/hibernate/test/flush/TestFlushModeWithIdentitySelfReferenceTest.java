/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.hibernate.test.flush;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.junit.Test;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-13042")
@RequiresDialectFeature(DialectChecks.SupportsIdentityColumns.class)
public class TestFlushModeWithIdentitySelfReferenceTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { SelfRefEntity.class };
	}

	@Test
	public void testFlushModeCommit() throws Exception {
		Session session = openSession();
		try {
			session.setHibernateFlushMode( FlushMode.COMMIT );
			loadAndAssert( session, createAndInsertEntity( session ) );
		}
		finally {
			session.close();
		}
	}

	@Test
	public void testFlushModeManual() throws Exception {
		Session session = openSession();
		try {
			session.setHibernateFlushMode( FlushMode.MANUAL );
			loadAndAssert( session, createAndInsertEntity( session ) );
		}
		finally {
			session.close();
		}
	}

	@Test
	public void testFlushModeAuto() throws Exception {
		Session session = openSession();
		try {
			session.setHibernateFlushMode( FlushMode.AUTO );
			loadAndAssert( session, createAndInsertEntity( session ) );
		}
		finally {
			session.close();
		}
	}

	private SelfRefEntity createAndInsertEntity(Session session) {
		try {
			session.getTransaction().begin();

			SelfRefEntity entity = new SelfRefEntity();
			entity.setSelfRefEntity( entity );
			entity.setData( "test" );
			entity = (SelfRefEntity) session.merge( entity );

			// only during manual flush do we want to force a flush prior to commit
			if ( session.getHibernateFlushMode().equals( FlushMode.MANUAL ) ) {
				session.flush();
			}

			session.getTransaction().commit();

			return entity;
		}
		catch ( Exception e ) {
			if ( session.getTransaction().isActive() ) {
				session.getTransaction().rollback();
			}
			throw e;
		}
	}

	private void loadAndAssert(Session session, SelfRefEntity mergedEntity) {
		final SelfRefEntity loadedEntity = session.get( SelfRefEntity.class, mergedEntity.getId() );
		assertNotNull( "Expected to find the merged entity but did not.", loadedEntity );
		assertEquals( "test", loadedEntity.getData() );
		assertNotNull( "Expected a non-null self reference", loadedEntity.getSelfRefEntity() );
	}

	@Entity(name = "SelfRefEntity")
	public static class SelfRefEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Integer id;
		@OneToOne(cascade = CascadeType.ALL, optional = true)
		private SelfRefEntity selfRefEntity;
		private String data;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public SelfRefEntity getSelfRefEntity() {
			return selfRefEntity;
		}

		public void setSelfRefEntity(SelfRefEntity selfRefEntity) {
			this.selfRefEntity = selfRefEntity;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}
	}
}
