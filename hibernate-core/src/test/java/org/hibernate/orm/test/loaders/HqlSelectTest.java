/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.loaders;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.HQLSelect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.PERSIST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SessionFactory
@DomainModel(annotatedClasses = {HqlSelectTest.WithHqlSelect.class, HqlSelectTest.UUID.class})
@SkipForDialect(dialectClass = SybaseASEDialect.class, reason = "Driver or DB omit trailing zero bytes of a varbinary, making this test fail intermittently")
public class HqlSelectTest {

	@Test
	void test(SessionFactoryScope scope) {
		WithHqlSelect withHqlSelect = new WithHqlSelect();
		withHqlSelect.name = "Hibernate";
		withHqlSelect.uuids.add( new UUID( withHqlSelect ) );
		withHqlSelect.uuids.add( new UUID( withHqlSelect ) );
		withHqlSelect.uuids.add( new UUID( withHqlSelect ) );

		scope.inTransaction( s -> s.persist( withHqlSelect ) );

		scope.inTransaction( s -> {
			WithHqlSelect wss = s.get( WithHqlSelect.class, withHqlSelect.id );
			assertEquals( "Hibernate", wss.name );
			assertEquals( 3, wss.uuids.size() );
			wss.uuids.get(2).deleted = true;
		});

		scope.inTransaction( s -> {
			WithHqlSelect wss = s.get( WithHqlSelect.class, withHqlSelect.id );
			assertEquals( "Hibernate", wss.name );
			assertEquals( 2, wss.uuids.size() );
			wss.deleted = true;
		});

		scope.inTransaction( s -> {
			WithHqlSelect wss = s.get( WithHqlSelect.class, withHqlSelect.id );
			assertNull( wss );
		});
	}

	@Entity(name = "WithHqlSelect")
	@Table(name = "With_Sql_Select")
	@HQLSelect(query = "from WithHqlSelect where id = ?1 and deleted = false")
	static class WithHqlSelect {
		@Id @GeneratedValue
		@Column(name = "Sql_Select_id")
		Long id;

		String name;

		boolean deleted = false;

		@OneToMany(mappedBy = "hqlSelect", cascade = PERSIST)
		@HQLSelect(query = "from UUID where hqlSelect.id = ?1 and deleted = false")
		List<UUID> uuids = new ArrayList<>();
	}

	@Entity(name = "UUID")
	@Table(name = "With_Uuids")
	static class UUID {
		@Id @GeneratedValue
		@Column(name = "Uuid_id")
		Long id;

		java.util.UUID uuid = SafeRandomUUIDGenerator.safeRandomUUID();

		boolean deleted = false;

		@ManyToOne
		@JoinColumn(name = "Sql_Select_id")
		WithHqlSelect hqlSelect;

		UUID() {}

		UUID(WithHqlSelect withHqlSelect) {
			hqlSelect = withHqlSelect;
		}
	}
}
