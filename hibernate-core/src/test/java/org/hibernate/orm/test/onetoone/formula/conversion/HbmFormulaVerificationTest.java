package org.hibernate.orm.test.onetoone.formula.conversion;

import org.hibernate.orm.test.onetoone.formula.Person;
import org.hibernate.orm.test.onetoone.formula.Address;
import org.hibernate.orm.test.onetoone.formula.OneToOneFormulaTest;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Establishes HBM behavior for missing addresses and cascade ordering.
///
/// @author Steve Ebersole
public class HbmFormulaVerificationTest extends OneToOneFormulaTest {
	private final SQLStatementInspector inspector = new SQLStatementInspector();

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builder) {
		super.applySettings(builder);
		builder.applySetting(Environment.STATEMENT_INSPECTOR, inspector);
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
		assertEquals( java.util.List.of( "Person", "Address" ), mutationTables( "insert into " ) );
		inTransaction( session -> {
			assertEquals( 2L, session.createQuery( "select count(*) from Person", Long.class ).getSingleResult() );
			assertEquals( 2L, session.createQuery( "select count(*) from Address", Long.class ).getSingleResult() );
		} );
		inspector.clear();
		inTransaction( session -> session.remove( p ) );
		assertEquals( java.util.List.of( "Address", "Person" ), mutationTables( "delete from " ) );
		inTransaction( session -> {
			assertEquals( 1L, session.createQuery( "select count(*) from Person", Long.class ).getSingleResult() );
			assertEquals( 1L, session.createQuery( "select count(*) from Address", Long.class ).getSingleResult() );
		} );
	}

	private java.util.List<String> mutationTables(String prefix) {
		return inspector.getSqlQueries().stream()
				.filter( sql -> sql.startsWith( prefix ) )
				.map( sql -> sql.substring( prefix.length() ).split( "[ (]", 2 )[0] )
				.toList();
	}
}
