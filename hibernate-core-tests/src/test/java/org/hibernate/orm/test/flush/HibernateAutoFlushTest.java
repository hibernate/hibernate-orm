package org.hibernate.orm.test.flush;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.QueryFlushMode;
import org.hibernate.FlushMode;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {
		HibernateAutoFlushTest.Person.class,
		HibernateAutoFlushTest.Advertisement.class
})
@SessionFactory
public class HibernateAutoFlushTest {
	private final Logger log  = Logger.getLogger( HibernateAutoFlushTest.class );

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testFlushAutoSQLNativeSession(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			log.info("testFlushAutoSQLNativeSession");
			//tag::flushing-auto-flush-sql-native-example[]
			Assertions.assertEquals( 0, (int) session.createNativeQuery( "select count(*) from Person", Integer.class )
					.getSingleResult() );

			Person person = new Person("John Doe");
			session.persist(person);

			Assertions.assertEquals( 0, (int) session.createNativeQuery( "select count(*) from Person", Integer.class )
					.uniqueResult() );
			//end::flushing-auto-flush-sql-native-example[]
		});
	}

	@Test
	void synchronizedSpacesControlFlush(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Person( "pending" ) );
			Assertions.assertEquals( 0, session.createNativeQuery( "select count(*) from Advertisement", Integer.class )
					.addSynchronizedEntityClass( Advertisement.class ).getSingleResult() );
			Assertions.assertEquals( 0, session.createNativeQuery( "select count(*) from Person", Integer.class )
					.getSingleResult() );
			Assertions.assertEquals( 1, session.createNativeQuery( "select count(*) from Person", Integer.class )
					.addSynchronizedEntityClass( Person.class ).getSingleResult() );
		} );
	}

	@Test
	void queryOverridesNativeBootstrapMode(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.setHibernateFlushMode( FlushMode.ALWAYS );
			session.persist( new Person( "pending" ) );
			Assertions.assertEquals( 0, session.createNativeQuery( "select count(*) from Person", Integer.class )
					.setQueryFlushMode( QueryFlushMode.NO_FLUSH ).getSingleResult() );
			session.setHibernateFlushMode( FlushMode.COMMIT );
			Assertions.assertEquals( 1, session.createNativeQuery( "select count(*) from Person", Integer.class )
					.setQueryFlushMode( QueryFlushMode.FLUSH ).getSingleResult() );
			Assertions.assertEquals( FlushMode.COMMIT, session.getHibernateFlushMode() );
		} );
	}

	//tag::flushing-auto-flush-jpql-entity-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public Person() {}

		public Person(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

	}

	@Entity(name = "Advertisement")
	public static class Advertisement {

		@Id
		@GeneratedValue
		private Long id;

		private String title;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	}
	//end::flushing-auto-flush-jpql-entity-example[]
}
