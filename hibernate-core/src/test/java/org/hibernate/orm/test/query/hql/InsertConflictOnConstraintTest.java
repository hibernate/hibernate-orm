package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = InsertConflictOnConstraintTest.Constrained.class)
@RequiresDialect(PostgreSQLDialect.class)
public class InsertConflictOnConstraintTest {
	@Test void test(SessionFactoryScope scope) {
		scope.inTransaction( s -> s.persist(new Constrained()));
		scope.inTransaction( s -> s.createMutationQuery("insert into Constrained(id, name, count) values (4,'Gavin',69) on conflict on constraint constrained_count_name_key do update set count = 96").executeUpdate());
		scope.inSession( s -> assertEquals(96, s.createSelectionQuery("select count from Constrained", int.class).getSingleResult()));

	}

	@Entity(name = "Constrained")
	@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"count","name"}))
	static class Constrained {
		@Id
		@GeneratedValue
		long id;
		String name = "Gavin";
		int count = 69;
	}

}
