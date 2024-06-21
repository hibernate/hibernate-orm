/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.sql;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Tuple;

import org.hibernate.Session;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.loader.NonUniqueDiscoveredSqlAliasException;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.query.TupleTransformer;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.testing.orm.domain.userguide.Account;
import org.hibernate.testing.orm.domain.userguide.AddressType;
import org.hibernate.testing.orm.domain.userguide.Call;
import org.hibernate.testing.orm.domain.userguide.CreditCardPayment;
import org.hibernate.testing.orm.domain.userguide.Partner;
import org.hibernate.testing.orm.domain.userguide.Person;
import org.hibernate.testing.orm.domain.userguide.PersonNames;
import org.hibernate.testing.orm.domain.userguide.PersonPhoneCount;
import org.hibernate.testing.orm.domain.userguide.Phone;
import org.hibernate.testing.orm.domain.userguide.PhoneType;
import org.hibernate.testing.orm.domain.userguide.WireTransferPayment;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class SQLTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class,
			Partner.class,
            Phone.class,
			Call.class,
			Account.class,
			CreditCardPayment.class,
			WireTransferPayment.class,
			SpaceShip.class,
			Captain.class,
		};
	}

	@Before
	public void init() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Person person1 = new Person("John Doe");
			person1.setNickName("JD");
			person1.setAddress("Earth");
			person1.setCreatedOn(LocalDateTime.of(2000, 1, 1, 0, 0, 0)) ;
			person1.getAddresses().put(AddressType.HOME, "Home address");
			person1.getAddresses().put(AddressType.OFFICE, "Office address");
			entityManager.persist(person1);

			Person person2 = new Person("Mrs. John Doe");
			person2.setAddress("Earth");
			person2.setCreatedOn(LocalDateTime.of(2000, 1, 2, 12, 0, 0)) ;
			entityManager.persist(person2);

			Person person3 = new Person("Dr_ John Doe");
			entityManager.persist(person3);

			Phone phone1 = new Phone("123-456-7890");
			phone1.setId(1L);
			phone1.setType(PhoneType.MOBILE);
			person1.addPhone(phone1);
			phone1.getRepairTimestamps().add(LocalDateTime.of(2005, 1, 1, 12, 0, 0));
			phone1.getRepairTimestamps().add(LocalDateTime.of(2006, 1, 1, 12, 0, 0));

			Call call11 = new Call();
			call11.setDuration(12);
			call11.setTimestamp(LocalDateTime.of(2000, 1, 1, 0, 0, 0));

			Call call12 = new Call();
			call12.setDuration(33);
			call12.setTimestamp(LocalDateTime.of(2000, 1, 1, 1, 0, 0));

			phone1.addCall(call11);
			phone1.addCall(call12);

			Phone phone2 = new Phone("098_765-4321");
			phone2.setId(2L);
			phone2.setType(PhoneType.LAND_LINE);

			Phone phone3 = new Phone("098-765-4320");
			phone3.setId(3L);
			phone3.setType(PhoneType.LAND_LINE);

			person2.addPhone(phone2);
			person2.addPhone(phone3);

			CreditCardPayment creditCardPayment = new CreditCardPayment();
			creditCardPayment.setCompleted(true);
			creditCardPayment.setAmount(BigDecimal.ZERO);
			creditCardPayment.setPerson(person1);

			WireTransferPayment wireTransferPayment = new WireTransferPayment();
			wireTransferPayment.setCompleted(true);
			wireTransferPayment.setAmount(BigDecimal.valueOf(100));
			wireTransferPayment.setPerson(person2);

			entityManager.persist(creditCardPayment);
			entityManager.persist(wireTransferPayment);

			Partner partner = new Partner("John Doe");
			entityManager.persist(partner);

			Captain captain = new Captain();
			captain.setId(new Identity());
			captain.getId().setFirstname("Jean-Luc");
			captain.getId().setLastname("Picard");

			entityManager.persist(captain);

			SpaceShip spaceShip = new SpaceShip();
			spaceShip.setName("Enterprise");
			spaceShip.setDimensions(new Dimensions());
			spaceShip.getDimensions().setLength(100);
			spaceShip.getDimensions().setWidth(20);
			spaceShip.setModel("E-1");
			spaceShip.setSpeed(150);
			spaceShip.setCaptain(captain);
			entityManager.persist(spaceShip);
		});
	}

	@Test
	public void test_sql_jpa_all_columns_scalar_query_example() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::sql-jpa-all-columns-scalar-query-example[]
			List<Object[]> persons = entityManager.createNativeQuery(
				"SELECT * FROM Person")
			.getResultList();
			//end::sql-jpa-all-columns-scalar-query-example[]
			assertEquals(3, persons.size());
		});
	}

	@Test
	public void test_sql_jpa_custom_column_selection_scalar_query_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::sql-jpa-custom-column-selection-scalar-query-example[]
			List<Object[]> persons = entityManager.createNativeQuery(
				"SELECT id, name FROM Person")
			.getResultList();

			for(Object[] person : persons) {
				Number id = (Number) person[0];
				String name = (String) person[1];
			}
			//end::sql-jpa-custom-column-selection-scalar-query-example[]
			assertEquals(3, persons.size());
		});
	}

	@Test
	public void test_sql_hibernate_query_scalar_example() {

        doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			//tag::sql-hibernate-all-columns-scalar-query-example[]
			List<Object[]> persons = session.createNativeQuery(
				"SELECT * FROM Person", Object[].class)
			.list();
			//end::sql-hibernate-all-columns-scalar-query-example[]
			assertEquals(3, persons.size());
		});
	}

	@Test
	@TestForIssue( jiraKey = "HHH-15914" )
	public void test_sql_hibernate_custom_column_selection_scalar_query_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			//tag::sql-hibernate-custom-column-selection-scalar-query-example[]
			List<Object[]> persons = session.createNativeQuery(
				"SELECT id, name FROM Person", Object[].class)
			.list();

			for(Object[] person : persons) {
				Number id = (Number) person[0];
				String name = (String) person[1];
			}
			//end::sql-hibernate-custom-column-selection-scalar-query-example[]
			assertEquals(3, persons.size());
		});
	}

	@Test
	public void test_sql_hibernate_query_scalar_explicit_result_set_example() {
        doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			//tag::sql-hibernate-scalar-query-explicit-result-set-example[]
			List<Object[]> persons = session.createNativeQuery(
				"SELECT * FROM Person", Object[].class)
			.addScalar("id", StandardBasicTypes.LONG)
			.addScalar("name", StandardBasicTypes.STRING)
			.list();

			for(Object[] person : persons) {
				Long id = (Long) person[0];
				String name = (String) person[1];
			}
			//end::sql-hibernate-scalar-query-explicit-result-set-example[]
			assertEquals(3, persons.size());
		});
	}

	@Test
	public void test_sql_hibernate_query_scalar_partial_explicit_result_set_example() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			//tag::sql-hibernate-scalar-query-partial-explicit-result-set-example[]
			List<Object[]> persons = session.createNativeQuery(
				"SELECT * FROM Person", Object[].class)
			.addScalar("id", StandardBasicTypes.LONG)
			.addScalar("name")
			.list();

			for(Object[] person : persons) {
				Long id = (Long) person[0];
				String name = (String) person[1];
			}
			//end::sql-hibernate-scalar-query-partial-explicit-result-set-example[]
			assertEquals(3, persons.size());
		});
	}

	@Test
	public void test_sql_jpa_entity_query_example() {
        doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::sql-jpa-entity-query-example[]
			List<Person> persons = entityManager.createNativeQuery(
				"SELECT * FROM Person", Person.class)
			.getResultList();
			//end::sql-jpa-entity-query-example[]
			assertEquals(3, persons.size());
		});
	}

	@Test
	public void test_sql_hibernate_entity_query_example() {
        doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			//tag::sql-hibernate-entity-query-example[]
			List<Person> persons = session.createNativeQuery(
				"SELECT * FROM Person", Person.class)
			.list();
			//end::sql-hibernate-entity-query-example[]
			assertEquals(3, persons.size());
		});
	}

	@Test
	public void test_sql_jpa_entity_query_explicit_result_set_example() {
        doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::sql-jpa-entity-query-explicit-result-set-example[]
			List<Person> persons = entityManager.createNativeQuery(
				"SELECT id, name, nick_name, address, created_on, version " +
				"FROM Person", Person.class)
			.getResultList();
			//end::sql-jpa-entity-query-explicit-result-set-example[]
			assertEquals(3, persons.size());
		});
	}

	@Test
	public void test_sql_hibernate_entity_query_explicit_result_set_example() {
        doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			//tag::sql-hibernate-entity-query-explicit-result-set-example[]
			List<Person> persons = session.createNativeQuery(
				"SELECT id, name, nick_name, address, created_on, version " +
				"FROM Person", Person.class)
			.list();
			//end::sql-hibernate-entity-query-explicit-result-set-example[]
			assertEquals(3, persons.size());
		});
	}

	@Test
	public void test_sql_jpa_entity_associations_query_many_to_one_example() {
        doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::sql-jpa-entity-associations-query-many-to-one-example[]
			List<Phone> phones = entityManager.createNativeQuery(
				"SELECT id, phone_number, phone_type, person_id " +
				"FROM Phone", Phone.class)
			.getResultList();
			//end::sql-jpa-entity-associations-query-many-to-one-example[]
			assertEquals(3, phones.size());
		});
	}

	@Test
	public void test_sql_hibernate_entity_associations_query_many_to_one_example() {
        doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			//tag::sql-hibernate-entity-associations-query-many-to-one-example[]
			List<Phone> phones = session.createNativeQuery(
				"SELECT id, phone_number, phone_type, person_id " +
				"FROM Phone", Phone.class)
			.list();
			//end::sql-hibernate-entity-associations-query-many-to-one-example[]
			assertEquals(3, phones.size());
		});
	}

	@Test
	public void test_sql_jpa_entity_associations_query_many_to_one_join_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			List<Phone> phones = entityManager.createNativeQuery(
				"SELECT ph.* " +
				"FROM Phone ph " +
				"JOIN Person pr ON ph.person_id = pr.id", Phone.class)
			.getResultList();

			for(Phone phone : phones) {
				assertNotNull(phone.getPerson().getName());
			}
			assertEquals(3, phones.size());
		});
	}

	@Test
	public void test_sql_hibernate_entity_associations_query_many_to_one_join_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			//tag::sql-hibernate-entity-associations-query-many-to-one-join-example[]
			List<Phone> tuples = session.createNativeQuery(
				"SELECT {ph.*}, {pr.*} " +
				"FROM Phone ph " +
				"JOIN Person pr ON ph.person_id = pr.id", Phone.class, "ph")
			.addJoin("pr", "ph.person")
			.list();

			for (Phone phone : tuples) {
				assertNotNull(phone.getPerson().getName());
			}
			//end::sql-hibernate-entity-associations-query-many-to-one-join-example[]
			assertEquals(3, tuples.size());
		});
	}

	@Test
	public void test_sql_hibernate_entity_associations_query_many_to_one_join_result_transformer_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			//tag::sql-hibernate-entity-associations-query-many-to-one-join-tuple-transformer-example[]
			List<Phone> phones = session.createNativeQuery(
		"SELECT {ph.*}, {pr.*} " +
				"FROM Phone ph " +
				"JOIN Person pr ON ph.person_id = pr.id", Phone.class, "ph")
			.addJoin("pr", "ph.person")
			.setTupleTransformer( (TupleTransformer<Phone>) (tuple, aliases) -> (Phone) tuple[0] )
			.list();

			for (Phone person : phones) {
				person.getPerson();
			}
			//end::sql-hibernate-entity-associations-query-many-to-one-join-tuple-transformer-example[]
			assertEquals(3, phones.size());
		});
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	@RequiresDialect(OracleDialect.class)
	@RequiresDialect(PostgreSQLDialect.class)
	public void test_sql_jpa_entity_associations_query_one_to_many_join_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::sql-jpa-entity-associations-query-one-to-many-join-example[]
			List<Phone> phones = entityManager.createNativeQuery(
				"SELECT ph.* " +
				"FROM Phone ph " +
				"JOIN phone_call c ON c.phone_id = ph.id", Phone.class)
			.getResultList();

			for (Phone phone : phones) {
				List<Call> calls = phone.getCalls();
			}
			//end::sql-jpa-entity-associations-query-one-to-many-join-example[]
			assertEquals(2, phones.size());
		});
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10504")
	public void test_sql_hibernate_entity_associations_query_one_to_many_join_example_1() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			List<Phone> phones = session.createNativeQuery(
				"SELECT {ph.*}, {c.*} " +
				"FROM Phone ph " +
				"JOIN phone_call c ON c.phone_id = ph.id", Phone.class, "ph")
			.addJoin("c", "ph.calls")
			.list();

			for (Phone phone : phones) {
				List<Call> calls = phone.getCalls();
			}
			assertEquals(2, phones.size());
		});
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	@RequiresDialect(OracleDialect.class)
	@RequiresDialect(PostgreSQLDialect.class)
	public void test_sql_hibernate_entity_associations_query_one_to_many_join_example_2() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			//tag::sql-hibernate-entity-associations-query-one-to-many-join-example[]
			List<Phone> tuples = session.createNativeQuery(
				"SELECT {ph.*}, {c.*} " +
				"FROM Phone ph " +
				"JOIN phone_call c ON c.phone_id = ph.id", Phone.class, "ph")
			.addJoin("c", "ph.calls")
			.list();

			for (Phone phone : tuples) {
				List<Call> calls = phone.getCalls();
			}
			//end::sql-hibernate-entity-associations-query-one-to-many-join-example[]
			assertEquals(2, tuples.size());
		});
	}

	@Test
	public void test_sql_jpa_multi_entity_query_example() {
		try {
			doInJPA(this::entityManagerFactory, entityManager -> {
				//tag::sql-jpa-multi-entity-query-example[]
				List<Person> entities = entityManager.createNativeQuery(
					"SELECT * " +
					"FROM Person pr, Partner pt " +
					"WHERE pr.name = pt.name", Person.class)
				.getResultList();
				//end::sql-jpa-multi-entity-query-example[]
				assertEquals(2, entities.size());
			});
			fail("Should throw NonUniqueDiscoveredSqlAliasException!");
		}
		catch (PersistenceException expected) {
			assertEquals(NonUniqueDiscoveredSqlAliasException.class, expected.getClass());
		}
	}

	@Test
	public void test_sql_hibernate_multi_entity_query_example() {
		try {
			doInJPA(this::entityManagerFactory, entityManager -> {
				Session session = entityManager.unwrap(Session.class);
				//tag::sql-hibernate-multi-entity-query-example[]
				List<Person> entities = session.createNativeQuery(
					"SELECT * " +
					"FROM Person pr, Partner pt " +
					"WHERE pr.name = pt.name", Person.class)
				.list();
				//end::sql-hibernate-multi-entity-query-example[]
				assertEquals(2, entities.size());
			});
			fail("Should throw NonUniqueDiscoveredSqlAliasException!");
		}
		catch (NonUniqueDiscoveredSqlAliasException e) {
			// expected
		}
		catch (PersistenceException e) {
			assertTyping(NonUniqueDiscoveredSqlAliasException.class, e.getCause());
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-15914" )
	public void test_sql_hibernate_multi_entity_query_alias_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			//tag::sql-hibernate-multi-entity-query-alias-example[]
			List<Object> entities = session.createNativeQuery(
				"SELECT {pr.*}, {pt.*} " +
				"FROM Person pr, Partner pt " +
				"WHERE pr.name = pt.name", Object.class)
			.addEntity("pr", Person.class)
			.addEntity("pt", Partner.class)
			.list();
			//end::sql-hibernate-multi-entity-query-alias-example[]
			assertEquals(1, entities.size());
		});
	}

	@Test
	public void test_sql_hibernate_dto_query_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			//tag::sql-hibernate-dto-query-example[]
			List<PersonSummaryDTO> dtos = session.createNativeQuery(
				"SELECT p.id as \"id\", p.name as \"name\" " +
				"FROM Person p", Tuple.class)
			.setTupleTransformer(
					(tuple, aliases) -> {
						PersonSummaryDTO dto = new PersonSummaryDTO();
						dto.setId( (Long)tuple[0] );
						dto.setName( (String)tuple[1] );
						return dto;
					}
			)
			.list();
			//end::sql-hibernate-dto-query-example[]
			assertEquals(3, dtos.size());
		});
	}

	@Test
	public void test_sql_hibernate_inheritance_query_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			//tag::sql-hibernate-inheritance-query-example[]
			List<CreditCardPayment> payments = session.createNativeQuery(
				"SELECT * " +
				"FROM Payment p " +
				"JOIN CreditCardPayment cp on cp.id = p.id", CreditCardPayment.class)
			.list();
			//end::sql-hibernate-inheritance-query-example[]
			assertEquals(1, payments.size());
		});
	}

	@Test
	public void test_sql_jpa_query_parameters_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::sql-jpa-query-parameters-example[]
			List<Person> persons = entityManager.createNativeQuery(
				"SELECT * " +
				"FROM Person " +
				"WHERE name like :name", Person.class)
			.setParameter("name", "J%")
			.getResultList();
			//end::sql-jpa-query-parameters-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	public void test_sql_hibernate_query_parameters_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			//tag::sql-hibernate-query-parameters-example[]
			List<Person> persons = session.createNativeQuery(
				"SELECT * " +
				"FROM Person " +
				"WHERE name like :name", Person.class)
			.setParameter("name", "J%")
			.list();
			//end::sql-hibernate-query-parameters-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	public void test_sql_jpa_scalar_named_query_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::sql-jpa-scalar-named-query-example[]
			List<String> names = entityManager.createNamedQuery(
				"find_person_name", String.class)
			.getResultList();
			//end::sql-jpa-scalar-named-query-example[]
			assertEquals(3, names.size());
		});
	}

	@Test
	public void test_sql_hibernate_scalar_named_query_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			//tag::sql-hibernate-scalar-named-query-example[]
			List<String> names = session.createNamedQuery(
				"find_person_name", String.class)
			.list();
			//end::sql-hibernate-scalar-named-query-example[]
			assertEquals(3, names.size());
		});
	}

	@Test
	public void test_sql_jpa_multiple_scalar_values_named_query_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::sql-jpa-multiple-scalar-values-named-query-example[]
			List<Object[]> tuples = entityManager.createNamedQuery(
				"find_person_name_and_nickName", Object[].class)
			.getResultList();

			for(Object[] tuple : tuples) {
				String name = (String) tuple[0];
				String nickName = (String) tuple[1];
			}
			//end::sql-jpa-multiple-scalar-values-named-query-example[]
			assertEquals(3, tuples.size());
		});
	}

	@Test
	public void test_sql_hibernate_multiple_scalar_values_named_query_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			//tag::sql-hibernate-multiple-scalar-values-named-query-example[]
			List<Object[]> tuples = session.createNamedQuery(
				"find_person_name_and_nickName", Object[].class)
			.list();

			for(Object[] tuple : tuples) {
				String name = (String) tuple[0];
				String nickName = (String) tuple[1];
			}
			//end::sql-hibernate-multiple-scalar-values-named-query-example[]
			assertEquals(3, tuples.size());
		});
	}

	@Test
	public void test_sql_jpa_multiple_scalar_values_dto_named_query_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::sql-jpa-multiple-scalar-values-dto-named-query-example[]
			List<PersonNames> personNames = entityManager.createNamedQuery(
				"find_person_name_and_nickName_dto", PersonNames.class)
			.getResultList();
			//end::sql-jpa-multiple-scalar-values-dto-named-query-example[]
			assertEquals(3, personNames.size());
			assertThat( personNames.get( 0) ).isNotNull();
			assertThat( personNames.get( 0) ).isInstanceOf( PersonNames.class);
		});
	}

	@Test
	public void test_sql_hibernate_multiple_scalar_values_dto_named_query_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			//tag::sql-hibernate-multiple-scalar-values-dto-named-query-example[]
			List<PersonNames> personNames = session.createNamedQuery(
				"find_person_name_and_nickName_dto", PersonNames.class)
			.list();
			//end::sql-hibernate-multiple-scalar-values-dto-named-query-example[]
			assertEquals(3, personNames.size());
		});
	}

	@Test
	public void test_sql_hibernate_multiple_scalar_values_dto_hibernate_named_query_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			//tag::sql-hibernate-multiple-scalar-values-dto-hibernate-named-query-example[]
			List<PersonPhoneCount> personNames = session.createNamedQuery(
				"get_person_phone_count", PersonPhoneCount.class)
			.getResultList();
			//end::sql-hibernate-multiple-scalar-values-dto-hibernate-named-query-example[]
			assertEquals(2, personNames.size());
			assertEquals( 1, personNames.stream().filter( person -> person.getName().equals( "John Doe")).map( PersonPhoneCount::getPhoneCount).findAny().get().intValue());
			assertEquals( 2, personNames.stream().filter( person -> person.getName().equals( "Mrs. John Doe")).map( PersonPhoneCount::getPhoneCount).findAny().get().intValue());
		});
	}

	@Test
	public void test_sql_jpa_entity_named_query_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::sql-jpa-entity-named-query-example[]
			List<Person> persons = entityManager.createNamedQuery(
				"find_person_by_name", Person.class)
			.setParameter("name", "J%")
			.getResultList();
			//end::sql-jpa-entity-named-query-example[]
			assertEquals(1, persons.size());
			assertThat( persons ).hasSize( 1 );
			assertThat( persons.get( 0 ) ).isInstanceOf( Person.class );
		});
	}

	@Test
	public void test_sql_hibernate_entity_named_query_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			//tag::sql-hibernate-entity-named-query-example[]
			List<Person> persons = session.createNamedQuery(
				"find_person_by_name", Person.class)
			.setParameter("name", "J%")
			.list();
			//end::sql-hibernate-entity-named-query-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	@RequiresDialect(OracleDialect.class)
	@RequiresDialect(PostgreSQLDialect.class)
	public void test_sql_jpa_entity_associations_named_query_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::sql-jpa-entity-associations_named-query-example[]
			List<Object[]> tuples = entityManager.createNamedQuery(
				"find_person_with_phones_by_name", Object[].class)
			.setParameter("name", "J%")
			.getResultList();

			for(Object[] tuple : tuples) {
				Person person = (Person) tuple[0];
				Phone phone = (Phone) tuple[1];
			}
			//end::sql-jpa-entity-associations_named-query-example[]
			assertEquals(1, tuples.size());
			assertThat( tuples.get(0).getClass().isArray() ).isTrue();
		});
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	@RequiresDialect(OracleDialect.class)
	@RequiresDialect(PostgreSQLDialect.class)
	public void test_sql_hibernate_entity_associations_named_query_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			//tag::sql-hibernate-entity-associations_named-query-example[]
			List<Object[]> tuples = session.createNamedQuery(
				"find_person_with_phones_by_name", Object[].class)
			.setParameter("name", "J%")
			.list();

			for(Object[] tuple : tuples) {
				Person person = (Person) tuple[0];
				Phone phone = (Phone) tuple[1];
			}
			//end::sql-hibernate-entity-associations_named-query-example[]
			assertEquals(1, tuples.size());
		});
	}

	@Test
	public void test_sql_jpa_composite_key_entity_associations_named_query_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::sql-jpa-composite-key-entity-associations_named-query-example[]
			List<Object[]> tuples = entityManager.createNamedQuery(
				"find_all_spaceships", Object[].class)
			.getResultList();

			for(Object[] tuple : tuples) {
				SpaceShip spaceShip = (SpaceShip) tuple[0];
				Number surface = (Number) tuple[1];
				Number volume = (Number) tuple[2];
			}
			//end::sql-jpa-composite-key-entity-associations_named-query-example[]
			assertEquals(1, tuples.size());
			SpaceShip spaceShip = (SpaceShip) tuples.get(0)[0];
			assertNotNull(spaceShip.getCaptain());
			assertTrue(spaceShip.getCaptain() instanceof HibernateProxy);
			assertTrue(((HibernateProxy) spaceShip.getCaptain()).getHibernateLazyInitializer().isUninitialized());
		});
	}

	@Test
	public void test_sql_hibernate_composite_key_entity_associations_named_query_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			//tag::sql-hibernate-composite-key-entity-associations_named-query-example[]
			List<Object[]> tuples = session.createNamedQuery(
				"find_all_spaceships", Object[].class)
			.list();

			for(Object[] tuple : tuples) {
				SpaceShip spaceShip = (SpaceShip) tuple[0];
				Number surface = (Number) tuple[1];
				Number volume = (Number) tuple[2];
			}
			//end::sql-hibernate-composite-key-entity-associations_named-query-example[]
			assertEquals(1, tuples.size());
		});
	}
}
