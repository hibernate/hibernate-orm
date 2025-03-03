/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.dynamic;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@JiraKey("HHH-15186")
@BytecodeEnhanced
@DomainModel(
		annotatedClasses = {
				DynamicStatusWithMultipleToManyRelationTest.FooEntity.class,
				DynamicStatusWithMultipleToManyRelationTest.BarEntity.class,
				DynamicStatusWithMultipleToManyRelationTest.BazEntity.class
		}
)
@SessionFactory
public class DynamicStatusWithMultipleToManyRelationTest {

	@Test
	public void test(SessionFactoryScope scope) {
		final EntityPersister persister = scope.getSessionFactory().getMappingMetamodel()
				.findEntityDescriptor( FooEntity.class );
		assertThat( persister.getEntityMetamodel().isDynamicUpdate() ).isFalse();
	}

	@Entity(name = "FooEntity")
	public static class FooEntity {

		@Id
		private long id;
		@Version
		private int version;

		private String name;

		@LazyGroup("bars")
		@OneToMany(mappedBy = "foo")
		public Set<BarEntity> bars = new HashSet<>();

		@LazyGroup("bazzes")
		@OneToMany(mappedBy = "foo")
		public Set<BazEntity> bazzes = new HashSet<>();

	}

	@Entity(name = "BazEntity")
	public static class BazEntity {

		@Id
		@GeneratedValue
		private long id;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		private FooEntity foo;

	}

	@Entity(name = "BarEntity")
	public static class BarEntity {

		@Id
		@GeneratedValue
		private long id;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		private FooEntity foo;

	}
}
