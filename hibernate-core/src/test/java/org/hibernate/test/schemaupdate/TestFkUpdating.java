/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.schemaupdate;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class TestFkUpdating {
	protected ServiceRegistry serviceRegistry;
	protected MetadataImplementor metadata;

	@Before
	public void setUp() {
		serviceRegistry = new StandardServiceRegistryBuilder().build();
		metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
				.addAnnotatedClass( User.class )
				.addAnnotatedClass(Role.class)
				.buildMetadata();

		System.out.println( "********* Starting SchemaExport for START-UP *************************" );
		SchemaExport schemaExport = new SchemaExport( serviceRegistry, metadata );
		schemaExport.create( true, true );
		System.out.println( "********* Completed SchemaExport for START-UP *************************" );
	}

	@After
	public void tearDown() {
		System.out.println( "********* Starting SchemaExport (drop) for TEAR-DOWN *************************" );
		SchemaExport schemaExport = new SchemaExport( serviceRegistry, metadata );
		schemaExport.drop( true, true );
		System.out.println( "********* Completed SchemaExport (drop) for TEAR-DOWN *************************" );


		StandardServiceRegistryBuilder.destroy( serviceRegistry );
		serviceRegistry = null;
	}

	@Test
	public void testUpdate() {
		System.out.println( "********* Starting SchemaUpdate for TEST *************************" );
		SchemaUpdate schemaUpdate = new SchemaUpdate( serviceRegistry, metadata );
		schemaUpdate.execute( true, true );
		System.out.println( "********* Completed SchemaUpdate for TEST *************************" );
	}


	@Entity( name = "User" )
	@Table( name = "user" )
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
	@Table( name = "role" )
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
