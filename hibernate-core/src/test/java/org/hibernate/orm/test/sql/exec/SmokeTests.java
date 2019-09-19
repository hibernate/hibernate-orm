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
import org.hibernate.orm.test.metamodel.mapping.SmokeTests.Component;
import org.hibernate.orm.test.metamodel.mapping.SmokeTests.SimpleEntity;
import org.hibernate.orm.test.metamodel.mapping.SmokeTests.Gender;
import org.hibernate.query.Query;
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
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.orm.test.metamodel.mapping.SmokeTests.Gender.FEMALE;
import static org.hibernate.orm.test.metamodel.mapping.SmokeTests.Gender.MALE;

/**
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = SimpleEntity.class,
		extraQueryImportClasses = {
				SmokeTests.ListItemDto.class,
				SmokeTests.CategorizedListItemDto.class,
				SmokeTests.CompoundDto.class,
				SmokeTests.BasicSetterBasedDto.class
		}
)
@ServiceRegistry(
		settings = {
				@ServiceRegistry.Setting(
						name = AvailableSettings.HBM2DDL_AUTO,
						value = "create-drop"
				)
		}
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
					simpleEntity.setComponent( new Component( "a1", "a2" ) );
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
	public void testHqlSelectEntityBasicAttribute(SessionFactoryScope scope) {
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
	public void testHqlSelectConvertedAttribute(SessionFactoryScope scope) {
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
	public void testHqlSelectRootEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final QueryImplementor<SimpleEntity> query = session.createQuery(
							"select e from SimpleEntity e",
							SimpleEntity.class
					);
					List<SimpleEntity> simpleEntities = query.list();
					assertThat( simpleEntities.size(), is( 1 ) );
					SimpleEntity simpleEntity = simpleEntities.get( 0 );
					assertThat( simpleEntity.getId(), is(1) );
					assertThat( simpleEntity.getGender(), is(FEMALE) );
					assertThat( simpleEntity.getGender2(), is(MALE) );
					assertThat( simpleEntity.getName(), is("Fab") );
					assertThat( simpleEntity.getComponent(), notNullValue() );
					assertThat( simpleEntity.getComponent().getAttribute1(), is( "a1" ) );
					assertThat( simpleEntity.getComponent().getAttribute2(), is( "a2" ) );
				}
		);
	}

	@Test
	public void testHqlSelectEmbeddedAttribute(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final QueryImplementor<Component> query = session.createQuery( "select e.component from SimpleEntity e", Component.class );
					final Component component = query.uniqueResult();
					assertThat( component, notNullValue() );
					assertThat( component.getAttribute1(), is( "a1" ) );
					assertThat( component.getAttribute2(), is( "a2" ) );
				}
		);
	}

	@Test
	public void testHqlSelectEmbeddableSubAttribute(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final QueryImplementor<String> query = session.createQuery( "select e.component.attribute1 from SimpleEntity e", String.class );
					final String attribute1 = query.uniqueResult();
					assertThat( attribute1, is( "a1" ) );
				}
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Dynamic instantiations

	public static class ListItemDto {
		private String code;
		private String value;

		public ListItemDto(String code, String value) {
			this.code = code;
			this.value = value;
		}
	}

	public static class CategorizedListItemDto {
		private ListItemDto category;
		private String code;
		private String value;

		public CategorizedListItemDto(ListItemDto category, String code, String value) {
			this.category = category;
			this.code = code;
			this.value = value;
		}
	}

	public static class CompoundDto {
		private ListItemDto first;
		private ListItemDto second;

		public CompoundDto(ListItemDto first, ListItemDto second) {
			this.first = first;
			this.second = second;
		}
	}

	public static class BasicSetterBasedDto {
		private String code;
		private String value;

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	@Test
	public void testHqlBasicDynamicInstantiation(SessionFactoryScope scope) {
		scope.getSessionFactory().inTransaction(
				session -> {
					final Query<ListItemDto> query = session.createQuery(
							"select new ListItemDto( e.component.attribute1, e.name ) from SimpleEntity e",
							ListItemDto.class
					);

					final ListItemDto dto = query.getSingleResult();
					assertThat( dto, notNullValue() );
					assertThat( dto.code, is( "a1" ) );
					assertThat( dto.value, is( "Fab" ) );
				}
		);
	}

	@Test
	public void testHqlNestedDynamicInstantiation(SessionFactoryScope scope) {
		scope.getSessionFactory().inTransaction(
				session -> {
					final Query<CategorizedListItemDto> query = session.createQuery(
							"select new CategorizedListItemDto( new ListItemDto( e.component.attribute2, e.component.attribute1 ), e.component.attribute1, e.name ) from SimpleEntity e",
							CategorizedListItemDto.class
					);

					final CategorizedListItemDto dto = query.getSingleResult();
					assertThat( dto, notNullValue() );
					assertThat( dto.category, notNullValue() );
					assertThat( dto.category.code, is( "a2") );
					assertThat( dto.category.value, is( "a1") );
					assertThat( dto.code, is( "a1" ) );
					assertThat( dto.value, is( "Fab" ) );
				}
		);
	}

	@Test
	public void testHqlMultipleDynamicInstantiation(SessionFactoryScope scope) {
		scope.getSessionFactory().inTransaction(
				session -> {
					final Query<CompoundDto> query = session.createQuery(
							"select new CompoundDto( new ListItemDto( e.component.attribute1, e.name ), new ListItemDto( e.component.attribute2, e.name ) ) from SimpleEntity e",
							CompoundDto.class
					);

					final CompoundDto dto = query.getSingleResult();
					assertThat( dto, notNullValue() );

					assertThat( dto.first, notNullValue() );
					assertThat( dto.first.code, is( "a1" ) );
					assertThat( dto.first.value, is( "Fab" ) );

					assertThat( dto.second, notNullValue() );
					assertThat( dto.second.code, is( "a2" ) );
					assertThat( dto.second.value, is( "Fab" ) );
				}
		);
	}

	@Test
	public void testBasicSetterDynamicInstantiation(SessionFactoryScope scope) {
		scope.getSessionFactory().inTransaction(
				session -> {
					final Query<BasicSetterBasedDto> query = session.createQuery(
							"select new BasicSetterBasedDto( e.component.attribute1 as code, e.name as value ) from SimpleEntity e",
							BasicSetterBasedDto.class
					);

					final BasicSetterBasedDto dto = query.getSingleResult();
					assertThat( dto, notNullValue() );

					assertThat( dto.code, is( "a1" ) );
					assertThat( dto.value, is( "Fab" ) );
				}
		);
	}
}
