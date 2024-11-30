/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.hbm.notfound;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.orm.test.unconstrained.UnconstrainedTest;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @see UnconstrainedTest
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(settings = @Setting(name= MappingSettings.TRANSFORM_HBM_XML, value = "true"))
@DomainModel(xmlMappings = "mappings/models/hbm/notfound/Person2.hbm.xml")
@SessionFactory
public class HbmNotFoundTransformationTests {
	@Test
	void testNotFoundTransformation(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Employee2 employee = new Employee2( 1, "employee" );
			final Person2 person = new Person2( 1, "person", employee );
			session.persist( employee );
			session.persist( person );
		} );
		scope.inTransaction( (session) -> session.createMutationQuery( "delete Employee2" ).executeUpdate() );
		scope.inTransaction( (session) -> {
			final Person2 loaded = session.find( Person2.class, 1 );
			assertThat( loaded ).isNotNull();
			assertThat( loaded.getEmployee() ).isNull();
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createMutationQuery( "delete Person2" ).executeUpdate();
			session.createMutationQuery( "delete Employee2" ).executeUpdate();
		} );
	}
}
