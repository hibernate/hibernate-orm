package org.hibernate.orm.test.onetoone.formula.conversion;

import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Exercises the formula regression scenarios using read-only shared join columns.
///
/// @author Steve Ebersole
public class AnnotationFormulaTest extends BaseSessionFactoryFunctionalTest {
	private static class TextAsMaterializedClobType extends AbstractSingleColumnStandardBasicType<String> {
		public final static TextAsMaterializedClobType INSTANCE = new TextAsMaterializedClobType();

		public TextAsMaterializedClobType() {
			super( ClobJdbcType.DEFAULT, StringJavaType.INSTANCE );
		}

		public String getName() {
			return "text";
		}
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builder) {
		builder.applySetting( Environment.STATEMENT_INSPECTOR, inspector );
		builder.applySetting( Environment.USE_SECOND_LEVEL_CACHE, "false" );
		builder.applySetting( Environment.GENERATE_STATISTICS, "true" );
		builder.applySetting( Environment.DEFAULT_BATCH_FETCH_SIZE, "2" );
	}

	@Override
	protected void applyMetadataBuilder(MetadataBuilder metadataBuilder) {
		if ( OracleDialect.class.isInstance( getDialect() ) ) {
			metadataBuilder.applyBasicType( TextAsMaterializedClobType.INSTANCE );
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Person.class, Address.class };
	}

	private final SQLStatementInspector inspector =
			new SQLStatementInspector();


	private Person person;
	private Address address;

	@BeforeEach
	public void setUp() {
		inspector.clear();
		person = new Person();
		person.setName( "Gavin King" );
		address = new Address();
		address.setPerson( person );
		address.setType( "HOME" );
		address.setZip( "3181" );
		address.setState( "VIC" );
		address.setStreet( "Karbarook Ave" );
		person.setAddress( address );

		inTransaction(
				session ->
						session.persist( person )
		);
		assertEquals( List.of( "Person", "Address" ), mutationTables( "insert into " ) );
	}

	@AfterEach
	protected void cleanupTest() {
		// Also clean auxiliary fixtures when a missing-address load fails.
		inTransaction( session -> {
			session.createMutationQuery( "delete from Address a where a.person.name <> :name" )
					.setParameter( "name", "Gavin King" ).executeUpdate();
			session.createMutationQuery( "delete from Person p where p.name <> :name" )
					.setParameter( "name", "Gavin King" ).executeUpdate();
		} );
		inspector.clear();
		if (person != null) {
			inTransaction( session -> session.remove( person ) );
		}
	}

	@Test
	public void testOneToOneFormula() {

		inTransaction( session -> {
			Person p = session.createQuery( "from Person", Person.class ).uniqueResult();

			assertNotNull( p.getAddress() );
			assertTrue( Hibernate.isInitialized( p.getAddress() ) );
			assertNull( p.getMailingAddress() );
		} );

		inTransaction( session -> {
			Person p = session.createQuery(
					"from Person p left join fetch p.mailingAddress left join fetch p.address", Person.class ).uniqueResult();

			assertNotNull( p.getAddress() );
			assertTrue( Hibernate.isInitialized( p.getAddress() ) );
			assertNull( p.getMailingAddress() );
		} );

		inTransaction( session -> {
			Person p = session.createQuery( "from Person p left join fetch p.address", Person.class ).uniqueResult();

			assertNotNull( p.getAddress() );
			assertTrue( Hibernate.isInitialized( p.getAddress() ) );
			assertNull( p.getMailingAddress() );
		} );

		inTransaction( session -> {
			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<Person> criteria = criteriaBuilder.createQuery( Person.class );
			Root<Person> root = criteria.from( Person.class );
			Join<Person, Object> address = root.join( "address", JoinType.INNER );
			criteria.where( criteriaBuilder.equal( address.get( "zip" ), "3181" ) );
			Person p = session.createQuery( criteria ).uniqueResult();

			assertNotNull( p );
		} );

		inTransaction( session -> {
			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<Person> criteria = criteriaBuilder.createQuery( Person.class );
			Root<Person> root = criteria.from( Person.class );
			root.fetch( "address", JoinType.LEFT );
			Person p = session.createQuery( criteria ).uniqueResult();

			assertNotNull( p.getAddress() );
			assertTrue( Hibernate.isInitialized( p.getAddress() ) );
			assertNull( p.getMailingAddress() );
		} );

		inTransaction( session -> {
			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<Person> criteria = criteriaBuilder.createQuery( Person.class );
			Root<Person> root = criteria.from( Person.class );
			root.fetch( "address", JoinType.LEFT );
			Person p = session.createQuery( criteria ).uniqueResult();

			assertNotNull( p.getAddress() );
			assertTrue( Hibernate.isInitialized( p.getAddress() ) );
			assertNull( p.getMailingAddress() );

		} );

		inTransaction( session -> {
			Address a = session.createQuery( "from Address", Address.class ).uniqueResult();

			Person person = a.getPerson();
			assertNotNull( person );
			assertTrue( Hibernate.isInitialized( person.getAddress() ) );
			assertTrue( Hibernate.isInitialized( person.getMailingAddress() ) );
			assertNull( person.getMailingAddress() );
		} );
	}


