/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.hibernate.cfg.MappingSettings.GLOBALLY_QUOTED_IDENTIFIERS;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-9849")
@RequiresDialect(MySQLDialect.class)
@ServiceRegistry(settings = @Setting(name = GLOBALLY_QUOTED_IDENTIFIERS, value = "false"))
@DomainModel(annotatedClasses = MixedFieldPropertyAnnotationTest.MyEntity.class)
public class MixedFieldPropertyAnnotationTest {
	@BeforeEach
	void setUp(DomainModelScope modelScope) {
		System.out.println( "********* Starting SchemaExport for START-UP *************************" );
		new SchemaExport().create( EnumSet.of( TargetType.STDOUT, TargetType.DATABASE ), modelScope.getDomainModel() );
		System.out.println( "********* Completed SchemaExport for START-UP *************************" );
	}

	@Test
	public void testUpdateSchema(DomainModelScope modelScope) {
		new SchemaUpdate().execute( EnumSet.of( TargetType.STDOUT, TargetType.DATABASE ), modelScope.getDomainModel() );
	}

	@AfterEach
	public void tearDown(DomainModelScope modelScope) {
		System.out.println( "********* Starting SchemaExport (drop) for TEAR-DOWN *************************" );
		new SchemaExport().drop( EnumSet.of( TargetType.STDOUT, TargetType.DATABASE ), modelScope.getDomainModel() );
		System.out.println( "********* Completed SchemaExport (drop) for TEAR-DOWN *************************" );
	}

	@Entity
	@Table(name = "MyEntity")
	public static class MyEntity {

		@Id
		public int getId() {
			return 0;
		}

		@Column(name = "Ul")
		public int getValue() {
			return 0;
		}

		public void setId(final int _id) {
		}

		public void setValue(int value) {
		}
	}

}
