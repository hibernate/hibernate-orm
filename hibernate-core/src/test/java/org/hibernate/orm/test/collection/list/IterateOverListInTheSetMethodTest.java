package org.hibernate.orm.test.collection.list;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@DomainModel(
		xmlMappings = "org/hibernate/orm/test/collection/list/ParentChildMapping.hbm.xml"
)
@SessionFactory
public class IterateOverListInTheSetMethodTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Child child = new Child( 1, "Luigi" );
					Child child2 = new Child( 2, "Franco" );
					Parent parent = new Parent( 2, "Fabio" );
					parent.addChild( child );
					parent.addChild( child2 );

					session.persist( parent );
					session.persist( child );
					session.persist( child2 );
				}
		);
	}

	@Test
	public void testHqlQuery(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					session.createQuery( "select p from Parent p" ).list();
				}
		);
	}
}
