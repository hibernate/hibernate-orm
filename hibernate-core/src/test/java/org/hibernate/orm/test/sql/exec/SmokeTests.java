/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.sql.exec;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.orm.test.metamodel.mapping.SmokeTests.OtherEntity;
import org.hibernate.orm.test.metamodel.mapping.SmokeTests.SimpleEntity;
import org.hibernate.query.Query;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
@SuppressWarnings({ "WeakerAccess", "DefaultAnnotationParam" })
@DomainModel(
		annotatedClasses = { SmokeTests.SimpleEntity.class, SmokeTests.OtherEntity.class },
		extraQueryImportClasses = {
				SmokeTests.ListItemDto.class,
				SmokeTests.CategorizedListItemDto.class,
				SmokeTests.CompoundDto.class,
				SmokeTests.BasicSetterBasedDto.class
		}
)
@ServiceRegistry(
		settings = {
				@ServiceRegistry.Setting(name = AvailableSettings.POOL_SIZE, value = "15"),
				@ServiceRegistry.Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false")
		}
)
@SessionFactory(exportSchema = true)
public class SmokeTests {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SimpleEntity simpleEntity = new SimpleEntity();
					simpleEntity.setId( 1 );
					simpleEntity.setGender( Gender.FEMALE );
					simpleEntity.setName( "Fab" );
					simpleEntity.setGender2( Gender.MALE );
					simpleEntity.setComponent( new Component( "a1", "a2" ) );
					session.save( simpleEntity );
					OtherEntity otherEntity = new OtherEntity();
					otherEntity.setId( 2 );
					otherEntity.setName( "Bar" );
					session.save( otherEntity );
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
									statement.execute( "delete from mapping_other_entity" );
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
					assertThat( simpleEntities.get( 0 ), is( Gender.FEMALE ) );
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
					assertThat( simpleEntity.getId(), is( 1 ) );
					assertThat( simpleEntity.getGender(), is( Gender.FEMALE ) );
					assertThat( simpleEntity.getGender2(), is( Gender.MALE ) );
					assertThat( simpleEntity.getName(), is( "Fab" ) );
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
					final QueryImplementor<Component> query = session.createQuery(
							"select e.component from SimpleEntity e",
							Component.class
					);
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
					final QueryImplementor<String> query = session.createQuery(
							"select e.component.attribute1 from SimpleEntity e",
							String.class
					);
					final String attribute1 = query.uniqueResult();
					assertThat( attribute1, is( "a1" ) );
				}
		);
	}

	@Test
	public void testHqlSelectLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final QueryImplementor<String> query = session.createQuery(
							"select 'items' from SimpleEntity e",
							String.class
					);
					final String attribute1 = query.uniqueResult();
					assertThat( attribute1, is( "items" ) );
				}
		);
	}

	@Test
	public void testHqlSelectParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final QueryImplementor<String> query = session.createQuery(
							"select :param from SimpleEntity e",
							String.class
					);
					final String attribute1 = query.setParameter( "param", "items" ).uniqueResult();
					assertThat( attribute1, is( "items" ) );
				}
		);
	}

	@Test
	public void testHqlBasicParameterUsage(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final QueryImplementor<Component> query = session.createQuery(
							"select e.component from SimpleEntity e where e.component.attribute1 = :param",
							Component.class
					);
					final Component component = query.setParameter( "param", "a1" ).uniqueResult();
					assertThat( component, notNullValue() );
					assertThat( component.getAttribute1(), is( "a1" ) );
					assertThat( component.getAttribute2(), is( "a2" ) );
				}
		);
	}

	@Test
	public void testColumnQualification(SessionFactoryScope scope) {
		// make sure column references to the same column qualified by different table references works properly

		// first, let's create a second entity
		scope.inTransaction(
				session -> {
					SimpleEntity simpleEntity = new SimpleEntity();
					simpleEntity.setId( 2 );
					simpleEntity.setGender( Gender.MALE );
					simpleEntity.setName( "Andrea" );
					simpleEntity.setGender2( Gender.FEMALE );
					simpleEntity.setComponent( new Component( "b1", "b2" ) );
					session.save( simpleEntity );
				}
		);

		scope.inTransaction(
				session -> {
					final Object[] result = session.createQuery(
							"select e, e2 from SimpleEntity e, SimpleEntity e2 where e.id = 1 and e2.id = 2",
							Object[].class
					)
							.uniqueResult();
					assertThat( result, notNullValue() );

					assertThat( result[0], instanceOf( SimpleEntity.class ) );
					assertThat( ( (SimpleEntity) result[0] ).getId(), is( 1 ) );
					assertThat( ( (SimpleEntity) result[0] ).getComponent().getAttribute1(), is( "a1" ) );

					assertThat( result[1], instanceOf( SimpleEntity.class ) );
					assertThat( ( (SimpleEntity) result[1] ).getId(), is( 2 ) );
					assertThat( ( (SimpleEntity) result[1] ).getComponent().getAttribute1(), is( "b1" ) );
				}
		);
	}

	@Test
	public void testHqlQueryReuseWithDiffParameterBinds(SessionFactoryScope scope) {
		// first, let's create a second entity
		scope.inTransaction(
				session -> {
					SimpleEntity simpleEntity = new SimpleEntity();
					simpleEntity.setId( 2 );
					simpleEntity.setGender( Gender.MALE );
					simpleEntity.setName( "Andrea" );
					simpleEntity.setGender2( Gender.FEMALE );
					simpleEntity.setComponent( new Component( "b1", "b2" ) );
					session.save( simpleEntity );
				}
		);

		scope.inTransaction(
				session -> {
					// create a Query ref that we will use to query for each entity individually through a parameter
					final QueryImplementor<Component> query = session.createQuery(
							"select e.component from SimpleEntity e where e.component.attribute1 = :param",
							Component.class
					);

					{
						// first query execution searching for the standard entity created in set-up
						final Component component = query.setParameter( "param", "a1" ).uniqueResult();
						assertThat( component, notNullValue() );
						assertThat( component.getAttribute1(), is( "a1" ) );
						assertThat( component.getAttribute2(), is( "a2" ) );
					}

					{
						// second query execution searching for the second entity created locally
						final Component component = query.setParameter( "param", "b1" ).uniqueResult();
						assertThat( component, notNullValue() );
						assertThat( component.getAttribute1(), is( "b1" ) );
						assertThat( component.getAttribute2(), is( "b2" ) );
					}
				}
		);
	}

	@Test
	@FailureExpected
	public void testSelectManyToOne(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select e.simpleEntity from OtherEntity e" ).list();
				}
		);
	}

	@Test
	public void testQueryConcurrency(SessionFactoryScope scope) throws InterruptedException {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		final int numberOfIterations = 400;
		final int numberOfForks = 50;

		final ExecutorService executor = Executors.newFixedThreadPool( 5 );

		try {
			for ( int f = 0; f < numberOfForks; f++ ) {
				final ArrayList<Callable<String>> tasks = CollectionHelper.arrayList( numberOfIterations );

				for ( int i = 0; i < numberOfIterations; i++ ) {
					tasks.add( () -> executeQueriesForConcurrency( scope ) );
				}

				executor.invokeAll( tasks );
			}
		}
		finally {
			// make sure all iterations/tasks have completed
			executor.shutdown();
			executor.awaitTermination( 15, TimeUnit.SECONDS );
		}
	}

	public String executeQueriesForConcurrency(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final QueryImplementor<Component> query1 = session.createQuery(
							"select e.component from SimpleEntity e where e.component.attribute1 = :param",
							Component.class
					);
					query1.setParameter( "param", "a1" ).list();

					final QueryImplementor<Component> query2 = session.createQuery(
							"select e.component from SimpleEntity e where e.component.attribute1 = :param",
							Component.class
					);
					query2.setParameter( "param", "b1" ).list();

					final QueryImplementor<Component> query3 = session.createQuery(
							"select e from SimpleEntity e where e.component.attribute1 = :param",
							Component.class
					);
					query3.setParameter( "param", "a1" ).list();
				}
		);

		return null;
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
					assertThat( dto.category.code, is( "a2" ) );
					assertThat( dto.category.value, is( "a1" ) );
					assertThat( dto.code, is( "a1" ) );
					assertThat( dto.value, is( "Fab" ) );
				}
		);
	}

	@Test
	public void testHqlNestedDynamicInstantiationWithLiteral(SessionFactoryScope scope) {
		scope.getSessionFactory().inTransaction(
				session -> {
					final Query<CategorizedListItemDto> query = session.createQuery(
							"select new CategorizedListItemDto( new ListItemDto( 'items', e.component.attribute1 ), e.component.attribute2, e.name ) from SimpleEntity e",
							CategorizedListItemDto.class
					);

					final CategorizedListItemDto dto = query.getSingleResult();
					assertThat( dto, notNullValue() );
					assertThat( dto.category, notNullValue() );
					assertThat( dto.category.code, is( "items" ) );
					assertThat( dto.category.value, is( "a1" ) );
					assertThat( dto.code, is( "a2" ) );
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
	public void testHqlBasicSetterDynamicInstantiation(SessionFactoryScope scope) {
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

	public enum Gender {
		MALE,
		FEMALE
	}

	@Entity(name = "OtherEntity")
	@Table(name = "mapping_other_entity")
	@SuppressWarnings("unused")
	public static class OtherEntity {
		private Integer id;
		private String name;

		@Id
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


	@Entity(name = "SimpleEntity")
	@Table(name = "mapping_simple_entity")
	@SuppressWarnings("unused")
	public static class SimpleEntity {
		private Integer id;
		private String name;
		private Gender gender;
		private Gender gender2;
		private Component component;

		@Id
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

		@Enumerated
		public Gender getGender() {
			return gender;
		}

		public void setGender(Gender gender) {
			this.gender = gender;
		}

		@Enumerated(EnumType.STRING)
		public Gender getGender2() {
			return gender2;
		}

		public void setGender2(Gender gender2) {
			this.gender2 = gender2;
		}

		@Embedded
		public Component getComponent() {
			return component;
		}

		public void setComponent(Component component) {
			this.component = component;
		}
	}

	@Embeddable
	static class SubComponent {
		private String subAttribute1;
		private String subAttribute2;

		public SubComponent() {
		}

		public SubComponent(String subAttribute1, String subAttribute2) {
			this.subAttribute1 = subAttribute1;
			this.subAttribute2 = subAttribute2;
		}

		public String getSubAttribute1() {
			return subAttribute1;
		}

		public void setSubAttribute1(String subAttribute1) {
			this.subAttribute1 = subAttribute1;
		}

		public String getSubAttribute2() {
			return subAttribute2;
		}

		public void setSubAttribute2(String subAttribute2) {
			this.subAttribute2 = subAttribute2;
		}
	}

	@Embeddable
	public static class Component {
		private String attribute1;
		private String attribute2;

		private SubComponent subComponent;

		public Component() {
		}

		public Component(String attribute1, String attribute2) {
			this.attribute1 = attribute1;
			this.attribute2 = attribute2;
		}

		public String getAttribute1() {
			return attribute1;
		}

		public void setAttribute1(String attribute1) {
			this.attribute1 = attribute1;
		}

		public String getAttribute2() {
			return attribute2;
		}

		public void setAttribute2(String attribute2) {
			this.attribute2 = attribute2;
		}

		@Embedded
		public SubComponent getSubComponent() {
			return subComponent;
		}

		public void setSubComponent(SubComponent subComponent) {
			this.subComponent = subComponent;
		}
	}
}
