package org.hibernate.orm.test.cascade.circle.delete;

import java.util.List;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@DomainModel(
		annotatedClasses = {
				Person.class,
				Address.class
		}
)
@SessionFactory
@TestForIssue(jiraKey = "HHH-15218")
public class CascadeDeleteTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person person = new Person();

					Address currentAddress = new Address( "Localita S. Egidio Gradoli (VT)" );
					person.addCurrentAddress( currentAddress );

					session.persist( person );
				}
		);
	}

	@Test
	public void testDelete(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Person> people = session.createSelectionQuery( "from Person", Person.class ).list();
					people.forEach( person -> {
						session.remove( person );
					} );
				}
		);
	}
}