	@Test
	@JiraKey(value = "HHH-5757")
	public void testQuery() {
		inTransaction( session -> {
			Person p = session.createQuery( "from Person p where p.address = :address", Person.class ).setParameter(
					"address",
					address
			).uniqueResult();
			assertThat( p, notNullValue() );
		} );

		inTransaction( session -> {
			Address a = session.createQuery( "from Address a where a.person = :person", Address.class ).setParameter(
					"person",
					person
			).uniqueResult();
			assertThat( a, notNullValue() );

		} );
	}

	@Test
	public void testIdClassLookup() {
		inTransaction( session -> {
			Address a = session.getReference( Address.class, new AddressId( person.getName(), "HOME" ) );
			assertFalse( Hibernate.isInitialized( a ) );
			assertEquals( "3181", a.getZip() );
		} );
		inTransaction( session -> {
			Address a = session.get( Address.class, new AddressId( person.getName(), "HOME" ) );
			assertEquals( "3181", a.getZip() );
		} );
	}

	@Test
	public void testBothAddressesMissing() {
		inTransaction( session -> {
			Person empty = new Person();
			empty.setName( "No addresses" );
			session.persist( empty );
		} );
		inTransaction( session -> {
			Person empty = session.find( Person.class, "No addresses" );
			assertNull( empty.getAddress() );
			assertNull( empty.getMailingAddress() );
			session.remove( empty );
		} );
	}

	@Test
	public void testHomeMissingMailingPresent() {
		inTransaction( session -> {
			Person p = new Person();
			p.setName( "Mailing only" );
			session.persist( p );
			Address mailing = new Address();
			mailing.setPerson( p );
			mailing.setType( "MAILING" );
			session.persist( mailing );
		} );
		inTransaction( session -> {
			Person p = session.find( Person.class, "Mailing only" );
			assertNull( p.getAddress() );
			assertNotNull( p.getMailingAddress() );
			session.remove( p.getMailingAddress() );
			session.remove( p );
		} );
	}

	@Test
	public void testCascadeInsertAndDeleteWithoutLoading() {
		Person p = new Person();
		p.setName( "Cascade ordering" );
		Address a = new Address();
		a.setPerson( p );
		a.setType( "HOME" );
		p.setAddress( a );
		inspector.clear();
		inTransaction( session -> session.persist( p ) );
		assertEquals( List.of( "Person", "Address" ), mutationTables( "insert into " ) );
		inTransaction( session -> {
			assertEquals( 2L, session.createQuery( "select count(*) from Person", Long.class ).getSingleResult() );
			assertEquals( 2L, session.createQuery( "select count(*) from Address", Long.class ).getSingleResult() );
		} );
		inspector.clear();
		inTransaction( session -> session.remove( p ) );
		assertEquals( List.of( "Address", "Person" ), mutationTables( "delete from " ) );
		inTransaction( session -> {
			assertEquals( 1L, session.createQuery( "select count(*) from Person", Long.class ).getSingleResult() );
			assertEquals( 1L, session.createQuery( "select count(*) from Address", Long.class ).getSingleResult() );
		} );
	}

