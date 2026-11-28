package org.hibernate.orm.test.query;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A parameter whose type cannot be determined from the query (here, an argument of {@code coalesce()})
 * gets its bind type from the bound value. For a value like {@link HashMap}, the resolved bind type is the
 * basic type for {@link java.io.Serializable}, which then prevents the parameter type from being refined
 * to the type of the converted attribute it is compared with/assigned to. As a consequence the value is
 * bound as a serialized {@code VARBINARY} instead of going through the {@link AttributeConverter}.
 */
@DomainModel(annotatedClasses = SerializableParameterTypeInferenceTest.MapHolder.class)
@SessionFactory
@JiraKey("HHH-20131")
public class SerializableParameterTypeInferenceTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var holder = new MapHolder();
			holder.id = 1L;
			holder.data = Map.of( "a", "1", "b", "2" );
			session.persist( holder );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@FailureExpected(jiraKey = "HHH-20131")
	void testSerializableParameterInPredicate(SessionFactoryScope scope) {
		final var parameter = new HashMap<String, String>();
		parameter.put( "a", "1" );
		parameter.put( "b", "2" );
		scope.inTransaction( session -> assertEquals(
				1L,
				session.createQuery(
								"select count(*) from MapHolder h where h.data = coalesce(:p, h.data)",
								Long.class
						)
						.setParameter( "p", parameter )
						.getSingleResult()
		) );
	}

	@Test
	@FailureExpected(jiraKey = "HHH-20131")
	void testSerializableParameterInAssignment(SessionFactoryScope scope) {
		final var parameter = new HashMap<String, String>();
		parameter.put( "c", "3" );
		scope.inTransaction( session -> assertEquals(
				1,
				session.createMutationQuery( "update MapHolder h set h.data = coalesce(:p, h.data)" )
						.setParameter( "p", parameter )
						.executeUpdate()
		) );
		scope.inTransaction( session -> assertEquals(
				Map.of( "c", "3" ),
				session.find( MapHolder.class, 1L ).data
		) );
	}

	@Entity(name = "MapHolder")
	public static class MapHolder {
		@Id
		Long id;
		@Convert(converter = MapConverter.class)
		Map<String, String> data;
	}

	@Converter
	public static class MapConverter implements AttributeConverter<Map<String, String>, String> {
		@Override
		public String convertToDatabaseColumn(Map<String, String> attribute) {
			if ( attribute == null ) {
				return null;
			}
			final var sb = new StringBuilder();
			for ( var entry : new TreeMap<>( attribute ).entrySet() ) {
				if ( !sb.isEmpty() ) {
					sb.append( ';' );
				}
				sb.append( entry.getKey() ).append( '=' ).append( entry.getValue() );
			}
			return sb.toString();
		}

		@Override
		public Map<String, String> convertToEntityAttribute(String dbData) {
			if ( dbData == null ) {
				return null;
			}
			final var map = new HashMap<String, String>();
			if ( !dbData.isEmpty() ) {
				for ( var part : dbData.split( ";" ) ) {
					final var keyValue = part.split( "=", 2 );
					map.put( keyValue[0], keyValue[1] );
				}
			}
			return map;
		}
	}
}
