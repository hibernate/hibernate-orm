/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.softdelete.converter.reversed;

import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.orm.test.softdelete.MappingVerifier;
import org.hibernate.orm.test.softdelete.ReverseYesNoConverter;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = { ReverseYesNoConverter.class, TheEntity.class })
@SessionFactory( useCollectingStatementInspector = true)
public class ReversedSoftDeleteTests {
	@Test
	void verifySchema(SessionFactoryScope scope) {
		final MappingMetamodelImplementor metamodel = scope.getSessionFactory().getMappingMetamodel();
		MappingVerifier.verifyMapping(
				metamodel.getEntityDescriptor( TheEntity.class ).getSoftDeleteMapping(),
				"active",
				"the_entity",
				'N'
		);
	}

	@Test
	void testUsage(SessionFactoryScope scope) {
		final SQLStatementInspector sqlInspector = scope.getCollectingStatementInspector();
		sqlInspector.clear();

		scope.inTransaction( (session) -> {
			session.persist( new TheEntity( 1, "it" ) );
		} );

		assertThat( sqlInspector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlInspector.getSqlQueries().get( 0 ) ).contains( "'Y'" );
		assertThat( sqlInspector.getSqlQueries().get( 0 ) ).doesNotContain( "'N'" );

		sqlInspector.clear();

		scope.inTransaction( (session) -> {
			final TheEntity reference = session.getReference( TheEntity.class, 1 );
			session.remove( reference );
		} );

		assertThat( sqlInspector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlInspector.getSqlQueries().get( 0 ) ).doesNotContainIgnoringCase( "delete " );
		assertThat( sqlInspector.getSqlQueries().get( 0 ) ).containsIgnoringCase( "update " );
		assertThat( sqlInspector.getSqlQueries().get( 0 ) ).contains( "active='N'" );
	}
}
