/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.foreignkeys.disabled;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.Environment;
import org.hibernate.mapping.Table;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Yanming Zhou
 */
@Jira("https://hibernate.atlassian.net/browse/HHH-14253")
@Jira("https://hibernate.atlassian.net/browse/HHH-17550")
@BaseUnitTest
public class DefaultConstraintModeTest {
	@Test
	public void testForeignKeyShouldNotBeCreated() {
		testForeignKeyCreation( false );
	}

	@Test
	public void testForeignKeyShouldBeCreated() {
		testForeignKeyCreation( true );
	}

	private void testForeignKeyCreation(boolean created) {
		try (StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( Environment.HBM2DDL_DEFAULT_CONSTRAINT_MODE, created ? "CONSTRAINT" : "NO_CONSTRAINT" )
				.build()) {
			Metadata metadata = new MetadataSources( ssr ).addAnnotatedClasses( TestEntity.class, ChildEntity.class )
					.buildMetadata();
			assertThat( findTable( metadata, "TestEntity" ).getForeignKeyCollection().isEmpty() )
					.isEqualTo( !created );
			assertThat( findTable( metadata, "ChildEntity" ).getForeignKeyCollection().isEmpty() )
					.isEqualTo( !created );
		}
	}

	private static Table findTable(Metadata metadata, String tableName) {
		return StreamSupport.stream( metadata.getDatabase().getNamespaces().spliterator(), false )
				.flatMap( namespace -> namespace.getTables().stream() ).filter( t -> t.getName().equals( tableName ) )
				.findFirst().orElse( null );
	}

	@Entity(name = "TestEntity")
	@jakarta.persistence.Table(name = "TestEntity")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class TestEntity {

		@Id
		private Long id;

		@ManyToOne
		private TestEntity toOne;

		@OneToMany
		@JoinColumn
		private List<ChildEntity> oneToMany;

		@ManyToMany
		private List<ChildEntity> manyToMany;

		@ElementCollection
		private List<String> elements;
	}

	@Entity(name = "ChildEntity")
	@jakarta.persistence.Table(name = "ChildEntity")
	public static class ChildEntity extends TestEntity {
		private String childName;
	}
}
