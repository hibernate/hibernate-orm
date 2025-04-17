/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.identity.joinedSubClass;

import org.hibernate.dialect.GaussDBDialect;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;


/**
 * @author Andrey Vlasov
 * @author Steve Ebersole
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@DomainModel(annotatedClasses = Sub.class)
@SessionFactory
public class JoinedSubclassHierarchyWithIdentityGenerationTest {
	@Test
	@SkipForDialect(dialectClass = GaussDBDialect.class, reason = "opengauss don't support")
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
