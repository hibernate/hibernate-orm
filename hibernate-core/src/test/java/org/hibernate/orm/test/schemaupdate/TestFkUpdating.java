/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import java.util.EnumSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {
		TestFkUpdating.User.class,
		TestFkUpdating.Role.class
})
public class TestFkUpdating {
	@BeforeEach
	public void setUp(DomainModelScope modelScope) {
		System.out.println( "********* Starting SchemaExport for START-UP *************************" );
		new SchemaExport().create( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ), modelScope.getDomainModel() );
		System.out.println( "********* Completed SchemaExport for START-UP *************************" );
	}

	@AfterEach
	public void tearDown(DomainModelScope modelScope) {
		System.out.println( "********* Starting SchemaExport (drop) for TEAR-DOWN *************************" );
		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ), modelScope.getDomainModel() );
		System.out.println( "********* Completed SchemaExport (drop) for TEAR-DOWN *************************" );
	}

	@Test
	public void testUpdate(DomainModelScope modelScope) {
		System.out.println( "********* Starting SchemaUpdate for TEST *************************" );
		new SchemaUpdate().execute( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ), modelScope.getDomainModel() );
		System.out.println( "********* Completed SchemaUpdate for TEST *************************" );
	}


	@Entity( name = "User" )
	@Table( name = "my_user")
	public static class User {
		private Integer id;
		private Set<Role> roles;

		@Id
		@GeneratedValue(generator = "increment")
		@GenericGenerator(name = "increment",strategy = "increment")
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
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

	@Entity( name = "Role" )
	@Table( name = "`Role`")
	public class Role {
		private Integer id;

		@Id
		@GeneratedValue(generator = "increment")
		@GenericGenerator(name = "increment",strategy = "increment")
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}
}
