/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11747")
@RequiresDialect(DB2390Dialect.class)
public class DB2390DialectTestCase extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { SimpleEntity.class };
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( AvailableSettings.USE_LEGACY_LIMIT_HANDLERS, Boolean.TRUE );
	}

	@Test
	public void testLegacyLimitHandlerWithNoOffset() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			List<SimpleEntity> results = entityManager.createQuery( "FROM SimpleEntity", SimpleEntity.class )
					.setMaxResults( 2 )
					.getResultList();
			assertEquals( Arrays.asList( 0, 1 ), results.stream().map( SimpleEntity::getId ).collect( Collectors.toList() ) );
		} );
	}

	@Test
	public void testLegacyLimitHandlerWithOffset() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			List<SimpleEntity> results = entityManager.createQuery( "FROM SimpleEntity", SimpleEntity.class )
					.setFirstResult( 2 )
					.setMaxResults( 2 )
					.getResultList();
			assertEquals( Arrays.asList( 2, 3 ), results.stream().map( SimpleEntity::getId ).collect( Collectors.toList() ) );
		} );
	}

	@Before
	public void populateSchema() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			for ( int i = 0; i < 10; ++i ) {
				final SimpleEntity simpleEntity = new SimpleEntity( i, "Entity" + i );
				entityManager.persist( simpleEntity );
			}
		} );
	}

	@After
	public void cleanSchema() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.createQuery( "DELETE FROM SimpleEntity" ).executeUpdate();
		} );
	}

	@Entity(name = "SimpleEntity")
	public static class SimpleEntity {
		@Id
		private Integer id;
		private String name;

		public SimpleEntity() {

		}

		public SimpleEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
