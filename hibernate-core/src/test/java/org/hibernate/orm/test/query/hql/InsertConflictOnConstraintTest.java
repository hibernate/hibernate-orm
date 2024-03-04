package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = InsertConflictOnConstraintTest.Constrained.class)
public class InsertConflictOnConstraintTest {

	@RequiresDialect(PostgreSQLDialect.class)
	@Test void testDoUpdate(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
		scope.inTransaction( s -> s.persist(new Constrained()));
		scope.inTransaction( s -> s.createMutationQuery("insert into Constrained(id, name, count) values (4,'Gavin',69) on conflict on constraint count_name_key do update set count = 96").executeUpdate());
		scope.inSession( s -> assertEquals(96, s.createSelectionQuery("select count from Constrained", int.class).getSingleResult()));
	}

	@RequiresDialect( PostgreSQLDialect.class )
	@RequiresDialect( OracleDialect.class )
	@RequiresDialect( SQLServerDialect.class )
	@RequiresDialect( MySQLDialect.class )
	@RequiresDialect( HSQLDialect.class )
	@RequiresDialect( DerbyDialect.class )
	@Test void testDoNothing(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
		scope.inTransaction( s -> s.persist(new Constrained()));
		scope.inTransaction( s -> s.createMutationQuery("insert into Constrained(id, name, count) values (4,'Gavin',69) on conflict on constraint count_name_key do nothing").executeUpdate());
		scope.inSession( s -> assertEquals(69, s.createSelectionQuery("select count from Constrained", int.class).getSingleResult()));
	}

	@RequiresDialect( H2Dialect.class )
	@Test void testDoNothing2(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
		scope.inTransaction( s -> s.persist(new Constrained()));
		scope.inTransaction( s -> s.createMutationQuery("insert into Constrained(id, name, count) values (4,'Gavin',69) on conflict on constraint count_name_key_index_2 do nothing").executeUpdate());
		scope.inSession( s -> assertEquals(69, s.createSelectionQuery("select count from Constrained", int.class).getSingleResult()));
	}

	@Entity(name = "Constrained")
	@Table(uniqueConstraints = @UniqueConstraint(name = "count_name_key", columnNames = {"count","name"}))
	static class Constrained {
		@Id
		@GeneratedValue
		long id;
		String name = "Gavin";
		int count = 69;
	}

}
