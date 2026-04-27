/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.orphan.one2one.fk.reversed.bidirectional.multilevelcascade;

import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = {
				Preisregelung.class,
				Tranche.class,
				Tranchenmodell.class
		}
)
@SessionFactory
public class DeleteMultiLevelOrphansTest {

	@BeforeEach
	public void createData(SessionFactoryScope scope) {
		Preisregelung preisregelung = new Preisregelung();
		preisregelung.setId( 17960L );

		Tranchenmodell tranchenmodell = new Tranchenmodell();
		tranchenmodell.setId( 1951L );

		Tranche tranche1 = new Tranche();
		tranche1.setId( 1951L );

		Tranche tranche2 = new Tranche();
		tranche2.setId( 1952L );

		preisregelung.setTranchenmodell( tranchenmodell );
		tranchenmodell.setPreisregelung( preisregelung );

		tranchenmodell.getTranchen().add( tranche1 );
		tranche1.setTranchenmodell( tranchenmodell );
		tranchenmodell.getTranchen().add( tranche2 );
		tranche2.setTranchenmodell( tranchenmodell );

		scope.inTransaction(
				session ->
						session.persist( preisregelung )
		);
	}

	@AfterEach
	public void cleanupData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-9091")
	public void testOrphanedWhileManaged(SessionFactoryScope scope) {

		Preisregelung p = scope.fromTransaction(
				session -> {
					assertEquals( 1, session.createQuery( "from Tranchenmodell", Tranchenmodell.class ).list().size() );
					List<Preisregelung> preisregelungen = session.createQuery( "from Preisregelung", Preisregelung.class ).list();
					assertEquals( 1, preisregelungen.size() );
					Preisregelung preisregelung = preisregelungen.get( 0 );
					assertNotNull( preisregelung.getTranchenmodell() );
					preisregelung.setTranchenmodell( null );
					return preisregelung;
				}
		);

		scope.inTransaction(
				session -> {
					Preisregelung preisregelung = session.get( Preisregelung.class, p.getId() );
					assertNull( preisregelung.getTranchenmodell() );
					assertEquals( 0, session.createQuery( "from Tranchenmodell", Tranchenmodell.class ).list().size() );
					assertEquals( 1, session.createQuery( "from Preisregelung", Preisregelung.class ).list().size() );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-9091")
	public void testReplacedWhileManaged(SessionFactoryScope scope) {

		Tranchenmodell t = scope.fromTransaction(
				session -> {
					assertEquals( 1, session.createQuery( "from Tranchenmodell", Tranchenmodell.class ).list().size() );
					List<Preisregelung> preisregelungen = session.createQuery( "from Preisregelung", Preisregelung.class ).list();
					assertEquals( 1, preisregelungen.size() );
					Preisregelung preisregelung = preisregelungen.get( 0 );
					assertNotNull( preisregelung.getTranchenmodell() );

					// Replace with a new Tranchenmodell instance
					Tranchenmodell tranchenmodellNew = new Tranchenmodell();
					tranchenmodellNew.setId( 1952L );
					preisregelung.setTranchenmodell( tranchenmodellNew );
					tranchenmodellNew.setPreisregelung( preisregelung );
					return tranchenmodellNew;
				}
		);

		scope.inTransaction(
				session -> {
					List<Tranchenmodell> tranchenmodelle = session.createQuery( "from Tranchenmodell", Tranchenmodell.class ).list();
					assertEquals( 1, tranchenmodelle.size() );
					Tranchenmodell tranchenmodellQueried = tranchenmodelle.get( 0 );
					assertEquals( t.getId(), tranchenmodellQueried.getId() );
					List<Preisregelung> preisregelungen = session.createQuery( "from Preisregelung", Preisregelung.class ).list();
					assertEquals( 1, preisregelungen.size() );
					Preisregelung preisregelung1Queried = preisregelungen.get( 0 );
					assertEquals( preisregelung1Queried.getTranchenmodell(), tranchenmodellQueried );
					assertEquals( 0, session.createQuery( "from Tranche", Tranche.class ).list().size() );
				}
		);
	}

}
