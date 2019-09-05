/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.sql.exec;

import java.sql.Statement;
import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.metamodel.mapping.SmokeTests.SimpleEntity;
import org.hibernate.orm.test.metamodel.mapping.SmokeTests.Gender;
import org.hibernate.query.spi.QueryImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.orm.test.metamodel.mapping.SmokeTests.Gender.FEMALE;
import static org.hibernate.orm.test.metamodel.mapping.SmokeTests.Gender.MALE;

/**
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = SimpleEntity.class
)
@ServiceRegistry(
		settings = @ServiceRegistry.Setting(
				name = AvailableSettings.HBM2DDL_AUTO,
				value = "create-drop"
		)
)
@SessionFactory
public class SmokeTests {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SimpleEntity simpleEntity = new SimpleEntity();
					simpleEntity.setId( 1 );
					simpleEntity.setGender( FEMALE );
					simpleEntity.setName( "Fab" );
					simpleEntity.setGender2( MALE );
					session.save( simpleEntity );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.doWork(
								work -> {
									Statement statement = work.createStatement();
									statement.execute( "delete from mapping_simple_entity" );
									statement.close();
								}
						)
		);
	}

	@Test
	public void testSelectEntityFieldHqlExecution(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final QueryImplementor<String> query = session.createQuery(
							"select e.name from SimpleEntity e",
							String.class
					);
					List<String> simpleEntities = query.list();
					assertThat( simpleEntities.size(), is( 1 ) );
					assertThat( simpleEntities.get( 0 ), is( "Fab" ) );
				}
		);
	}

	@Test
	public void testSelectGenderHql(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final QueryImplementor<Gender> query = session.createQuery(
							"select e.gender from SimpleEntity e",
							Gender.class
					);
					List<Gender> simpleEntities = query.list();
					assertThat( simpleEntities.size(), is( 1 ) );
					assertThat( simpleEntities.get( 0 ), is( FEMALE ) );
				}
		);
	}

	@Test
	@FailureExpected( reason = "Support for entity-values DomainResults not yet implemented")
	public void testSelectEntityHqlExecution(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final QueryImplementor<SimpleEntity> query = session.createQuery(
							"select e from SimpleEntity e",
							SimpleEntity.class
					);
					List<SimpleEntity> simpleEntities = query.list();
					assertThat( simpleEntities.size(), is( 1 ) );
				}
		);
	}
}
