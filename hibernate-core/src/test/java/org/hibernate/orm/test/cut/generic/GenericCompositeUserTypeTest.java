package org.hibernate.orm.test.cut.generic;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;


@TestForIssue(jiraKey = "HHH-17019")
@DomainModel(
		annotatedClasses = {
				GenericCompositeUserTypeEntity.class
		}
)
@SessionFactory
public class GenericCompositeUserTypeTest {

	@Test
	@DisabledIfSystemProperty(named = "java.vm.name", matches = "\\b.*OpenJ9.*\\b", disabledReason = "https://github.com/eclipse-openj9/openj9/issues/19369")
	public void hhh17019Test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EnumPlaceholder<Weekdays, Weekdays> placeholder = new EnumPlaceholder<>( Weekdays.MONDAY, Weekdays.SUNDAY );
			GenericCompositeUserTypeEntity entity = new GenericCompositeUserTypeEntity( placeholder );

			session.persist( entity );
		} );
	}
}
