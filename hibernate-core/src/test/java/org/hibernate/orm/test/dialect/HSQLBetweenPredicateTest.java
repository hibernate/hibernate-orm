package org.hibernate.orm.test.dialect;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import org.hibernate.dialect.HSQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@RequiresDialect(HSQLDialect.class)
@DomainModel(annotatedClasses = HSQLBetweenPredicateTest.IndexedValue.class)
@SessionFactory
class HSQLBetweenPredicateTest {
	@Test
	void indexedColumnAsLowerBound(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new IndexedValue( 1, "21" ) );
			session.persist( new IndexedValue( 2, "12" ) );
			session.persist( new IndexedValue( 3, null ) );
		} );
		scope.inTransaction( session -> {
			assertThat( session.createSelectionQuery(
					"select v.id from IndexedValue v where v.code between v.code and :upper", Integer.class )
					.setParameter( "upper", "12" ).getResultList() ).containsExactly( 2 );
			assertThat( session.createSelectionQuery(
					"select v.id from IndexedValue v where v.code not between v.code and :upper", Integer.class )
					.setParameter( "upper", "12" ).getResultList() ).containsExactly( 1 );
		} );
	}

	@Entity(name = "IndexedValue")
	@Table(name = "indexed_value", indexes = @Index(columnList = "code"))
	static class IndexedValue {
		@Id int id;
		String code;

		IndexedValue() {
		}

		IndexedValue(int id, String code) {
			this.id = id;
			this.code = code;
		}
	}
}
