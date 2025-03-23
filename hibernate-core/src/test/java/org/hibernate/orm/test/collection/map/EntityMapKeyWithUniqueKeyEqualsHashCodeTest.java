/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.OneToMany;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(
		annotatedClasses = {
				EntityMapKeyWithUniqueKeyEqualsHashCodeTest.ContainerEntity.class, EntityMapKeyWithUniqueKeyEqualsHashCodeTest.KeyEntity.class, EntityMapKeyWithUniqueKeyEqualsHashCodeTest.ValueEntity.class
		}
)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-7045")
public class EntityMapKeyWithUniqueKeyEqualsHashCodeTest {

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					ContainerEntity ce = new ContainerEntity();
					ce.id = 1L;
					KeyEntity k1 = new KeyEntity();
					k1.id = 1L;
					k1.handle = "k1";
					KeyEntity k2 = new KeyEntity();
					k2.id = 2L;
					k2.handle = "k2";
					ValueEntity v1 = new ValueEntity();
					v1.id = 1L;
					v1.key = k1;
					v1.container = ce;
					ce.values.put( k1, v1 );
					ValueEntity v2 = new ValueEntity();
					v2.id = 2L;
					v2.key = k2;
					v2.container = ce;
					ce.values.put( k2, v2 );
					session.persist( ce );

				}
		);
		scope.inTransaction(
				session -> {
					ContainerEntity ce = session.find( ContainerEntity.class, 1L );
					assertEquals( 2, ce.values.size() );
				}
		);

	}

	@Entity(name = "ContainerEntity")
	public static class ContainerEntity {
		@Id
		public Long id;
		@OneToMany(mappedBy = "container", cascade = CascadeType.ALL)
		@MapKeyJoinColumn(name = "key_id")
		public Map<KeyEntity, ValueEntity> values = new HashMap<>();
	}

	@Entity(name = "KeyEntity")
	public static class KeyEntity {
		@Id
		public Long id;
		@Column(nullable = false, unique = true)
		public String handle;

		@Override
		public boolean equals(Object o) {
			return o instanceof KeyEntity oke
				&& Objects.equals( handle, oke.handle );
		}

		@Override
		public int hashCode() {
			return Objects.hashCode( handle );
		}
	}

	@Entity(name = "ValueEntity")
	public static class ValueEntity {
		@Id
		public Long id;
		@ManyToOne(cascade = CascadeType.ALL)
		public KeyEntity key;
		@ManyToOne(cascade = CascadeType.ALL)
		public ContainerEntity container;

		@Override
		public boolean equals(Object o) {
			return o instanceof ValueEntity ove
				&& Objects.equals( key, ove.key );
		}

		@Override
		public int hashCode() {
			return Objects.hashCode( key );
		}
	}

}
