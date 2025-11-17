/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.join;

import jakarta.persistence.PersistenceException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.mapping.Join;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				Life.class,
				Death.class,
				Cat.class,
				Dog.class,
				A.class,
				B.class,
				C.class,
				SysGroupsOrm.class,
				SysUserOrm.class}
)
@SessionFactory
@ServiceRegistry(
		settingProviders = @SettingProvider(settingName = AvailableSettings.IMPLICIT_NAMING_STRATEGY,
				provider = JoinTest.NamingStrategyProvider.class)
)
public class JoinTest {

	public static class NamingStrategyProvider implements SettingProvider.Provider<String> {

		@Override
		public String getSetting() {
			return ImplicitNamingStrategyLegacyJpaImpl.INSTANCE.getClass().getName();
		}
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testDefaultValue(SessionFactoryScope scope) {
		Join join = scope.getMetadataImplementor().getEntityBinding( Life.class.getName() ).getJoinClosure().get( 0 );
		assertThat( join.getTable().getName() ).isEqualTo( "ExtendedLife" );
		org.hibernate.mapping.Column owner = new org.hibernate.mapping.Column();
		owner.setName( "LIFE_ID" );
		assertThat( join.getTable().getPrimaryKey().containsColumn( owner ) ).isTrue();
		scope.inTransaction(
				session -> {
					Life life = new Life();
					life.duration = 15;
					life.fullDescription = "Long long description";
					session.persist( life );
				}
		);

		scope.inTransaction(
				session -> {
					Life life = session.createQuery( "from " + Life.class.getName(), Life.class ).uniqueResult();
					assertThat( life.fullDescription ).isEqualTo( "Long long description" );
				}
		);
	}

	@Test
	public void testCompositePK(SessionFactoryScope scope) {
		Join join = scope.getMetadataImplementor().getEntityBinding( Dog.class.getName() ).getJoinClosure().get( 0 );
		assertThat( join.getTable().getName() ).isEqualTo( "DogThoroughbred" );
		org.hibernate.mapping.Column owner = new org.hibernate.mapping.Column();
		owner.setName( "OWNER_NAME" );
		assertThat( join.getTable().getPrimaryKey().containsColumn( owner ) ).isTrue();

		scope.inTransaction(
				session -> {
					Dog dog = new Dog();
					DogPk id = new DogPk();
					id.name = "Thalie";
					id.ownerName = "Martine";
					dog.id = id;
					dog.weight = 30;
					dog.thoroughbredName = "Colley";
					session.persist( dog );
				}
		);

		scope.inTransaction(
				session -> {
					Dog dog = session.createQuery( "from Dog", Dog.class ).uniqueResult();
					assertThat( dog.thoroughbredName ).isEqualTo( "Colley" );
				}
		);
	}

	@Test
	public void testExplicitValue(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Death death = new Death();
					death.date = new Date();
					death.howDoesItHappen = "Well, haven't seen it";
					session.persist( death );
				}
		);

		scope.inTransaction(
				session -> {
					Death death = session.createQuery( "from " + Death.class.getName(), Death.class ).uniqueResult();
					assertThat( death.howDoesItHappen ).isEqualTo( "Well, haven't seen it" );
					session.remove( death );
				}
		);
	}

	@Test
	public void testManyToOne(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Life life = new Life();
					Cat cat = new Cat();
					cat.setName( "kitty" );
					cat.setStoryPart2( "and the story continues" );
					life.duration = 15;
					life.fullDescription = "Long long description";
					life.owner = cat;
					session.persist( life );
				}
		);

		scope.inTransaction(
				session -> {
					CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
					CriteriaQuery<Life> criteria = criteriaBuilder.createQuery( Life.class );
					Root<Life> root = criteria.from( Life.class );
					jakarta.persistence.criteria.Join<Object, Object> owner = root.join( "owner", JoinType.INNER );
					criteria.where( criteriaBuilder.equal( owner.get( "name" ), "kitty" ) );
					Life life = session.createQuery( criteria ).uniqueResult();

//		Criteria crit = s.createCriteria( Life.class );
//		crit.createCriteria( "owner" ).add( Restrictions.eq( "name", "kitty" ) );
//		life = (Life) crit.uniqueResult();
					assertThat( life.fullDescription ).isEqualTo( "Long long description" );
					session.remove( life.owner );
					session.remove( life );
				}
		);
	}

	@Test
	public void testReferenceColumnWithBacktics(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SysGroupsOrm g = new SysGroupsOrm();
					SysUserOrm u = new SysUserOrm();
					u.setGroups( new ArrayList<>() );
					u.getGroups().add( g );
					session.persist( g );
					session.persist( u );
				}
		);
	}

	@Test
	public void testUniqueConstaintOnSecondaryTable(SessionFactoryScope scope) {
		Cat cat = new Cat();
		cat.setStoryPart2( "My long story" );
		Cat cat2 = new Cat();
		cat2.setStoryPart2( "My long story" );
		PersistenceException exception = assertThrows( PersistenceException.class, () -> scope.inTransaction(
						session -> {
							session.persist( cat );
							session.persist( cat2 );
						}
				),
				"Expected unique constraints violation on secondary table"
		);

		assertThat( exception ).isInstanceOf( ConstraintViolationException.class );

	}

	@Test
	public void testFetchModeOnSecondaryTable(SessionFactoryScope scope) {
		Cat cat = new Cat();
		cat.setStoryPart2( "My long story" );
		scope.inTransaction(
				session -> {
					session.persist( cat );
					session.flush();
					session.clear();

					session.find( Cat.class, cat.getId() );
					//Find a way to test it, I need to define the secondary table on a subclass
				}
		);
	}

	@Test
	public void testCustomSQL(SessionFactoryScope scope) {
		Cat cat = new Cat();
		String storyPart2 = "My long story";
		cat.setStoryPart2( storyPart2 );

		scope.inTransaction(
				session -> {
					session.persist( cat );
					session.flush();
					session.clear();

					Cat c = session.find( Cat.class, cat.getId() );
					assertThat( c.getStoryPart2() ).isEqualTo( storyPart2.toUpperCase( Locale.ROOT ) );
				}
		);
	}

	@Test
	public void testMappedSuperclassAndSecondaryTable(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					C c = new C();
					c.setAge( 12 );
					c.setCreateDate( new Date() );
					c.setName( "Bob" );
					session.persist( c );
					session.flush();
					session.clear();
					c = session.find( C.class, c.getId() );
					assertThat( c.getCreateDate() ).isNotNull();
					assertThat( c.getName() ).isNotNull();
				}
		);
	}
}
