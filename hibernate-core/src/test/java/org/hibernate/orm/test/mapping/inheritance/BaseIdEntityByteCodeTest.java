/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.inheritance;

import org.hibernate.annotations.LazyGroup;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Basic;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(BytecodeEnhancerRunner.class)
public class BaseIdEntityByteCodeTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { BaseEntity.class, ContainingEntity.class };
	}

	@Test
	public void test() {
		inTransaction( session -> {
			ContainingEntity entity1 = new ContainingEntity();
			entity1.id = 1;
			entity1.baseText = "initialValue";

			ContainedEmbeddable containedEmbeddable = new ContainedEmbeddable();
			entity1.containedEmbeddable = containedEmbeddable;
			containedEmbeddable.text = "initialValue";
			session.persist( entity1 );
		} );

		inTransaction( session -> {
			ContainingEntity entity = session.load( ContainingEntity.class, 1 );
			ContainedEmbeddable containedEmbeddable = entity.getContainedEmbeddable();
			assertThat( containedEmbeddable.getText() ).isEqualTo( "initialValue" );
		} );
	}

	@Entity(name = "base")
	public static class BaseEntity {

		@Id
		public Integer id;

		@Basic(fetch = FetchType.LAZY)
		@LazyGroup("base")
		public String baseText;

		public String baseText2;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getBaseText() {
			return baseText;
		}

		public void setBaseText(String baseText) {
			this.baseText = baseText;
		}

		public String getBaseText2() {
			return baseText2;
		}

		public void setBaseText2(String baseText2) {
			this.baseText2 = baseText2;
		}


	}

	@Entity(name = "containing")
	public static class ContainingEntity extends BaseEntity {
		@Embedded
		public ContainedEmbeddable containedEmbeddable;

		public ContainedEmbeddable getContainedEmbeddable() {
			return containedEmbeddable;
		}

		public void setContainedEmbeddable(ContainedEmbeddable containedEmbeddable) {
			this.containedEmbeddable = containedEmbeddable;
		}

	}

	@Embeddable
	public static class ContainedEmbeddable {

		@Basic(fetch = FetchType.LAZY)
		public String text;

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

}
