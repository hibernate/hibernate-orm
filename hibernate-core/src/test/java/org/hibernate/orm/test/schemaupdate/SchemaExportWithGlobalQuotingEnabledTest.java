/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.hibernate.cfg.MappingSettings.GLOBALLY_QUOTED_IDENTIFIERS;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-9866")
@RequiresDialect(MySQLDialect.class)
@org.hibernate.testing.orm.junit.ServiceRegistry(settings = @Setting(name = GLOBALLY_QUOTED_IDENTIFIERS, value = "true"))
@DomainModel(annotatedClasses = {SchemaExportWithGlobalQuotingEnabledTest.MyEntity.class, SchemaExportWithGlobalQuotingEnabledTest.Role.class})
public class SchemaExportWithGlobalQuotingEnabledTest {
	@Test
	public void testSchemaExport(DomainModelScope modelScope) {
		SchemaExport schemaExport = new SchemaExport();
		schemaExport.create( EnumSet.of( TargetType.STDOUT, TargetType.DATABASE ), modelScope.getDomainModel() );

		//noinspection unchecked
		List<SQLException> exceptions = schemaExport.getExceptions();
		for ( SQLException exception : exceptions ) {
			assertThat( exception.getMessage(), exception.getSQLState(), not( "42000" ) );
		}
	}

	@BeforeEach
	public void setUp(DomainModelScope modelScope) {
		System.out.println( "********* Starting SchemaExport for START-UP *************************" );
		new SchemaExport().create( EnumSet.of( TargetType.STDOUT, TargetType.DATABASE ), modelScope.getDomainModel() );
		System.out.println( "********* Completed SchemaExport for START-UP *************************" );
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
		private int id;
		private Set<Role> roles;

		@Id
		public int getId() {
			return this.id;
		}

		public void setId(final int id) {
			this.id = id;
		}

		@ManyToMany
		public Set<Role> getRoles() {
			return roles;
		}

		public void setRoles(Set<Role> roles) {
			this.roles = roles;
		}
	}

	@Entity
	public static class Role {
		private Integer id;

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

}
