/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.hbm.foreigngenerator;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {Thing.class, Info.class})
@SessionFactory
public class ForeignGeneratorTests {
	@Test
	void checkGeneration(SessionFactoryScope sessionFactoryScope) {
		sessionFactoryScope.inTransaction( (session) -> {
			final Thing thing = new Thing( 1, "thing-1" );
			final Info info = new Info( thing, "info-1" );
			thing.setInfo( info );
			info.setOwner( thing );
			session.persist( thing );
			session.persist( info );
		} );

		sessionFactoryScope.inTransaction( (session) -> {
			final Thing thing = session.find( Thing.class, 1 );
			assertThat( thing.getId() ).isEqualTo( thing.getInfo().getId() );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope sessionFactoryScope) {
		sessionFactoryScope.inTransaction( (session) -> {
			session.createMutationQuery( "delete Info" ).executeUpdate();
			session.createMutationQuery( "delete Thing" ).executeUpdate();
		} );
	}
}
