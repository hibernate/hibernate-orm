/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.xml.column.transform;

import java.sql.ResultSet;
import java.sql.Statement;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
@DomainModel(xmlMappings = "mappings/models/column/transform/mapping.xml")
@SessionFactory
@SuppressWarnings("JUnitMalformedDeclaration")
public class SimpleUsageTests {
	@Test
	void testTransformation(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new Item( 1, "primo", 123.45 ) );
		} );

		scope.inTransaction( (session) -> {
			session.doWork( (connection) -> {
				final Statement statement = connection.createStatement();
				final ResultSet resultSet = statement.executeQuery( "select cost from items where id = 1" );
				assertThat( resultSet.next() ).isTrue();
				assertThat( resultSet.getInt( 1 ) ).isEqualTo( 12345 );
			} );
		} );

		scope.inTransaction( (session) -> {
			final Item loaded = session.find( Item.class, 1 );
			assertThat( loaded.getCost() ).isEqualTo( 123.45 );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createMutationQuery( "delete Item" ).executeUpdate();
		} );
	}
}
