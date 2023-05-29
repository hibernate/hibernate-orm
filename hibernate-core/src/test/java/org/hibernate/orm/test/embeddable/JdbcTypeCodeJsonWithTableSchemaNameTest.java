package org.hibernate.orm.test.embeddable;

import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.Test;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import jakarta.transaction.Transactional;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.jupiter.api.Assertions.assertEquals;

@RequiresDialect(PostgreSQLDialect.class)
@JiraKey( "HHH-16612" )
public class JdbcTypeCodeJsonWithTableSchemaNameTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				MyEntity.class,
				MyJson.class
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.JAKARTA_HBM2DDL_CREATE_SCHEMAS, true );
		super.addConfigOptions( options );
	}

	private static final long PK = 123L;

	@Test
	@Transactional
	@JiraKey( "HHH-16612" )
	public void shouldQueryMyEntity() throws Exception {
		doInJPA( this::entityManagerFactory, entityManager -> {
			insert( entityManager );
			findAndUpdate( entityManager );
			List<MyEntity> resultWithoutFilter = selectWithoutFilter( entityManager );
			selectFiltered( entityManager );
		} );
	}

	@Transactional
	void findAndUpdate(EntityManager em) {
		MyEntity found = em.find( MyEntity.class, PK );
		found.jsonProperty.setStringProp( "TEST" );
	}

	@Transactional
	void selectFiltered(EntityManager em) {
		//then
		List<Tuple> resultWithFilter = em
				.createQuery( "SELECT e FROM MyEntity e WHERE e.jsonProperty.longProp = :x", Tuple.class )
				.setParameter( "x", 100L )
				.getResultList();
		assertEquals( 1, resultWithFilter.size() );
	}


	@Transactional
	List<MyEntity> selectWithoutFilter(EntityManager em) {
		List<MyEntity> resultWithoutFilter = em
				.createQuery( "SELECT e FROM MyEntity e", MyEntity.class )
				.getResultList();
		assertEquals( 1, resultWithoutFilter.size() );
		return resultWithoutFilter;
	}

	@Transactional(Transactional.TxType.REQUIRES_NEW)
	void insert(EntityManager em) {
		//given
		MyEntity myEntity = new MyEntity();
		myEntity.id = PK;
		MyJson myJson = new MyJson();
		myJson.setLongProp( 100L );
		myJson.setStringProp( "Hallo" );
		myEntity.jsonProperty = myJson;
//        myEntity.jsonProperty = new HashMap<>(Map.of("x", "Y"));
		//when
		em.persist( myEntity );
	}

	@Entity(name = "MyEntity")
	@Table(name = "MY_ENTITY", schema = "base") //with explict table-name & schema it won't work
	public static class MyEntity {

		@Id
		@Column
		public Long id;

		@JdbcTypeCode(SqlTypes.JSON)
		MyJson jsonProperty;
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
