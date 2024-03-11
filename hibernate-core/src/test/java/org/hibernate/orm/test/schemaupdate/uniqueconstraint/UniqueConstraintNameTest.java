package org.hibernate.orm.test.schemaupdate.uniqueconstraint;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@SessionFactory
@DomainModel(annotatedClasses = {UniqueConstraintNameTest.MyEntity.class, UniqueConstraintNameTest.MyOtherEntity.class})
@JiraKey("HHH-17825")
@JiraKey("HHH-17132")
public class UniqueConstraintNameTest {

	@Test void test(SessionFactoryScope scope) {
		scope.getSessionFactory();
	}

	@Entity
	@Table(name = "my_entity",
			uniqueConstraints =
					@UniqueConstraint(
							name = "my_other_entity_id_unique",
							columnNames = "my_other_entity_id"
					),
			indexes = @Index(name = "some_long_index", columnList = "some_long"))
	static class MyEntity {

		@Id
		@GeneratedValue
		long id;

		@OneToOne(fetch = FetchType.LAZY, optional = false)
		@JoinColumn(name = "my_other_entity_id",
				updatable = false,
				foreignKey = @ForeignKey(name = "FK_moe"))
		private MyOtherEntity myOtherEntity;

		@Column(name = "some_long")
		private long someLong;

	}

	@Entity
	@Table(name = "my_other_entity")
	static class MyOtherEntity {
		@Id
		@GeneratedValue
		long id;

		@Column(name = "some_string")
		private String someString;
	}
}
