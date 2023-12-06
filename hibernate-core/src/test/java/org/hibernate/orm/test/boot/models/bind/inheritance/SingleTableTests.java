/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.bind.inheritance;

import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Subclass;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.bind.BindingTestingHelper.checkDomainModel;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry
public class SingleTableTests {
	@Test
	void testNoInheritance(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final var metadataCollector = context.getMetadataCollector();
					final PersistentClass rootBinding = metadataCollector.getEntityBinding( Single.class.getName() );
					final BasicValue discriminatorMapping = (BasicValue) rootBinding.getDiscriminator();
					assertThat( discriminatorMapping ).isNull();
				},
				scope.getRegistry(),
				Single.class
		);
	}

	@Test
	void testImplicitDiscriminator(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final var metadataCollector = context.getMetadataCollector();
					final PersistentClass rootBinding = metadataCollector.getEntityBinding( Root.class.getName() );
					final PersistentClass subBinding = metadataCollector.getEntityBinding( Sub.class.getName() );

					assertThat( rootBinding.getTable().isAbstract() ).isFalse();
					assertThat( rootBinding.getTable().getName() ).isEqualToIgnoringCase( "data" );

					final BasicValue discriminatorMapping = (BasicValue) rootBinding.getDiscriminator();
					assertThat( discriminatorMapping ).isNotNull();
					assertThat( discriminatorMapping.getColumn() ).isNotNull();
					assertThat( ( (Column) discriminatorMapping.getColumn() ).getName() ).isEqualToIgnoringCase( "dtype" );
					assertThat( discriminatorMapping.resolve().getDomainJavaType().getJavaType() ).isEqualTo( String.class );

					assertThat( rootBinding.getDiscriminatorValue() ).isEqualTo( Root.class.getName() );

					assertThat( subBinding.getTable() ).isSameAs( rootBinding.getTable() );
					assertThat( subBinding.getDiscriminatorValue() ).isEqualTo( Sub.class.getName() );
				},
				scope.getRegistry(),
				Root.class,
				Sub.class
		);
	}

	@Test
	void testExplicitDiscriminator(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final var metadataCollector = context.getMetadataCollector();
					final PersistentClass rootBinding = metadataCollector.getEntityBinding( ExplicitRoot.class.getName() );
					final PersistentClass subBinding = metadataCollector.getEntityBinding( ExplicitSub.class.getName() );

					assertThat( rootBinding.getTable().isAbstract() ).isFalse();
					assertThat( rootBinding.getTable().getName() ).isEqualToIgnoringCase( "data2" );

					final BasicValue discriminatorMapping = (BasicValue) rootBinding.getDiscriminator();
					assertThat( discriminatorMapping ).isNotNull();
					assertThat( discriminatorMapping.getColumn() ).isNotNull();
					assertThat( ( (Column) discriminatorMapping.getColumn() ).getName() ).isEqualToIgnoringCase( "type_discriminator" );
					assertThat( discriminatorMapping.resolve().getDomainJavaType().getJavaType() ).isEqualTo( Character.class );

					assertThat( rootBinding.getDiscriminatorValue() ).isEqualTo( "R" );

					assertThat( subBinding.getTable() ).isSameAs( rootBinding.getTable() );
					assertThat( subBinding.getDiscriminatorValue() ).isEqualTo( "S" );
				},
				scope.getRegistry(),
				ExplicitRoot.class,
				ExplicitSub.class
		);
	}

	@Test
	void testAttributes(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final var metadataCollector = context.getMetadataCollector();
					final RootClass rootBinding = (RootClass) metadataCollector.getEntityBinding( Root.class.getName() );
					final SingleTableSubclass subBinding = (SingleTableSubclass) metadataCollector.getEntityBinding( Sub.class.getName() );

					assertThat( rootBinding.getDeclaredProperties() ).hasSize( 2 );
					assertThat( rootBinding.getProperties() ).hasSize( 2 );
					assertThat( rootBinding.getPropertyClosure() ).hasSize( 2 );
					assertThat( rootBinding.getSubclassPropertyClosure() ).hasSize( 3 );

					assertThat( subBinding.getDeclaredProperties() ).hasSize( 1 );
					assertThat( subBinding.getProperties() ).hasSize( 1 );
					assertThat( subBinding.getPropertyClosure() ).hasSize( 3 );
					assertThat( subBinding.getSubclassPropertyClosure() ).hasSize( 3 );
				},
				scope.getRegistry(),
				Root.class,
				Sub.class
		);
	}

	@Entity(name="Single")
	@Table(name="Single")
	public static class Single {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name="Root")
	@Table(name="data")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class Root {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name="Sub")
	public static class Sub extends Root {
		private String details;
	}

	@Entity(name="ExplicitRoot")
	@Table(name="data2")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "type_discriminator", discriminatorType = DiscriminatorType.CHAR, length = 1)
	@DiscriminatorValue( "R" )
	public static class ExplicitRoot {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name="Sub")
	@DiscriminatorValue( "S" )
	public static class ExplicitSub extends ExplicitRoot {
		private String details;
	}
}
