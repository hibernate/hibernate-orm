/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.joincolumn;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

@DomainModel(xmlMappings = "org/hibernate/orm/test/mapping/joincolumn/JoinColumnOverrideTest.orm.xml")
@SessionFactory
public class JoinColumnOverrideTest {

	@Test
	public void testIt(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Size size = new Size( 2 );
			session.persist( size );
			ContainedSize containedSize = new ContainedSize( size );

			Material material = new Material( 1 );
			material.addContainedSize( containedSize );
			session.persist( material );
		} );
	}

	public static class Material {
		private int id;

		private Set<ContainedSize> containedSizes = new HashSet<>();

		public Material() {
		}

		public Material(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public Set<ContainedSize> getContainedSizes() {
			return containedSizes;
		}

		public void setContainedSizes(Set<ContainedSize> containedSizes) {
			this.containedSizes = containedSizes;
		}

		public void addContainedSize(ContainedSize containedSize) {
			this.containedSizes.add( containedSize );
		}
	}

	public static class ContainedSize {

		private Size size;

		public ContainedSize() {
		}

		public ContainedSize(Size size) {
			this.size = size;
		}

		public Size getSize() {
			return size;
		}

		public void setSize(Size size) {
			this.size = size;
		}
	}

	public static class Size {
		private int id;

		public Size() {
		}

		public Size(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}
	}
}
