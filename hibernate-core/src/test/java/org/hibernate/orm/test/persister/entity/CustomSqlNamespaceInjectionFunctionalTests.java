/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.persister.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLSelect;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(settings = @Setting( name= AvailableSettings.DEFAULT_SCHEMA, value = "my_schema" ) )
@DomainModel(annotatedClasses = CustomSqlNamespaceInjectionFunctionalTests.CustomEntity.class)
@SessionFactory(useCollectingStatementInspector = true, createSecondarySchemas = true)
@RequiresDialect(H2Dialect.class)
public class CustomSqlNamespaceInjectionFunctionalTests {
	@Test
	void testUsage(SessionFactoryScope sessions) {
		final SQLStatementInspector statementInspector = sessions.getCollectingStatementInspector();

		// create one, verifying the executed INSERT
		statementInspector.clear();
		final Integer generatedId = sessions.fromTransaction( (session) -> {
			final CustomEntity entity = new CustomEntity( "Evets" );
			session.persist( entity );
			session.flush();
			return entity.id;
		} );
		assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
		assertThat( statementInspector.getSqlQueries().get( 0 ) )
				.isEqualTo( "insert into my_schema.the_table (name) values (?)" );

		// modify one, verifying the executed UPDATE.
		// NOTE : because of the find we also get the @SQLSelect performed
		statementInspector.clear();
		sessions.inTransaction( (session) -> {
			final CustomEntity loaded = session.find( CustomEntity.class, generatedId );
			loaded.name = "Steve";
		} );
		assertThat( statementInspector.getSqlQueries() ).hasSize( 2 );
		assertThat( statementInspector.getSqlQueries().get( 0 ) )
				.isEqualTo( "select id, name from my_schema.the_table where id = ?" );
		assertThat( statementInspector.getSqlQueries().get( 1 ) )
				.isEqualTo( "update my_schema.the_table set name = ? where id = ?" );

		// delete one, verifying the executed DELETE
		// NOTE : because of the find we also get the @SQLSelect performed
		statementInspector.clear();
		sessions.inTransaction( (session) -> {
			final CustomEntity loaded = session.find( CustomEntity.class, generatedId );
			session.remove( loaded );
		} );
		assertThat( statementInspector.getSqlQueries() ).hasSize( 2 );
		assertThat( statementInspector.getSqlQueries().get( 0 ) )
				.isEqualTo( "select id, name from my_schema.the_table where id = ?" );
		assertThat( statementInspector.getSqlQueries().get( 1 ) )
				.isEqualTo( "delete from my_schema.the_table where id = ?" );
	}

	@AfterEach
	void tearDown(SessionFactoryScope sessions) {
		sessions.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "CustomEntity")
	@SQLSelect(sql = "select id, name from {h-schema}the_table where id = ?")
	@SQLInsert(sql = "insert into {h-schema}the_table (name) values (?)")
	@SQLDelete(sql = "delete from {h-schema}the_table where id = ?")
	@SQLUpdate(sql = "update {h-schema}the_table set name = ? where id = ? ")
	@Table(name="the_table")
	public static class CustomEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public Integer id;
		private String name;

		public CustomEntity() {
		}

		public CustomEntity(String name) {
			this.name = name;
		}
	}
}
