/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.joined;

import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.RootClass;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.bind.BindingTestingHelper.checkDomainModel;

/**
 * @author Steve Ebersole
 */
public class SimpleJoinedTests {
	@Test
	@ServiceRegistry
	void simpleTest(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final var metadataCollector = context.getMetadataCollector();
					final RootClass rootBinding = (RootClass) metadataCollector.getEntityBinding( Root.class.getName() );
					final JoinedSubclass subBinding = (JoinedSubclass) metadataCollector.getEntityBinding( Sub.class.getName() );

					assertThat( rootBinding.getTable() ).isNotNull();
					assertThat( rootBinding.getTable() ).isSameAs( rootBinding.getIdentityTable() );
					assertThat( rootBinding.getTable() ).isSameAs( rootBinding.getRootTable() );
					assertThat( rootBinding.getDiscriminator() ).isNotNull();
					assertThat( rootBinding.getDiscriminator().getColumns() ).hasSize( 1 );
					assertThat( rootBinding.getDiscriminatorValue() ).isEqualTo( "R" );

					assertThat( subBinding.getTable() ).isNotNull();
					assertThat( subBinding.getTable() ).isNotSameAs( rootBinding.getIdentityTable() );
					assertThat( subBinding.getTable() ).isNotSameAs( rootBinding.getRootTable() );
					assertThat( subBinding.getIdentityTable() ).isSameAs( rootBinding.getRootTable() );
					assertThat( subBinding.getRootTable() ).isSameAs( rootBinding.getRootTable() );
					assertThat( subBinding.getDiscriminatorValue() ).isEqualTo( "S" );

					assertThat( rootBinding.getTable().getPrimaryKey() ).isNotNull();
					assertThat( rootBinding.getTable().getPrimaryKey().getColumns() ).hasSize( 1 );

					assertThat( subBinding.getTable().getPrimaryKey() ).isNotNull();
					assertThat( subBinding.getTable().getPrimaryKey().getColumns() ).hasSize( 1 );
				},
				scope.getRegistry(),
				Root.class,
				Sub.class
		);
	}

	@Test
	@ServiceRegistry
	void compositeIdTest(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final var metadataCollector = context.getMetadataCollector();
					final RootClass rootBinding = (RootClass) metadataCollector.getEntityBinding( CompositeRoot.class.getName() );
					final JoinedSubclass subBinding = (JoinedSubclass) metadataCollector.getEntityBinding( CompositeSub.class.getName() );

					assertThat( rootBinding.getTable().getPrimaryKey() ).isNotNull();
					assertThat( rootBinding.getTable().getPrimaryKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id1", "id2" );

					assertThat( subBinding.getKey() ).isNotNull();
					assertThat( subBinding.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id1", "id2" );
					assertThat( subBinding.getTable().getPrimaryKey() ).isNotNull();
					assertThat( subBinding.getTable().getPrimaryKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id1", "id2" );
				},
				scope.getRegistry(),
				CompositeRoot.class,
				CompositeSub.class
		);
	}

	@Test
	@ServiceRegistry
	void idClassTest(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final var metadataCollector = context.getMetadataCollector();
					final RootClass rootBinding = (RootClass) metadataCollector.getEntityBinding( IdClassRoot.class.getName() );
					final JoinedSubclass subBinding = (JoinedSubclass) metadataCollector.getEntityBinding( IdClassSub.class.getName() );

					assertThat( rootBinding.getTable().getPrimaryKey() ).isNotNull();
					assertThat( rootBinding.getTable().getPrimaryKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id1", "id2" );

					assertThat( subBinding.getKey() ).isNotNull();
					assertThat( subBinding.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id1", "id2" );
					assertThat( subBinding.getTable().getPrimaryKey() ).isNotNull();
					assertThat( subBinding.getTable().getPrimaryKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id1", "id2" );
				},
				scope.getRegistry(),
				IdClassRoot.class,
				IdClassSub.class
		);
	}

	@Entity(name = "Root")
	@Table(name = "Root")
	@Inheritance(strategy = InheritanceType.JOINED)
	@DiscriminatorColumn(discriminatorType = DiscriminatorType.CHAR)
	@DiscriminatorValue("R")
	public static class Root {
		@Id
		private Integer id;

		private String name;
	}

	@Entity(name = "Sub")
	@Table(name = "Sub")
	@DiscriminatorValue("S")
	public static class Sub extends Root {
		@Id
		private Integer id;

		private String name;
	}

	@Entity(name = "CompositeRoot")
	@Table(name = "CompositeRoot")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class CompositeRoot {
		@EmbeddedId
		private Pk id;

		private String name;
	}

	@Entity(name = "CompositeSub")
	@Table(name = "CompositeSub")
	public static class CompositeSub extends CompositeRoot {
		private String details;
	}

	@Embeddable
	public static class Pk {
		private Integer id1;
		private Integer id2;
	}

	@Entity(name = "IdClassRoot")
	@Table(name = "IdClassRoot")
	@Inheritance(strategy = InheritanceType.JOINED)
	@IdClass( IdClassPk.class )
	public static class IdClassRoot {
		@Id
		private Integer id1;

		@Id
		private Integer id2;

		private String name;
	}

	@Entity(name = "IdClassSub")
	@Table(name = "IdClassSub")
	public static class IdClassSub extends IdClassRoot {
		private String details;
	}

	public static class IdClassPk {
		private Integer id1;
		private Integer id2;
	}
}
