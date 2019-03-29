/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.joins;

import java.util.Date;
import java.util.Locale;
import javax.persistence.PersistenceException;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.mapping.Join;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.query.Query;
import org.hibernate.query.spi.QueryImplementor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Emmanuel Bernard
 */
public class JoinTest extends SessionFactoryBasedFunctionalTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Life.class,
				Death.class,
				Cat.class,
				Dog.class,
				A.class,
				B.class,
				C.class,
				SysGroupsOrm.class,
				SysUserOrm.class,
		};
	}

	@AfterEach
	public void tearDown() {
		inTransaction(
				session -> {
					session.createQuery( "delete from Life" ).executeUpdate();
					session.createQuery( "delete from Death" ).executeUpdate();
					session.createQuery( "delete from Cat" ).executeUpdate();
					session.createQuery( "delete from Dog" ).executeUpdate();
					session.createQuery( "delete from B" ).executeUpdate();
					session.createQuery( "delete from C" ).executeUpdate();
					session.createQuery( "delete from SysGroupsOrm" ).executeUpdate();
					session.createQuery( "delete from sys_user" ).executeUpdate();
				}
		);
	}

	@Test
	public void testDelete() {
		inTransaction(
				session -> {
					session.createQuery( "delete from Life" ).executeUpdate();
				}
		);
	}

	@Test
	public void testDefaultValue() {
		Join join = (Join) getMetadata().getEntityBinding( Life.class.getName() ).getJoinClosureIterator().next();
		MappedTable joinTable = join.getMappedTable();
		assertThat(  joinTable.getName(), is("ExtendedLife") );
		org.hibernate.mapping.Column owner = new org.hibernate.mapping.Column(
				joinTable.getNameIdentifier(),
				"LIFE_ID",
				false
		);
		assertTrue( joinTable.getPrimaryKey().getColumns().contains( owner ) );
		inTransaction(
				session -> {
					Life life = new Life();
					life.duration = 15;
					life.fullDescription = "Long long description";
					session.persist( life );
				} );

		inTransaction(
				session -> {
					Query q = session.createQuery( "from " + Life.class.getName() );
					Life life = (Life) q.uniqueResult();
					assertThat( life.fullDescription, is( "Long long description" ) );
				} );
	}

	@Test
	public void testCompositePK() {
		Join join = (Join) getMetadata().getEntityBinding( Dog.class.getName() ).getJoinClosureIterator().next();
		MappedTable joinTable = join.getMappedTable();
		assertThat( joinTable.getName(), is( "DogThoroughbred" ) );
		org.hibernate.mapping.Column owner = new org.hibernate.mapping.Column(
				joinTable.getNameIdentifier(),
				"OWNER_NAME",
				false
		);
		assertTrue( joinTable.getPrimaryKey().getColumns().contains( owner ) );

		inTransaction(
				session -> {
					Dog dog = new Dog();
					DogPk id = new DogPk();
					id.name = "Thalie";
					id.ownerName = "Martine";
					dog.id = id;
					dog.weight = 30;
					dog.thoroughbredName = "Colley";
					session.persist( dog );

					Dog dog2 = new Dog();
					DogPk dog2id = new DogPk();
					dog2id.name = "Pluto";
					dog2id.ownerName = "Mickey";
					dog2.id = dog2id;
					dog2.weight = 5;
					dog2.thoroughbredName = "Bloodhound";
					session.persist( dog2 );
				}
		);

		inTransaction(
				session -> {
					Query q = session.createQuery( "from Dog where weight = 30" );
					Dog dog = (Dog) q.uniqueResult();
					assertThat( dog.thoroughbredName, is( "Colley" ) );
				}
		);
	}

	@Test
	public void testExplicitValue() {
		inTransaction(
				session -> {
					Death death = new Death();
					death.date = new Date();
					death.howDoesItHappen = "Well, haven't seen it";
					session.persist( death );
				} );

		inTransaction(
				session -> {
					Query q = session.createQuery( "from " + Death.class.getName() );
					Death death = (Death) q.uniqueResult();
					assertThat( death.howDoesItHappen, is( "Well, haven't seen it" ) );
					session.delete( death );
				} );
	}

	@Test
	@Disabled("the generated query 'from Life l where l.owner.name = :name' is not correct")
	public void testManyToOne() {
		inTransaction(
				session -> {
					Life life = new Life();
					Cat cat = new Cat();
					cat.setName( "kitty" );
					cat.setStoryPart2( "and the story continues" );
					life.duration = 15;
					life.fullDescription = "Long long description";
					life.owner = cat;
					session.persist( life );
				} );

		inTransaction(
				session -> {
					QueryImplementor query = session.createQuery( "from Life l where l.owner.name = :name" );
					query.setParameter( "name", "kitty" );
					Life life = (Life) query.uniqueResult();
					assertThat( life.fullDescription, is( "Long long description" ) );
					session.delete( life.owner );
					session.delete( life );
				} );
	}

	@Test
	public void testReferenceColumnWithBacktics() {
		inTransaction(
				session -> {
					SysUserOrm u = new SysUserOrm();
					session.save( u );
				}
		);
	}

	@Test
	public void testUniqueConstaintOnSecondaryTable() {
		Cat cat = new Cat();
		cat.setStoryPart2( "My long story" );
		Cat cat2 = new Cat();
		cat2.setStoryPart2( "My long story" );
		try {
			inTransaction(
					session -> {
						session.persist( cat );
						session.persist( cat2 );
					} );
			fail( "unique constraints violation on secondary table" );
		}
		catch (PersistenceException e) {
			assertThat( e.getCause(), instanceOf( ConstraintViolationException.class ) );
			//success
		}

		inTransaction(
				session -> {
					session.createQuery( "delete from Cat" ).executeUpdate();
				}
		);
	}

	@Test
	public void testFetchModeOnSecondaryTable() {
		Cat cat = new Cat();
		cat.setStoryPart2( "My long story" );
		inTransaction(
				session -> {

					session.persist( cat );
					session.flush();
					session.clear();

					session.get( Cat.class, cat.getId() );
					//Find a way to test it, I need to define the secondary table on a subclass
				} );
	}

	@Test
	@Disabled(value = "customSQL has not been yet implemented")
	public void testCustomSQL() {
		final Cat cat = new Cat();
		String storyPart2 = "My long story";
		cat.setStoryPart2( storyPart2 );
		inTransaction(
				session -> {

					session.persist( cat );
					session.flush();
					session.clear();

					Cat c = session.get( Cat.class, cat.getId() );
					assertThat( c.getStoryPart2(), is( storyPart2.toUpperCase( Locale.ROOT ) ) );

				} );
	}

	@Test
	@Disabled(value = "MappedSuperclass support has not yet been implemented ")
	public void testMappedSuperclassAndSecondaryTable() {
		inTransaction(
				session -> {
					C c = new C();
					c.setAge( 12 );
					c.setCreateDate( new Date() );
					c.setName( "Bob" );
					session.persist( c );
					session.flush();
					session.clear();
					c = session.get( C.class, c.getId() );
					assertNotNull( c.getCreateDate() );
					assertNotNull( c.getName() );
				} );
	}

//	@Override
//	protected void configureMetadataBuilder(MetadataBuilder metadataBuilder) {
//		super.configureMetadataBuilder( metadataBuilder );
//		metadataBuilder.applyImplicitNamingStrategy( ImplicitNamingStrategyLegacyJpaImpl.INSTANCE );
//	}

}
