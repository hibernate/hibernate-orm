/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.transaction.TransactionUtil;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.EnumSet;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Yanming Zhou
 */
@JiraKey("HHH-18784")
@RequiresDialect(H2Dialect.class)
public class ColumnLengthTest extends BaseCoreFunctionalTestCase {

	private File output;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { TestEntity.class };
	}

	@Before
	public void setUp() throws IOException {
		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.createNativeQuery( "DROP TABLE IF EXISTS en CASCADE" ).executeUpdate();
			session.createNativeQuery(
					"CREATE TABLE en ( " +
							"  `ID` integer NOT NULL, " +
							"  `TEXT` varchar(1000), " +
							"  `DECIMAL` numeric(38, 4), " +
							"   PRIMARY KEY (`ID`)" +
							")" )
					.executeUpdate();
		} );
	}

	@After
	public void tearDown() {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.createNativeQuery( "DROP TABLE en CASCADE" ).executeUpdate();
		} );
		output.delete();
	}

	@Test
	public void testUpdateIsNotExecuted() throws Exception {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.build();
		try {
			final MetadataSources metadataSources = new MetadataSources( ssr );
			metadataSources.addAnnotatedClass( TestEntity.class );
			MetadataImplementor metadata = (MetadataImplementor) metadataSources.buildMetadata();
			metadata.orderColumns( false );
			new SchemaUpdate()
					.setHaltOnError( true )
					.setOutputFile( output.getAbsolutePath() )
					.setFormat( false )
					.execute( EnumSet.of( TargetType.SCRIPT, TargetType.DATABASE ), metadata );

			String fileContent = new String( Files.readAllBytes( output.toPath() ) );
			assertThat( fileContent ).isEmpty();
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}


	@Entity(name = "en")
	@Table(name = "en")
	public static class TestEntity {
		@Id
		private Integer id;

		private String text;

		private BigDecimal decimal;
	}
}
