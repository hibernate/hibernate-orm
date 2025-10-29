/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.community.dialect.FirebirdDialect;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

/**
 * @author Vlad Mihalcea
 * @author Gail Badner
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@SkipForDialect(dialectClass = DB2Dialect.class,
		reason = "DB2 is far more resistant to the reserved keyword usage. See HHH-12832.")
@SkipForDialect(dialectClass = DerbyDialect.class,
		reason = "Derby is far more resistant to the reserved keyword usage.")
@SkipForDialect(dialectClass = FirebirdDialect.class,
		reason = "FirebirdDialect has autoQuoteKeywords enabled, so it is far more resistant to the reserved keyword usage.")
@SkipForDialect(dialectClass = AltibaseDialect.class,
		reason = "AltibaseDialect has autoQuoteKeywords enabled, so it is far more resistant to the reserved keyword usage.")
@SkipForDialect(dialectClass = InformixDialect.class,
		reason = "Informix is far more resistant to the reserved keyword usage.")
@ServiceRegistry
@DomainModel(annotatedClasses = SchemaUpdateHaltOnErrorTest.From.class)
public class SchemaUpdateHaltOnErrorTest {
	@Test
	public void testHaltOnError(DomainModelScope modelScope) {
		var model = modelScope.getDomainModel();
		model.orderColumns( false );
		model.validate();

		try {
			new SchemaUpdate()
					.setHaltOnError( true )
					.execute( EnumSet.of( TargetType.DATABASE ), model );
			Assertions.fail( "Should halt on error!" );
		}
		catch ( Exception e ) {
			SchemaManagementException cause = (SchemaManagementException) e;
			Assertions.assertTrue( cause.getMessage().startsWith( "Halting on error : Error executing DDL" ) );
		}
	}

	@Entity(name = "From")
	public static class From {

		@Id
		private Integer id;

		private String table;

		private String select;
	}
}
