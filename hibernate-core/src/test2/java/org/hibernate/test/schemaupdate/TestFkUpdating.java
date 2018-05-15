/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate;

import java.util.EnumSet;
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
import org.hibernate.tool.schema.TargetType;

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
		new SchemaExport().create( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ), metadata );
		System.out.println( "********* Completed SchemaExport for START-UP *************************" );
	}

	@After
	public void tearDown() {
		System.out.println( "********* Starting SchemaExport (drop) for TEAR-DOWN *************************" );
		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ), metadata );
		System.out.println( "********* Completed SchemaExport (drop) for TEAR-DOWN *************************" );


		StandardServiceRegistryBuilder.destroy( serviceRegistry );
		serviceRegistry = null;
	}

	@Test
	public void testUpdate() {
		System.out.println( "********* Starting SchemaUpdate for TEST *************************" );
		new SchemaUpdate().execute( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ), metadata );
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
