package org.hibernate.orm.test.embeddable;

import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = {
		JdbcTypeCodeJsonWithTableSchemaNameTest.MyEntity.class,
		JdbcTypeCodeJsonWithTableSchemaNameTest.MyJson.class
})
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.JAKARTA_HBM2DDL_CREATE_SCHEMAS, value = "true")
		}
)
@RequiresDialect(PostgreSQLDialect.class)
@JiraKey("HHH-16612")
public class JdbcTypeCodeJsonWithTableSchemaNameTest {

	@Test
	@Transactional
	@JiraKey("HHH-16612")
	public void shouldQueryMyEntity(DomainModelScope domainModelScope, SessionFactoryScope sessionFactoryScope) {
		final var pk = sessionFactoryScope.fromTransaction(
				session -> {
					MyEntity myEntity = new MyEntity();
					MyJson myJson = new MyJson();
					myJson.setLongProp( 100L );
					myJson.setStringProp( "Hallo" );
					myEntity.jsonProperty = myJson;
					session.persist( myEntity );
					return myEntity.id;
				} );

		sessionFactoryScope.inTransaction(
				session -> {
					MyEntity found = session.find( MyEntity.class, pk );
					found.jsonProperty.setStringProp( "TEST" );
				} );

		sessionFactoryScope.inSession(
				session -> {
					List<MyEntity> resultWithoutFilter =
							session
									.createQuery( "SELECT e FROM MyEntity e", MyEntity.class )
									.getResultList();
					assertEquals( 1, resultWithoutFilter.size() );
				}
		);

		sessionFactoryScope.inSession(
				session -> {
					List<Tuple> resultWithFilter = session
							.createQuery( "SELECT e FROM MyEntity e WHERE e.jsonProperty.stringProp = :x", Tuple.class )
							.setParameter( "x", "TEST" )
							.getResultList();
					assertEquals( 1, resultWithFilter.size() );
				}
		);

		sessionFactoryScope.inSession(
				session -> {
					List<Tuple> resultWithFilter = session
							.createQuery( "SELECT e FROM MyEntity e WHERE e.jsonProperty.longProp = :x", Tuple.class )
							.setParameter( "x", 100L )
							.getResultList();
					assertEquals( 1, resultWithFilter.size() );
				}
		);

		sessionFactoryScope.inSession(
				session -> {
					List<Tuple> resultWithFilter = session
							.createQuery( "SELECT e FROM MyEntity e WHERE e.jsonProperty.longProp = :x", Tuple.class )
							.setParameter( "x", 200L )
							.getResultList();
					assertEquals( 0, resultWithFilter.size() );
				}
		);
	}

	@Entity(name = "MyEntity")
	@Table(name = "MY_ENTITY", schema = "base") //with explict table-name & schema it won't work
	public static class MyEntity {

		@Id
		@GeneratedValue
		public Long id;

		@JdbcTypeCode(SqlTypes.JSON)
		MyJson jsonProperty;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public MyJson getJsonProperty() {
			return jsonProperty;
		}

		public void setJsonProperty(MyJson jsonProperty) {
			this.jsonProperty = jsonProperty;
		}
	}

	@Embeddable
	@Access(AccessType.PROPERTY)
	public static class MyJson {

		private String stringProp;
		private Long longProp;

		public String getStringProp() {
			return stringProp;
		}

		public void setStringProp(String aStringProp) {
			this.stringProp = aStringProp;
		}

		public Long getLongProp() {
			return longProp;
		}

		public void setLongProp(Long aLongProp) {
			this.longProp = aLongProp;
		}
	}
}
