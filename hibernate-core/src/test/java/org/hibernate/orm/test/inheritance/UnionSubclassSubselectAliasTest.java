package org.hibernate.orm.test.inheritance;

import org.hibernate.annotations.Subselect;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Derived tables in the union of a {@link InheritanceType#TABLE_PER_CLASS} hierarchy
 * whose subclasses are mapped with {@link Subselect} must be aliased, because
 * databases like MySQL, MariaDB and SQL Server reject unaliased derived tables.
 */
@Jira("https://hibernate.atlassian.net/browse/HHH-21071")
@DomainModel(
		annotatedClasses = {
				UnionSubclassSubselectAliasTest.Animal.class,
				UnionSubclassSubselectAliasTest.Dog.class,
				UnionSubclassSubselectAliasTest.Cat.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
// H2 accepts unaliased derived tables, so the generated SQL is asserted instead; it is H2 specific
@RequiresDialect(H2Dialect.class)
class UnionSubclassSubselectAliasTest {

	@BeforeEach
	void createTables(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createNativeMutationQuery( "create table dog_t (id bigint primary key, name varchar(50))" )
					.executeUpdate();
			session.createNativeMutationQuery( "create table cat_t (id bigint primary key, name varchar(50))" )
					.executeUpdate();
		} );
	}

	@AfterEach
	void dropTables(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createNativeMutationQuery( "drop table dog_t" ).executeUpdate();
			session.createNativeMutationQuery( "drop table cat_t" ).executeUpdate();
		} );
	}

	@Test
	void testUnionOfAllSubselects(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			inspector.clear();
			session.createSelectionQuery( "from Animal", Animal.class ).getResultList();
			inspector.assertExecutedCount( 1 );
			assertThat( inspector.getSqlQueries().get( 0 ) )
					.contains( "from ( select id, name from dog_t ) sub_ union all" )
					.contains( "from ( select id, name from cat_t ) sub_)" );
		} );
	}

	@Test
	void testPrunedUnion(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			inspector.clear();
			session.createSelectionQuery( "from Animal a where type(a) = Dog", Animal.class ).getResultList();
			inspector.assertExecutedCount( 1 );
			assertThat( inspector.getSqlQueries().get( 0 ) )
					.contains( "from ( select id, name from dog_t ) sub_)" )
					.doesNotContain( "cat_t" );
		} );
	}

	@Entity(name = "Animal")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static abstract class Animal {
		@Id
		Long id;
		String name;
	}

	@Entity(name = "Dog")
	@Subselect("select id, name from dog_t")
	public static class Dog extends Animal {
	}

	@Entity(name = "Cat")
	@Subselect("select id, name from cat_t")
	public static class Cat extends Animal {
	}
}
