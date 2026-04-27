/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.orphan.one2one.fk.bidirectional.multilevelcascade;

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
				Tranchenmodell.class,
				X.class,
				Y.class
		}
)
@SessionFactory
public class DeleteMultiLevelOrphansTest {

	@BeforeEach
	public void createData(SessionFactoryScope scope) {
		Preisregelung preisregelung = new Preisregelung();

		Tranchenmodell tranchenmodell = new Tranchenmodell();

		X x = new X();

		Tranche tranche1 = new Tranche();

		Y y = new Y();

		Tranche tranche2 = new Tranche();

		preisregelung.setTranchenmodell( tranchenmodell );
		tranchenmodell.setPreisregelung( preisregelung );

		tranchenmodell.setX( x );
		x.setTranchenmodell( tranchenmodell );

		tranchenmodell.getTranchen().add( tranche1 );
		tranche1.setTranchenmodell( tranchenmodell );
		tranchenmodell.getTranchen().add( tranche2 );
		tranche2.setTranchenmodell( tranchenmodell );

		tranche1.setY( y );
		y.setTranche( tranche1 );

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
	public void testDirectAssociationOrphanedWhileManaged(SessionFactoryScope scope) {

		Preisregelung p = scope.fromTransaction(
				session -> {
					assertEquals( 1, session.createQuery( "from Tranchenmodell", Tranchenmodell.class ).list().size() );
					List<Preisregelung> preisregelungen = session.createQuery( "from Preisregelung", Preisregelung.class ).list();
					assertEquals( 1, preisregelungen.size() );
					Preisregelung preisregelung = preisregelungen.get( 0 );
					Tranchenmodell tranchenmodell = preisregelung.getTranchenmodell();
					assertNotNull( tranchenmodell );
					assertNotNull( tranchenmodell.getX() );
					assertEquals( 2, tranchenmodell.getTranchen().size() );
					assertNotNull( tranchenmodell.getTranchen().get( 0 ).getY() );
					preisregelung.setTranchenmodell( null );
					return preisregelung;
				}
		);


		scope.inTransaction(
				session -> {
					Preisregelung preisregelung = session.get( Preisregelung.class, p.getId() );
					assertNull( preisregelung.getTranchenmodell() );
					assertEquals( 0, session.createQuery( "from Tranchenmodell", Tranchenmodell.class ).list().size() );
					assertEquals( 0, session.createQuery( "from Tranche", Tranche.class ).list().size() );
					assertEquals( 0, session.createQuery( "from X", X.class ).list().size() );
					assertEquals( 0, session.createQuery( "from Y", Y.class ).list().size() );

					assertEquals( 1, session.createQuery( "from Preisregelung", Preisregelung.class ).list().size() );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-9091")
	public void testReplacedDirectAssociationWhileManaged(SessionFactoryScope scope) {

		// Create a new Tranchenmodell with new direct and nested associations
		Tranchenmodell tranchenmodellNew = new Tranchenmodell();
		X xNew = new X();
		tranchenmodellNew.setX( xNew );
		xNew.setTranchenmodell( tranchenmodellNew );
		Tranche trancheNew = new Tranche();
		tranchenmodellNew.getTranchen().add( trancheNew );
		trancheNew.setTranchenmodell( tranchenmodellNew );
		Y yNew = new Y();
		trancheNew.setY( yNew );
		yNew.setTranche( trancheNew );

		scope.inTransaction(
				session -> {
					assertEquals( 1, session.createQuery( "from Tranchenmodell", Tranchenmodell.class ).list().size() );
					List<Preisregelung> preisregelungen = session.createQuery( "from Preisregelung", Preisregelung.class ).list();
					assertEquals( 1, preisregelungen.size() );
					Preisregelung preisregelung = preisregelungen.get( 0 );
					Tranchenmodell tranchenmodell = preisregelung.getTranchenmodell();
					assertNotNull( tranchenmodell );
					assertNotNull( tranchenmodell.getX() );
					assertEquals( 2, tranchenmodell.getTranchen().size() );
					assertNotNull( tranchenmodell.getTranchen().get( 0 ).getY() );


					// Replace with a new Tranchenmodell instance containing new direct and nested associations
					preisregelung.setTranchenmodell( tranchenmodellNew );
					tranchenmodellNew.setPreisregelung( preisregelung );
				}
		);

		Tranchenmodell t = scope.fromTransaction(
				session -> {

					assertEquals( 1, session.createQuery( "from Tranche", Tranche.class ).list().size() );
					assertEquals( 1, session.createQuery( "from Tranchenmodell", Tranchenmodell.class ).list().size() );
					assertEquals( 1, session.createQuery( "from X", X.class ).list().size() );
					assertEquals( 1, session.createQuery( "from Y", Y.class ).list().size() );
					List<Preisregelung> preisregelungen = session.createQuery( "from Preisregelung", Preisregelung.class ).list();
					assertEquals( 1, preisregelungen.size() );
					Preisregelung preisregelung = preisregelungen.get( 0 );
					Tranchenmodell tranchenmodell = preisregelung.getTranchenmodell();
					assertNotNull( tranchenmodell );
					assertEquals( tranchenmodellNew.getId(), tranchenmodell.getId() );
					assertNotNull( tranchenmodell.getX() );
					assertEquals( xNew.getId(), tranchenmodell.getX().getId() );
					assertEquals( 1, tranchenmodell.getTranchen().size() );
					assertEquals( trancheNew.getId(), tranchenmodell.getTranchen().get( 0 ).getId() );
					assertEquals( yNew.getId(), tranchenmodell.getTranchen().get( 0 ).getY().getId() );

					// Replace with a new Tranchenmodell instance with no associations
					Tranchenmodell tr = new Tranchenmodell();
					preisregelung.setTranchenmodell( tr );
					tr.setPreisregelung( preisregelung );
					return tr;
				}
		);


		scope.inTransaction(
				session -> {
					List<Tranchenmodell> tranchenmodelle = session.createQuery( "from Tranchenmodell", Tranchenmodell.class ).list();
					assertEquals( 1, tranchenmodelle.size() );
					Tranchenmodell tranchenmodell = tranchenmodelle.get( 0 );
					assertEquals( t.getId(), tranchenmodell.getId() );
					List<Preisregelung> preisregelungen = session.createQuery( "from Preisregelung", Preisregelung.class ).list();
					assertEquals( 1, preisregelungen.size() );
					Preisregelung preisregelung = preisregelungen.get( 0 );
					assertEquals( tranchenmodell, preisregelung.getTranchenmodell() );
					assertEquals( 0, session.createQuery( "from Tranche", Tranche.class ).list().size() );
					assertEquals( 0, session.createQuery( "from X", X.class ).list().size() );
					assertEquals( 0, session.createQuery( "from Y", Y.class ).list().size() );
				}
		);

	}

	@Test
	@JiraKey(value = "HHH-9091")
	public void testDirectAndNestedAssociationsOrphanedWhileManaged(SessionFactoryScope scope) {

		Preisregelung p = scope.fromTransaction(
				session -> {
					assertEquals( 1, session.createQuery( "from Tranchenmodell", Tranchenmodell.class ).list().size() );
					List<Preisregelung> preisregelungen = session.createQuery( "from Preisregelung", Preisregelung.class ).list();
					assertEquals( 1, preisregelungen.size() );
					Preisregelung preisregelung = preisregelungen.get( 0 );
					Tranchenmodell tranchenmodell = preisregelung.getTranchenmodell();
					assertNotNull( tranchenmodell );
					assertNotNull( tranchenmodell.getX() );
					assertEquals( 2, tranchenmodell.getTranchen().size() );
					assertNotNull( tranchenmodell.getTranchen().get( 0 ).getY() );
					preisregelung.setTranchenmodell( null );
					tranchenmodell.setX( null );
					tranchenmodell.getTranchen().get( 0 ).setY( null );
					return preisregelung;
				}
		);

		scope.inTransaction(
				session -> {
					Preisregelung preisregelung = session.get(
							Preisregelung.class,
							p.getId()
					);
					assertNull( preisregelung.getTranchenmodell() );
					assertEquals( 0, session.createQuery( "from Tranchenmodell", Tranchenmodell.class ).list().size() );
					assertEquals( 0, session.createQuery( "from Tranche", Tranche.class ).list().size() );
					assertEquals( 0, session.createQuery( "from X", X.class ).list().size() );
					assertEquals( 0, session.createQuery( "from Y", Y.class ).list().size() );

					assertEquals( 1, session.createQuery( "from Preisregelung", Preisregelung.class ).list().size() );
				}
		);
	}
}
