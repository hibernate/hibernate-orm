/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cascade.internal;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;

/// Annotation model implementing Hibernate's enhancement contract for
/// `CascadeTraversalBenchmark`. Hibernate injects its real lazy-attribute
/// interceptor when the benchmark graph is prepared.
///
/// @author Steve Ebersole
public final class CascadeTraversalBenchmarkModel {
	private CascadeTraversalBenchmarkModel() {
	}

	@Entity(name = "EnhancedCascadeRoot")
	public static class Root implements PersistentAttributeInterceptable {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
		private Child child;

		private transient PersistentAttributeInterceptor interceptor;

		public Root() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Child getChild() {
			return child;
		}

		public void setChild(Child child) {
			this.child = child;
		}

		@Override
		public PersistentAttributeInterceptor $$_hibernate_getInterceptor() {
			return interceptor;
		}

		@Override
		public void $$_hibernate_setInterceptor(PersistentAttributeInterceptor interceptor) {
			this.interceptor = interceptor;
		}
	}

	@Entity(name = "EnhancedCascadeChild")
	public static class Child {
		@Id
		private Long id;

		private String name;

		public Child() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
