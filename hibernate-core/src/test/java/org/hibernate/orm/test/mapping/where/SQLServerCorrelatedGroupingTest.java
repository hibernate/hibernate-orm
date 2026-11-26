package org.hibernate.orm.test.mapping.where;

import org.hibernate.Session;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.orm.domain.gambit.EmbeddedIdEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-21078")
@RequiresDialect(SQLServerDialect.class)
@DomainModel(annotatedClasses = EmbeddedIdEntity.class)
@SessionFactory
class SQLServerCorrelatedGroupingTest {
	@BeforeEach
	void createData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			persist( session, 1, "a" );
			persist( session, 2, "a" );
			persist( session, 3, "bb" );
		} );
	}

	private static void persist(Session session, int id, String data) {
		final var entity = new EmbeddedIdEntity();
		entity.setId( new EmbeddedIdEntity.EmbeddedIdEntityId( id, data ) );
		entity.setData( data );
		session.persist( entity );
	}

	@AfterEach
	void dropData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void groupingExpressionsWithOuterAndLocalColumns(SessionFactoryScope scope) {
		final var ids = scope.fromTransaction( session -> session.createSelectionQuery( """
				select e.id.value1 from EmbeddedIdEntity e where exists (
					select count(*) from EmbeddedIdEntity x where x.id.value1 <= e.id.value1
					group by upper(e.data), e.id.value1 + length(x.data) having count(*) = 2
				) order by e.id.value1
				""", Integer.class ).getResultList() );
		assertThat( ids ).containsExactly( 2, 3 );
	}

	@Test
	void compositeOuterKeyAndLocalKey(SessionFactoryScope scope) {
		final var ids = scope.fromTransaction( session -> session.createSelectionQuery( """
				select e.id.value1 from EmbeddedIdEntity e where exists (
					select count(*) from EmbeddedIdEntity x where x.id.value1 <= e.id.value1
					group by e.id, x.data having count(*) = 2
				) order by e.id.value1
				""", Integer.class ).getResultList() );
		assertThat( ids ).containsExactly( 2, 3 );
	}
}
