package org.hibernate.orm.test.ondelete.toone;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = {
		JoinedSelfReferenceCustomDeleteTest.Base.class,
		JoinedSelfReferenceCustomDeleteTest.Child.class
})
@ServiceRegistry(settings = @Setting(name = "hibernate.flush.queue.type", value = "graph"))
@SessionFactory
@RequiresDialect(H2Dialect.class)
public class JoinedSelfReferenceCustomDeleteTest {
	@Test
	void customDeleteKeepsRowsAndSelfReference(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var child = new Child();
			child.id = 1;
			session.persist( child );
			session.flush();
			child.self = child;
		} );

		scope.inTransaction( session -> session.remove( session.find( Child.class, 1 ) ) );

		scope.inTransaction( session -> {
			assertEquals( 1, session.createNativeQuery(
					"select self_id from joined_custom_base where id = 1", Integer.class
			).getSingleResult() );
			assertEquals( 1, session.createNativeQuery(
					"select base_deleted from joined_custom_base where id = 1", Integer.class
			).getSingleResult() );
			assertEquals( 1, session.createNativeQuery(
					"select child_deleted from joined_custom_child where id = 1", Integer.class
			).getSingleResult() );
		} );
	}

	@Entity(name = "JoinedCustomBase")
	@Table(name = "joined_custom_base")
	@Inheritance(strategy = InheritanceType.JOINED)
	@SQLDelete(sql = "update joined_custom_base set base_deleted = 1 where id = ?")
	public static class Base {
		@Id
		Integer id;
		@Column(name = "base_deleted")
		int baseDeleted;
		@ManyToOne
		@JoinColumn(name = "self_id")
		@OnDelete(action = OnDeleteAction.CASCADE)
		Child self;
	}

	@Entity(name = "JoinedCustomChild")
	@Table(name = "joined_custom_child")
	@SQLDelete(sql = "update joined_custom_child set child_deleted = 1 where id = ?")
	public static class Child extends Base {
		@Column(name = "child_deleted")
		int childDeleted;
	}
}
