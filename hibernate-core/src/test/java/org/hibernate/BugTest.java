package org.hibernate;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

@SessionFactory
@DomainModel (annotatedClasses = {BugTest.MyInstance.class, BugTest.My.class})
public class BugTest {

	@Test void test(SessionFactoryScope scope) {
		scope.inTransaction( s -> s.persist( new My() ) );
		scope.inTransaction(
				s -> s.createMutationQuery("update My m set m.smallintCol = cast(1 as Short)")
						.executeUpdate()
		);
	}

	@MappedSuperclass
	static abstract class MyInstance {
		@Column(name = "iface_ids", columnDefinition = "TEXT")
		@Convert(converter = StringListToStringConverter.class)
		List<String> interfaceIds = List.of("a", "b", "c");
	}

	@Entity(name="My")
	static class My extends MyInstance {
		@Id @GeneratedValue
		Long id;

		@Column(name = "smallint_col", nullable = false)
		private short smallintCol;
	}
}