	@Test
	public void testSpecialOneToOneBootMapping() {
		var personBinding = getMetadata().getEntityBinding(Person.class.getName());
		for (String name : List.of("address", "mailingAddress")) {
			var value = personBinding.getProperty(name).getValue();
			assertTrue(value instanceof org.hibernate.mapping.OneToOne);
			var association = (org.hibernate.mapping.OneToOne) value;
			assertTrue(association.getType() instanceof org.hibernate.type.SpecialOneToOneType);
			assertEquals(org.hibernate.type.ForeignKeyDirection.TO_PARENT, association.getForeignKeyType());
			assertFalse(association.isConstrained());
			for (boolean writable : association.getColumnInsertability()) assertFalse(writable);
			for (boolean writable : association.getColumnUpdateability()) assertFalse(writable);
		}
		assertTrue(personBinding.getTable().getForeignKeys().isEmpty());
		assertTrue(personBinding.getTable().getUniqueKeys().isEmpty());
		assertFalse(getMetadata().getEntityBinding(Address.class.getName()).getTable().getForeignKeys().isEmpty());
	}

	@Test
	public void testCascadeDeleteManaged() {
		inTransaction(session -> {
			Person managed = session.find(Person.class, person.getName());
			inspector.clear();
			session.remove(managed);
		});
		assertEquals(List.of("Address", "Person"), mutationTables("delete from "));
		person = null;
		inTransaction(session -> {
			assertEquals(0L, session.createQuery("select count(*) from Person", Long.class).getSingleResult());
			assertEquals(0L, session.createQuery("select count(*) from Address", Long.class).getSingleResult());
		});
	}

	@Test
	public void testAssociationAssignmentDoesNotWriteSharedIdentifier() {
		inTransaction(session -> {
			Person managed = session.find(Person.class, person.getName());
			inspector.clear();
			managed.setAddress(null);
			session.flush();
			assertTrue(inspector.getSqlQueries().stream().noneMatch(sql -> sql.startsWith("update ")));
			session.clear();
			Person reloaded = session.find(Person.class, person.getName());
			assertEquals(person.getName(), reloaded.getName());
			assertNotNull(reloaded.getAddress());
		});
	}

	@Test
	public void testLeftFetchWhenBothAddressesMissing() {
		inTransaction(session -> {
			Person empty = new Person();
			empty.setName("Left fetch without addresses");
			session.persist(empty);
		});
		inTransaction(session -> {
			Person empty = session.createQuery(
					"from Person p left join fetch p.address left join fetch p.mailingAddress where p.name = :name",
					Person.class).setParameter("name", "Left fetch without addresses").getSingleResult();
			assertNull(empty.getAddress());
			assertNull(empty.getMailingAddress());
		});
		inTransaction(session -> {
			var builder = session.getCriteriaBuilder();
			var query = builder.createQuery(Person.class);
			var root = query.from(Person.class);
			root.fetch("address", JoinType.LEFT);
			root.fetch("mailingAddress", JoinType.LEFT);
			query.where(builder.equal(root.get("name"), "Left fetch without addresses"));
			Person empty = session.createQuery(query).getSingleResult();
			assertNull(empty.getAddress());
			assertNull(empty.getMailingAddress());
			session.remove(empty);
		});
	}

	private List<String> mutationTables(String prefix) {
		return inspector.getSqlQueries().stream()
				.filter( sql -> sql.startsWith( prefix ) )
				.map( sql -> sql.substring( prefix.length() ).split( "[ (]", 2 )[0] )
				.toList();
	}
}
