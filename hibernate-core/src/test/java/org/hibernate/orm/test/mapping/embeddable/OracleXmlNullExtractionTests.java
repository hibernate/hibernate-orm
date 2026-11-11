package org.hibernate.orm.test.mapping.embeddable;

import java.util.List;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.type.SqlTypes.SQLXML;

/// Non-temporal nested XML proves that null semantics are independent of preservation.
/// Explicitly present all-null members retain their existing projection semantics.
///
/// @author Steve Ebersole
@RequiresDialect(OracleDialect.class)
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "true"))
@DomainModel(annotatedClasses = OracleXmlNullExtractionTests.Row.class)
@SessionFactory
class OracleXmlNullExtractionTests {
	boolean preservationEnabled() { return true; }

	@Test
	void nestedNullsAndPredicates(SessionFactoryScope scope) {
		assertThat( scope.getSessionFactory().getSessionFactoryOptions().isTemporalPreservationEnabled() )
				.isEqualTo( preservationEnabled() );
		try {
			scope.inTransaction( session -> {
				for ( int id = 1; id <= 7; id++ ) {
					final var row = new Row(); row.id = id;
					if ( id != 3 ) row.payload = new Payload();
					if ( id == 1 || id == 2 || id == 4 ) row.payload.middle = new Middle();
					if ( id == 1 || id == 4 ) row.payload.middle.leaf = new Leaf();
					if ( id == 1 ) row.payload.middle.leaf.label = "value";
					session.persist( row );
				}
			});
			// Independent raw representations: absent middle, empty middle, absent leaf.
			scope.inTransaction( session -> session.doWork( connection -> {
				try ( var statement = connection.createStatement() ) {
					statement.executeUpdate( "update XmlNullRow set payload=xmltype('<e><marker>present</marker></e>') where id=5" );
					statement.executeUpdate( "update XmlNullRow set payload=xmltype('<e><middle/></e>') where id=6" );
					statement.executeUpdate( "update XmlNullRow set payload=xmltype('<e><middle><marker>present</marker></middle></e>') where id=7" );
				}
			}));
			assertState( scope, false );
			scope.inTransaction( session -> assertThat( session.createMutationQuery(
					"update XmlNullRow e set e.payload.middle.leaf=null where id=1" ).executeUpdate() ).isEqualTo( 1 ) );
			assertState( scope, true );
		}
		finally { scope.dropData(); }
	}

	private static void assertState(SessionFactoryScope scope, boolean cleared) {
		final var nullIds = cleared ? List.of( 1, 2, 3, 5, 6, 7 ) : List.of( 2, 3, 5, 6, 7 );
		final var nonNullIds = cleared ? List.of( 4 ) : List.of( 1, 4 );
		scope.inTransaction( session -> {
			for ( int id : nullIds ) {
				final var row = session.find( Row.class, id );
				assertThat( row.payload == null || row.payload.middle == null || row.payload.middle.leaf == null ).isTrue();
			}
			if ( !cleared ) assertThat( session.find( Row.class, 1 ).payload.middle.leaf.label ).isEqualTo( "value" );
			// Characterize, do not broaden this repair to all-null embeddable assembly.
			final var empty = session.find( Row.class, 4 );
			assertThat( empty.payload.middle.leaf ).isNotNull();
			assertThat( empty.payload.middle.leaf.label ).isNull();
		});
		scope.inTransaction( session -> {
			for ( var tuple : session.createSelectionQuery(
					"select e.id,e.payload.middle.leaf from XmlNullRow e order by id", Object[].class ).getResultList() ) {
				if ( nullIds.contains( tuple[0] ) ) assertThat( tuple[1] ).isNull();
				else assertThat( ((Leaf) tuple[1]).label ).isEqualTo( tuple[0].equals( 4 ) ? null : "value" );
			}
		});
		scope.inTransaction( session -> {
			for ( var tuple : session.createSelectionQuery(
					"select e.id,e.payload.middle.leaf.label from XmlNullRow e order by id", Object[].class ).getResultList() ) {
				assertThat( tuple[1] ).isEqualTo( !cleared && tuple[0].equals( 1 ) ? "value" : null );
			}
		});
		scope.inTransaction( session -> {
			assertThat( session.createSelectionQuery( "select e.id from XmlNullRow e where e.payload.middle.leaf is null order by id", Integer.class )
					.getResultList() ).containsExactlyElementsOf( nullIds );
			assertThat( session.createSelectionQuery( "select e.id from XmlNullRow e where e.payload.middle.leaf is not null order by id", Integer.class )
					.getResultList() ).containsExactlyElementsOf( nonNullIds );
			assertThat( session.createSelectionQuery( "select e.id from XmlNullRow e where e.payload.middle is null order by id", Integer.class )
					.getResultList() ).containsExactly( 3, 5, 6 );
		});
	}

	@Entity(name = "XmlNullRow")
	static class Row { @Id int id; @JdbcTypeCode(SQLXML) Payload payload; }
	@Embeddable
	static class Payload { @JdbcTypeCode(SQLXML) Middle middle; String marker; }
	@Embeddable
	static class Middle { @JdbcTypeCode(SQLXML) Leaf leaf; String marker; }
	@Embeddable
	static class Leaf { String label; }
}
