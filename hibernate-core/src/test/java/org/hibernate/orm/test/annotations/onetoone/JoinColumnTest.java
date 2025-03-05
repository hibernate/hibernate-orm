package org.hibernate.orm.test.annotations.onetoone;

import java.io.Serializable;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@JiraKey("HHH-17420")
@Jpa(
		annotatedClasses = {
				JoinColumnTest.LeftEntity.class,
				JoinColumnTest.MidEntity.class,
				JoinColumnTest.RightEntity.class,
		}
)
class JoinColumnTest extends BaseUnitTestCase {

	@Test
	void testLeftToRight(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
				}
		);
	}

	@Entity(name = "LeftEntity")
	static class LeftEntity {

		@Embeddable
		static class Pk implements Serializable {
			@Column
			String id_one_2;

			@Column
			String id_two_2;

			@Column
			String id_three_2;
		}

		@EmbeddedId
		Pk id;

		@OneToOne(mappedBy = "aLeftEntity")
		MidEntity midEntity;
	}

	@Entity(name = "MidEntity")
	static class MidEntity {

		@Id
		@Column
		String id;

		@Column
		String id_one;

		@Column
		String id_two;

		@Column
		String id_three;

		@OneToOne
		@JoinColumn(name = "id_one", referencedColumnName = "id_one_2", insertable = false, updatable = false)
		@JoinColumn(name = "id_two", referencedColumnName = "id_two_2", insertable = false, updatable = false)
		@JoinColumn(name = "id_three", referencedColumnName = "id_three_2", insertable = false, updatable = false)
		LeftEntity aLeftEntity;

		@OneToOne(mappedBy = "midEntity")
		RightEntity rightEntity;
	}

	@Entity(name = "RightEntity")
	static class RightEntity {

		@Id
		Long id;

		@Column
		String id_three;

		@OneToOne
		@JoinColumn(name = "id_one", referencedColumnName = "id_one", insertable = false, updatable = false)
		@JoinColumn(name = "id_two", referencedColumnName = "id_two", insertable = false, updatable = false)
		MidEntity midEntity;
	}
}
