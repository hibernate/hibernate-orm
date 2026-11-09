package org.hibernate.orm.test.hql;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Johannes Buehler
 */
@JiraKey( value = "HHH-10463")
@DomainModel(annotatedClasses = org.hibernate.testing.orm.domain.gambit.EntityOfBasics.class)
@SessionFactory
// Retain the legacy temporal semantics of this regression fixture.
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "false"))
public class CoalesceTest {
	final String QRY_STR = "from EntityOfBasics e where e.theString = coalesce(:p , e.theString)";

	@Test
	public void testCoalesce(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final List<EntityOfBasics> resultList = session.createQuery( QRY_STR, EntityOfBasics.class )
					.setParameter("p", "a string")
					.getResultList();
			assertThat( resultList ).hasSize( 1 );
		} );

		sessions.inTransaction( (session) -> {
			final List<EntityOfBasics> resultList = session.createQuery( QRY_STR, EntityOfBasics.class )
					.setParameter("p", "$^&@#")
					.getResultList();
			assertThat( resultList ).hasSize( 0 );
		} );
	}

	@Test
	public void testCoalesceWithNull(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final List<EntityOfBasics> resultList = session.createQuery( QRY_STR, EntityOfBasics.class )
					.setParameter("p", null)
					.getResultList();
			assertThat( resultList ).hasSize( 1 );
		} );
	}

	@Test
	public void testCoalesceWithCast(SessionFactoryScope sessions) {
		final String qryStr = "from EntityOfBasics e where e.theString = coalesce(cast(:p as string) , e.theString)";
		sessions.inTransaction( (session) -> {
			final List<EntityOfBasics> resultList = session.createQuery( qryStr, EntityOfBasics.class )
					.setParameter("p", null)
					.getResultList();
			assertThat( resultList ).hasSize( 1 );
		} );
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final EntityOfBasics entity = new EntityOfBasics( 1 );
			entity.setTheString( "a string" );
			entity.setTheField( "another string" );
			session.persist( entity );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope sessions) {
		sessions.dropData();
	}

}
