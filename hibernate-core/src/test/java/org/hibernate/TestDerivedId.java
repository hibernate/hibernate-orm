/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
public class TestDerivedId {
	@Test
	@DomainModel(annotatedClasses = {Owner.class, OwnerAddress.class})
	@SessionFactory
	void verifyModel(DomainModelScope modelScope, SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var owner = new Owner();
			var address = new OwnerAddress();

			owner.id = 1;
			owner.address = address;
			owner.name = "John";

			address.owner = owner;
			address.txt = "123 Main St";

			session.persist( owner );
			session.persist( address );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Entity
	@Table( name = "owners" )
	public static class Owner {
		@Id
		private Integer id;

		String name;

		@OneToOne(mappedBy = "owner")
		@PrimaryKeyJoinColumn
		OwnerAddress address;
	}

	@Entity
	@Table( name = "addresses" )
	public static class OwnerAddress {

		@Id
		@OneToOne
		@JoinColumn(name = "owner_id")
		private Owner owner;

		private String txt;
	}
}
