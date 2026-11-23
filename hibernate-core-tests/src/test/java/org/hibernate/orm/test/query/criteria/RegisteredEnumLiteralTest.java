package org.hibernate.orm.test.query.criteria;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-21072")
@DomainModel(
		annotatedClasses = EnumLiteralWithoutMappingTest.LiteralEntity.class,
		typeContributors = RegisteredEnumLiteralTest.EnumTypeContributor.class
)
@SessionFactory
@org.hibernate.testing.orm.junit.ServiceRegistry(
		settings = @Setting(name = AvailableSettings.CRITERIA_VALUE_HANDLING_MODE, value = "inline")
)
public class RegisteredEnumLiteralTest {
	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new EnumLiteralWithoutMappingTest.LiteralEntity( 1L ) ) );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testConstantBodyThenResult(SessionFactoryScope scope) {
		assertCaseResult( scope, Status.CLOSED, 1L, Status.CLOSED );
	}

	@Test
	void testConstantBodyElseResult(SessionFactoryScope scope) {
		assertCaseResult( scope, Status.CLOSED, 2L, Status.OPEN );
	}

	@Test
	void testOrdinaryConstantThenResult(SessionFactoryScope scope) {
		assertCaseResult( scope, Status.OPEN, 1L, Status.OPEN );
	}

	@Test
	void testOrdinaryConstantElseResult(SessionFactoryScope scope) {
		assertCaseResult( scope, Status.OPEN, 2L, Status.OPEN );
	}

	private void assertCaseResult(SessionFactoryScope scope, Status thenValue, long matchingId, Status expected) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( Status.class );
			final var root = query.from( EnumLiteralWithoutMappingTest.LiteralEntity.class );
			query.select( builder.<Status>selectCase()
					.when( builder.equal( root.get( "id" ), matchingId ), thenValue )
					.otherwise( Status.OPEN ) );
			assertThat( session.createQuery( query ).getSingleResult() ).isEqualTo( expected );
		} );
	}

	enum Status {
		OPEN, CLOSED {}
	}

	public static class EnumTypeContributor implements TypeContributor {
		@Override
		public void contribute(TypeContributions contributions, ServiceRegistry serviceRegistry) {
			contributions.contributeType( new AbstractSingleColumnStandardBasicType<Status>(
					VarcharJdbcType.INSTANCE, new EnumJavaType<>( Status.class ) ) {
				@Override
				public String getName() {
					return Status.class.getName();
				}
			} );
		}
	}
}
