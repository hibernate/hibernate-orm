package org.hibernate.orm.test.schemaupdate.uniqueconstraint;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@SessionFactory
@DomainModel(annotatedClasses = {MultiUniqueConstraintNameTest.MyEntity.class, MultiUniqueConstraintNameTest.MyOtherEntity.class})
public class MultiUniqueConstraintNameTest {

	@Test void test(SessionFactoryScope scope) {
		scope.getSessionFactory();
	}

	@Entity
	@Table(name = "my_entity",
			uniqueConstraints =
					@UniqueConstraint(
							name = "my_other_entity_id_unique",
							columnNames = {"my_other_entity_id1","my_other_entity_id2"}
					),
			indexes = @Index(name = "some_long_index", columnList = "some_long"))
	static class MyEntity {

		@Id
		long id1;
		@Id
		long id2;

		@OneToOne(fetch = FetchType.LAZY, optional = false)
		@JoinColumn(name = "my_other_entity_id1")
		@JoinColumn(name = "my_other_entity_id2")
		private MyOtherEntity myOtherEntity;

		@Column(name = "some_long")
		private long someLong;

	}

	@Entity
	@Table(name = "my_other_entity")
	static class MyOtherEntity {
		@Id
		long id1;
		@Id
		long id2;

		@Column(name = "some_string")
		private String someString;
	}
}
