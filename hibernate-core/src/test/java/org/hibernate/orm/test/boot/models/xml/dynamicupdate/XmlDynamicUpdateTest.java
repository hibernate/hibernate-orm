/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.dynamicupdate;

import java.util.Locale;

import org.hibernate.testing.jdbc.CollectingStatementObserver;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-20774")
@DomainModel(xmlMappings = "org/hibernate/orm/test/boot/models/xml/dynamicupdate/DynamicEntity.orm.xml")
@SessionFactory(useCollectingStatementObserver = true)
public class XmlDynamicUpdateTest {

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testDynamicInsertFromXmlMapping(SessionFactoryScope scope) {
		final CollectingStatementObserver observer = scope.getCollectingStatementObserver();
		observer.clear();

		scope.inTransaction( session -> {
			final DynamicEntity entity = new DynamicEntity();
			entity.setName( "initial" );
			session.persist( entity );
			session.flush();

			final String insertSql = observer.getSqlQueries().stream()
					.filter( sql -> sql.toLowerCase( Locale.ROOT ).startsWith( "insert" ) )
					.findFirst()
					.orElseThrow( () -> new AssertionError( "No insert statement found" ) );

			assertThat( insertSql ).contains( "name" );
			assertThat( insertSql ).doesNotContain( "description" );
			assertThat( insertSql ).doesNotContain( "notes" );
		} );
	}

	@Test
	public void testDynamicUpdateFromXmlMapping(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final DynamicEntity entity = new DynamicEntity();
			entity.setName( "initial" );
			entity.setDescription( "desc" );
			entity.setNotes( "notes" );
			session.persist( entity );
		} );

		final CollectingStatementObserver observer = scope.getCollectingStatementObserver();
		observer.clear();

		scope.inTransaction( session -> {
			final DynamicEntity entity = session
					.createQuery( "from DynamicEntity", DynamicEntity.class )
					.uniqueResult();
			entity.setName( "updated" );
			session.flush();

			final String updateSql = observer.getSqlQueries().stream()
					.filter( sql -> sql.toLowerCase( Locale.ROOT ).startsWith( "update" ) )
					.findFirst()
					.orElseThrow( () -> new AssertionError( "No update statement found" ) );

			assertThat( updateSql ).contains( "name" );
			assertThat( updateSql ).doesNotContain( "description" );
			assertThat( updateSql ).doesNotContain( "notes" );
		} );
	}
}
