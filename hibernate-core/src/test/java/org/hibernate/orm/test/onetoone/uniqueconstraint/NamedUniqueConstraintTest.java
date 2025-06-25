/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.uniqueconstraint;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Jake Goldsmith
 */
public class NamedUniqueConstraintTest extends EntityManagerFactoryBasedFunctionalTest {

	public static final String TEST_CONSTRAINT_NAME = "manually_set_unique_constraint_name";
	protected ServiceRegistry serviceRegistry;
	protected MetadataImplementor metadata;

	@Test
	void hhh19006Test() {
		//Tests that the unique constraint was created using the manually defined name rather than generated
		serviceRegistry = ServiceRegistryUtil.serviceRegistry();
		metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
				.addAnnotatedClass( A.class )
				.addAnnotatedClass( B.class )
				.buildMetadata();

		PersistentClass a = metadata.getEntityBinding(
				"org.hibernate.orm.test.onetoone.uniqueconstraint.NamedUniqueConstraintTest$A" );
		org.hibernate.mapping.Column foreignKeyColumn = a.getTable().getColumn( Identifier.toIdentifier( "b_id" ) );
		assertTrue( foreignKeyColumn.isUnique() );
		assertEquals( TEST_CONSTRAINT_NAME, foreignKeyColumn.getUniqueKeyName() );
	}

	@Entity
	@Table(name = "a", uniqueConstraints = {
			@UniqueConstraint(name = TEST_CONSTRAINT_NAME, columnNames = "b_id")
	})
	public class A {
		@Id
		@Column(name = "a_id")
		private Long id;

		@OneToOne(orphanRemoval = true, cascade = CascadeType.ALL)
		@JoinColumn(nullable = false, name = "b_id", foreignKey = @ForeignKey(name = "fk_a_b"))
		private B b;
	}

	@Entity
	@Table(name = "b")
	public class B {
		@Id
		@Column(name = "b_id")
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "b_seq")
		@SequenceGenerator(name = "b_seq", sequenceName = "b_seq", allocationSize = 1)
		private Long id;
	}
}
