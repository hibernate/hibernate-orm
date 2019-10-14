/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.manytomany.mapkey;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKey;
import javax.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-4235")
@RequiresDialectFeature(DialectChecks.SupportSchemaCreation.class)
public class ManyToManyWithMaykeyAndSchemaDefinitionTest extends BaseCoreFunctionalTestCase {

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.HBM2DDL_CREATE_SCHEMAS, "true" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { EntityA.class, EntityB.class };
	}

	@Before
	public void setUp() {
		inTransaction(
				session -> {
					EntityA entityA = new EntityA();
					entityA.setId( 1L );

					EntityB entityB = new EntityB();
					entityB.setId( 1L );
					entityA.setEntityBs( "B", entityB );
					session.persist( entityB );
					session.persist( entityA );
				}
		);
	}

	@Test
	public void testRetrievingTheMapGeneratesACorrectlyQuery() {

		inTransaction(
				session -> {
					EntityA entityA = session.get( EntityA.class, 1L );
					Collection<EntityB> values = entityA.getEntityBMap().values();
					assertThat( values.size(), is( 1 ) );
				}
		);

	}


	@Entity(name = "EntityA")
	@Table(name = "entitya", schema = "myschema")
	public static class EntityA {

		@Id
		private Long id;

		private String name;

		@ManyToMany
		@MapKey(name = "id")
		@JoinTable(name = "entitya_entityb", schema = "myschema", joinColumns = @JoinColumn(name = "entitya_pk"), inverseJoinColumns = @JoinColumn(name = "entityb_pk"))
		private Map<String, EntityB> entityBMap = new HashMap<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public void setEntityBs(String key, EntityB entityB) {
			this.entityBMap.put( key, entityB );
		}

		public Map<String, EntityB> getEntityBMap() {
			return entityBMap;
		}
	}

	@Entity(name = "EntityB")
	@Table(name = "entityb", schema = "myschema")
	public static class EntityB {
		@Id
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

	}
}
