/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.idgen.identity.joinedSubClass;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;


/**
 * @author Andrey Vlasov
 * @author Steve Ebersole
 */
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsIdentityColumns.class )
@DomainModel( annotatedClasses = Sub.class )
@SessionFactory
public class JoinedSubclassHierarchyWithIdentityGenerationTest {
	@Test
	public void shouldPersistDebtorAccountWhenParentServiceAgreementPersisted(SessionFactoryScope scope) {
		scope.inTransaction(
				(s) -> {
					s.persist( new Sub() );
				}
		);
	}

	@AfterEach
	public void cleanTestData(SessionFactoryScope scope) {
		scope.inTransaction( (s) -> s.createQuery( "delete Sub" ).executeUpdate() );
	}
}
