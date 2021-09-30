/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.nationalized;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import org.hibernate.annotations.Nationalized;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10364")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNationalizedData.class)
@DomainModel(
		annotatedClasses = NationalizedLobFieldTest.MyEntity.class
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, value = "false")
)
public class NationalizedLobFieldTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					MyEntity e = new MyEntity( 1L );
					e.setState( "UK" );
					session.save( e );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createQuery( "delete from MyEntity" ).executeUpdate()
		);
	}

	@Test
	public void testNationalization(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					MyEntity myEntity = session.get( MyEntity.class, 1L );
					assertNotNull( myEntity );
					assertThat( myEntity.getState(), is( "UK" ) );
				}
		);
	}

	@Entity(name = "MyEntity")
	@Table(name = "my_entity")
	public static class MyEntity {
		@Id
		private long id;

		@Lob
		@Nationalized
		private String state;

		public MyEntity() {
		}

		public MyEntity(long id) {
			this.id = id;
		}

		public long getId() {
			return id;
		}

		public String getState() {
			return state;
		}

		public void setState(String state) {
			this.state = state;
		}
	}
}
