package org.hibernate.orm.test.inheritance;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

/**
 * Test for HHH-20884: Verifies that JOINED inheritance works correctly
 * with composite IDs containing 3+ columns.
 */
@JiraKey("HHH-20884")
@DomainModel(
		annotatedClasses = {
				JoinedInheritanceWithThreeColumnCompositeIdTest.TripleId.class,
				JoinedInheritanceWithThreeColumnCompositeIdTest.BaseEntity.class,
				JoinedInheritanceWithThreeColumnCompositeIdTest.DerivedEntity.class
		}
)
@SessionFactory
public class JoinedInheritanceWithThreeColumnCompositeIdTest {

	@Test
	public void testThreeColumnCompositeId(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			DerivedEntity entity = new DerivedEntity();
			entity.setId( new TripleId( 1, 2, 3 ) );
			entity.setName( "Test Entity" );
			entity.setValue( "Derived Value" );

			session.persist( entity );
		} );

		scope.inTransaction( session -> {
			DerivedEntity found = session.find( DerivedEntity.class, new TripleId( 1, 2, 3 ) );
			assert found != null;
			assert "Test Entity".equals( found.getName() );
			assert "Derived Value".equals( found.getValue() );
		} );
	}

	@Embeddable
	public static class TripleId implements Serializable {
		private Integer part1;
		private Integer part2;
		private Integer part3;

		public TripleId() {
		}

		public TripleId(Integer part1, Integer part2, Integer part3) {
			this.part1 = part1;
			this.part2 = part2;
			this.part3 = part3;
		}

		public Integer getPart1() {
			return part1;
		}

		public void setPart1(Integer part1) {
			this.part1 = part1;
		}

		public Integer getPart2() {
			return part2;
		}

		public void setPart2(Integer part2) {
			this.part2 = part2;
		}

		public Integer getPart3() {
			return part3;
		}

		public void setPart3(Integer part3) {
			this.part3 = part3;
		}
	}

	@Entity(name = "BaseEntityTriple")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static abstract class BaseEntity {
		@EmbeddedId
		private TripleId id;

		private String name;

		public TripleId getId() {
			return id;
		}

		public void setId(TripleId id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "DerivedEntityTriple")
	// No explicit @PrimaryKeyJoinColumns - testing implicit mapping with 3 columns
	public static class DerivedEntity extends BaseEntity {
		private String data;

		public String getValue() {
			return data;
		}

		public void setValue(String data) {
			this.data = data;
		}
	}
}
