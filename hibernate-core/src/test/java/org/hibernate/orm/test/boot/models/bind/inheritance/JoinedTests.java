/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.bind.inheritance;

import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.RootClass;

import org.hibernate.testing.orm.junit.FailureExpected;
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
public class JoinedTests {
	/**
	 * Allowing for something like:
	 *
	 * primaryKeyBinder.whenResolved( (primaryKey) -> ... )
	 *
	 */
	@SuppressWarnings("JUnitMalformedDeclaration")
	@Test
	@ServiceRegistry
	@FailureExpected(
			reason = "Binding the primary key is done twice by 2 'owners' overwriting details. " +
					"Might be case for distinct, sequential root key and secondary key (secondary tables, subclass tables) table phases."
	)
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

	@Entity(name="Root")
	@Table(name="Root")
	@Inheritance(strategy = InheritanceType.JOINED)
	@DiscriminatorColumn(discriminatorType = DiscriminatorType.CHAR)
	@DiscriminatorValue("R")
	public static class Root {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name="Sub")
	@Table(name="Sub")
	@DiscriminatorValue("S")
	public static class Sub extends Root {
		@Id
		private Integer id;
		private String name;
	}
}
