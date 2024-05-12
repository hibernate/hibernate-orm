package org.hibernate.orm.test.bytecode.enhancement.lazy.group;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hibernate.stat.SessionStatistics;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * Testing OneToOne LazyToOne association
 *
 * @author Jan-Oliver Lustig, Sebastian Viefhaus
 */
@JiraKey("HHH-11986")
@DomainModel(
		annotatedClasses = {
				LGMB_From.class, LGMB_To.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class LazyGroupMappedByTest {

	@Test
	@JiraKey("HHH-11986")
	public void test(SessionFactoryScope scope) {
		Long fromId = createEntities( scope );

		Statistics stats = scope.getSessionFactory().getStatistics();
		stats.setStatisticsEnabled( true );
		stats.clear();

		scope.inTransaction( session -> {
					SessionStatistics sessionStats = session.getStatistics();

					// Should be loaded lazy.
					LGMB_From from = session.get( LGMB_From.class, fromId );
					assertEquals( 1, sessionStats.getEntityCount() );
					assertEquals( 1, stats.getPrepareStatementCount() );

					// Lazy text is accessed, toRelation should not be read yet.
					String bigText = from.getBigText();
					assertEquals( 1, sessionStats.getEntityCount() );
					assertEquals( 2, stats.getPrepareStatementCount() );

					// Second table is accessed and the lazy one should be reloaded.
					LGMB_To to = from.getToRelation();
					assertEquals( 2, sessionStats.getEntityCount() );
					assertEquals( 3, stats.getPrepareStatementCount() );

					to.getFromRelation().getName();
					assertEquals( 3, stats.getPrepareStatementCount() );
				}
		);
	}

	/**
	 * Hilfsmethode: Eine Entität anlegen
	 *
	 * @return ID der Quell-Entität
	 */
	public Long createEntities(SessionFactoryScope scope) {
		return scope.fromTransaction( session -> {
					session.createQuery( "delete from LGMB_To" ).executeUpdate();
					session.createQuery( "delete from LGMB_From" ).executeUpdate();

					LGMB_From from = new LGMB_From( "A" );
					LGMB_To to = new LGMB_To( "B" );
					from.setToRelation( to );
					to.setFromRelation( from );

					session.save( from );
					session.flush();

					return from.getId();
				}
		);
	}

}
