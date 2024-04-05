/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bytecode.enhance.internal.bytebuddy;

import java.util.List;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Tuple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.bytecode.enhance.internal.bytebuddy.DirtyCheckingWithEmbeddableExtendingMappedSuperclassTest.*;


@DomainModel(annotatedClasses = { MyEntity.class, })
@SessionFactory
@BytecodeEnhanced(testEnhancedClasses = MyEntity.class)
@EnhancementOptions(inlineDirtyChecking = true)
public class DirtyCheckingWithEmbeddableExtendingMappedSuperclassTest {

	@JiraKey("HHH-17041")
	@Test
	public void testQueryEmbeddableFields(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					MyEntity myEntity  = new MyEntity(1, "one");
					session.persist( myEntity );
				}
		);
		scope.inTransaction(
				session -> {
					List<Tuple> result = session.createQuery( "select m.embedded.text, m.embedded.name from MyEntity m", Tuple.class ).list();
					assertThat( result.size() ).isEqualTo( 1 );
					Tuple tuple = result.get( 0 );
					assertThat( tuple.get( 0 ) ).isEqualTo( "one" );
					assertThat( tuple.get( 1 ) ).isNull();

				}
		);
	}

	@MappedSuperclass
	public static abstract class MyAbstractEmbeddable {
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Embeddable
	public static class MyEmbeddable extends MyAbstractEmbeddable {

		private String text;

		public MyEmbeddable() {
		}

		private MyEmbeddable(String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}


	@Entity(name = "MyEntity")
	public static class MyEntity  {

		@Id
		private Integer id;

		@Embedded
		private MyEmbeddable embedded;

		public MyEmbeddable getEmbedded() {
			return embedded;
		}

		public void setEmbedded(MyEmbeddable embedded) {
			this.embedded = embedded;
		}

		public MyEntity() {
		}

		private MyEntity(Integer id, String text) {
			this.id = id;
			setEmbedded( new MyEmbeddable( text ) );
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

	}

}
