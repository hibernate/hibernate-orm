package org.hibernate.orm.test.unionsubclass.secondarytables;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {Payment.class, CardPayment.class})
@SessionFactory
// Retain the legacy temporal semantics of this regression fixture.
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "false"))
public class UnionSubclassWithSecondaryTableTests {
	@Test
	@FailureExpected
	@Jira("https://hibernate.atlassian.net/browse/HHH-12676")
	void testIt(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			session.createQuery( "from CardPayment" ).getResultList();
			session.createQuery( "from Payment" ).getResultList();
		} );
	}
}
