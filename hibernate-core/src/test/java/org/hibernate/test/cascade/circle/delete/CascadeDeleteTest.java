package org.hibernate.test.cascade.circle.delete;

import java.util.List;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

@TestForIssue(jiraKey = "HHH-15218")
public class CascadeDeleteTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{
				Address.class,
				Person.class
		};
	}

	@Before
	public void setUp() {
		inTransaction(
				session -> {
					Person person = new Person();

					Address currentAddress = new Address( "Localita S. Egidio Gradoli (VT)" );
					person.addCurrentAddress( currentAddress );

					session.persist( person );
				}
		);
	}

	@Test
	public void testDelete() {
		inTransaction(
				session -> {
					List<Person> people = session.createQuery( "from Person", Person.class ).list();
					people.forEach( person -> {
						session.remove( person );
					} );
				}
		);
	}
}
