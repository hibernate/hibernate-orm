/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.converted.converter;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = {EntityWithEnum.class} )
@SessionFactory
public class AttributeConverterAndFunctionTest {

	@BeforeAll
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new EntityWithEnum( 1L, State.SOMESTATE ) );
				}
		);
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete from EntityWithEnum" ).executeUpdate();
				}
		);
	}

	@Test
	@JiraKey( "HHH-18474" )
	@FailureExpected
	public void testFunctionOnConvertedEnum(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					CriteriaBuilder cb = session.getCriteriaBuilder();
					CriteriaQuery<Tuple> cq = cb.createTupleQuery();
					final Root<EntityWithEnum> from = cq.from( EntityWithEnum.class );

					Path<State> state = from.get( EntityWithEnum_.state );
					cq.multiselect( cb.function("max", State.class, state) );
					final TypedQuery<Tuple> query = session.createQuery( cq );
					final List<Tuple> results = query.getResultList();

					// The result should be SOMESTATE
					final State resultState = results.get(0).get(0, State.class);
					assertEquals(State.SOMESTATE, resultState);
				}
		);
	}

	public enum State {
		SOMESTATE(2),
		OTHER(3),
		EVENMORE(1);

		private static final Map<Integer, State> LOOKUP = new HashMap<>();

		static {
			for (State type : EnumSet.allOf( State.class)) {
				LOOKUP.put(type.getValue(), type);
			}
		}
		private final Integer value;

		State(final Integer value) {
			this.value = value;
		}

		public Integer getValue() {
			return value;
		}

		public static State getEnum(final Integer value) {
			return LOOKUP.get(value);
		}
	}

	@Converter
	public static class StateJpaConverter implements AttributeConverter<State, Integer> {

		@Override
		public Integer convertToDatabaseColumn(final State state) {
			return state == null ? null : state.getValue();
		}

		@Override
		public State convertToEntityAttribute(final Integer dbData) {
			return dbData == null ? null : State.getEnum( dbData);
		}
	}

}
