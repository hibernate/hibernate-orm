package org.hibernate.orm.test.query.returns;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.internal.util.MutableInteger;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.assertj.core.api.Assertions;

/**
 * @author Nathan Xu
 */
@DomainModel(annotatedClasses = BasicEntity.class)
@SessionFactory
@TestForIssue(jiraKey = "HHH-15149")
class ResultUniqueListTest {

	private final List<String> values = Arrays.asList( "v1", "v1", "v2", null, "v3", null, "v3", "v4", "v4" );
	private final List<String> expectedUniqueValues = Arrays.asList( "v1", "v2", null, "v3", "v4" );

	@BeforeEach
	void setUpTestData(SessionFactoryScope scope) {
		final MutableInteger id = new MutableInteger();
		final List<BasicEntity> entities = values.stream().map( v -> new BasicEntity( id.incrementAndGet(), v ) ).collect( Collectors.toList() );
		scope.inTransaction( session -> entities.forEach( session::persist ) );
	}

	@Test
	void testUniqueList(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<String> uniqueValues = session.createQuery( "SELECT data FROM BasicEntity", String.class )
					.uniqueList();
			Assertions.assertThat( uniqueValues ).isEqualTo( expectedUniqueValues );
		} );
	}

	@AfterEach
	void cleanUpTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "delete BasicEntity" ).executeUpdate()
		);
	}

}
