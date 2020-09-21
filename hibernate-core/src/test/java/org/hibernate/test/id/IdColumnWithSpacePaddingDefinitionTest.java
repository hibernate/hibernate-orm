package org.hibernate.test.id;

import java.util.Collections;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Mariano Eloy Fernández Osca
 * @author Nathan Xu
 */
@TestForIssue( jiraKey = "HHH-14222" )
@RequiresDialect( PostgreSQL82Dialect.class )
public class IdColumnWithSpacePaddingDefinitionTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				A.class,
				B.class
		};
	}

	@Before
	public void setUp() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			B b = new B();
			b.id = "b";
			entityManager.persist( b );

			A a = new A();
			a.id = "a";
			a.something = "test";
			a.many = Collections.singletonList( b );
			entityManager.persist( a );
		} );
	}

	@Test
	@FailureExpected( jiraKey = "HHH-14222" )
	public void testFindByPrimaryKeyFailsToGetCollectionOwnerWhenJoinColumnWidthIsFixed() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			A a = entityManager.find( A.class, "a" );
			assertNotNull( a );
			assertEquals( "a", a.id ); // <-- No padding because it's not loaded from database
			assertEquals( "test      ", a.something );
			assertNotNull( a.many );
			assertEquals( 1, a.many.size() ); // <<<---- FAILS

		} );
	}

	@Entity(name = "A")
	static class A {

		@Id
		@Column(columnDefinition = "char(10)")
		String id;

		@Column(columnDefinition = "char(10)")
		String something;

		@OneToMany(cascade = CascadeType.ALL)
		@JoinColumn
		List<B> many;

	}

	@Entity(name = "B")
	static class B {

		@Id
		@Column(columnDefinition = "char(10)")
		String id;

	}
}
