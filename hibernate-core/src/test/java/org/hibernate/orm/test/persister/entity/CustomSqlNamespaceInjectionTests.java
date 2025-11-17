/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.persister.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.LockMode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLSelect;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.MutationCoordinator;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.JdbcSettings.DIALECT;
import static org.hibernate.cfg.MappingSettings.DEFAULT_CATALOG;
import static org.hibernate.cfg.MappingSettings.DEFAULT_SCHEMA;
import static org.hibernate.engine.jdbc.env.spi.NameQualifierSupport.BOTH;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialect(
		value = H2Dialect.class,
		comment = "Actual database being tested is irrelevant, so only run for the default"
)
@ServiceRegistry( settings = {
		@Setting( name = DEFAULT_SCHEMA, value="my_schema" ),
		@Setting( name = DEFAULT_CATALOG, value="my_catalog" ),
		@Setting( name = DIALECT, value = "org.hibernate.orm.test.persister.entity.CustomSqlNamespaceInjectionTests$CustomDialect" )
} )
public class CustomSqlNamespaceInjectionTests {
	@Test
	@DomainModel(annotatedClasses = CustomSchemaEntity.class)
	@SessionFactory(exportSchema = false, useCollectingStatementInspector = true)
	void testSchemaReplacement(SessionFactoryScope sessionFactoryScope) {
		verifyReplacements( sessionFactoryScope, CustomSchemaEntity.class, "my_schema.the_table" );
	}

	@Test
	@DomainModel(annotatedClasses = CustomCatalogEntity.class)
	@SessionFactory(exportSchema = false, useCollectingStatementInspector = true)
	void testCatalogReplacement(SessionFactoryScope sessionFactoryScope) {
		verifyReplacements( sessionFactoryScope, CustomCatalogEntity.class, "my_catalog.the_table" );
	}

	@Test
	@DomainModel(annotatedClasses = CustomDomainEntity.class)
	@SessionFactory(exportSchema = false, useCollectingStatementInspector = true)
	void testDomainReplacement(SessionFactoryScope sessionFactoryScope) {
		verifyReplacements( sessionFactoryScope, CustomDomainEntity.class, "my_catalog.my_schema.the_table" );
	}

	private void verifyReplacements(
			SessionFactoryScope sessionFactoryScope,
			Class<?> entityClass,
			String expectedTableName) {
		final SessionFactoryImplementor sessionFactory = sessionFactoryScope.getSessionFactory();

		final MappingMetamodelImplementor mappingMetamodel = sessionFactory.getMappingMetamodel();
		final EntityPersister persister = mappingMetamodel.getEntityDescriptor( entityClass );

		verifySelectSql( sessionFactoryScope, persister, expectedTableName );
		verifyDmlSql( sessionFactoryScope, persister, expectedTableName );
	}

	private static void verifyDmlSql(SessionFactoryScope sessionFactoryScope, EntityPersister persister, String expectedTableName) {
		verifyDmlSql( persister.getInsertCoordinator(), expectedTableName );
		verifyDmlSql( persister.getUpdateCoordinator(), expectedTableName );
		verifyDmlSql( persister.getDeleteCoordinator(), expectedTableName );
	}

	private static void verifyDmlSql(MutationCoordinator mutationCoordinator, String expectedTableName) {
		final MutationOperationGroup mutationOperationGroup = mutationCoordinator.getStaticMutationOperationGroup();
		final MutationOperation mutationOperation = mutationOperationGroup.getSingleOperation();
		final String sql = ( (JdbcMutationOperation) mutationOperation ).getSqlString();
		assertThat( sql ).contains( expectedTableName );
	}

	private static void verifySelectSql(SessionFactoryScope sessionFactoryScope, EntityPersister persister, String expectedTableName) {
		final SQLStatementInspector sqlStatementCollector = sessionFactoryScope.getCollectingStatementInspector();
		sqlStatementCollector.clear();

		try {
			sessionFactoryScope.inTransaction( (session) -> {
				persister.load( 1, null, LockMode.NONE, session );
			} );
		}
		catch (Exception ignore) {
		}

		assertThat( sqlStatementCollector.getSqlQueries() ).hasSize( 1 );
		final String query = sqlStatementCollector.getSqlQueries().get( 0 );
		assertThat( query ).contains( expectedTableName );
	}

	@Entity(name = "CustomSchemaEntity")
	@SQLSelect(sql = "select id, name from {h-schema}the_table where id = ?")
	@SQLInsert(sql = "insert into {h-schema}the_table (name) values (?)")
	@SQLDelete(sql = "delete from {h-schema}the_table where id = ?")
	@SQLUpdate(sql = "update {h-schema}the_table set name = ? where id = ? ")
	public static class CustomSchemaEntity {
		@Id
		public Integer id;
		private String name;
	}

	@Entity(name = "CustomCatalogEntity")
	@SQLSelect(sql = "select id, name from {h-catalog}the_table where id = ?")
	@SQLInsert(sql = "insert into {h-catalog}the_table (name) values (?)")
	@SQLDelete(sql = "delete from {h-catalog}the_table where id = ?")
	@SQLUpdate(sql = "update {h-catalog}the_table set name = ? where id = ? ")
	public static class CustomCatalogEntity {
		@Id
		public Integer id;
		private String name;
	}

	@Entity(name = "CustomDomainEntity")
	@SQLSelect(sql = "select id, name from {h-domain}the_table where id = ?")
	@SQLInsert(sql = "insert into {h-domain}the_table (name) values (?)")
	@SQLDelete(sql = "delete from {h-domain}the_table where id = ?")
	@SQLUpdate(sql = "update {h-domain}the_table set name = ? where id = ? ")
	public static class CustomDomainEntity {
		@Id
		public Integer id;
		private String name;
	}

	public static class CustomDialect extends H2Dialect {
		@Override
		public NameQualifierSupport getNameQualifierSupport() {
			return BOTH;
		}
	}
}
