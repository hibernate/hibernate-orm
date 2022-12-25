package org.hibernate.orm.test.filter.subclass.joined2;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

@TestForIssue(jiraKey = "HHH-9646")
@SessionFactory
@DomainModel(annotatedClasses = {Animal.class, Dog.class, Owner.class, JoinedInheritanceFilterTest.class})
@FilterDef(name = "companyFilter", parameters = @ParamDef(name = "companyIdParam", type = long.class))
public class JoinedInheritanceFilterTest {
	@Test public void test(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			s.createQuery("SELECT o FROM Owner o INNER JOIN FETCH o.dog d WHERE o.id = 1").getResultList();
			s.enableFilter("companyFilter").setParameter("companyIdParam", 2l).validate();
			s.createQuery("SELECT o FROM Owner o INNER JOIN FETCH o.dog d WHERE o.id = 1").getResultList();
			s.createQuery("FROM Animal").getResultList();
			s.createQuery("FROM Dog").getResultList();
			assertNull( s.find(Owner.class, 1) );
			assertNull( s.find(Animal.class, 1) );
			assertNull( s.find(Dog.class, 1) );
		});
	}
}
