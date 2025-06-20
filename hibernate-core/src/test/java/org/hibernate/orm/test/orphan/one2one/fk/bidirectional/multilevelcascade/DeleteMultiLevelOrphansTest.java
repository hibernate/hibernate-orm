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
					List results = session.createQuery( "from Tranchenmodell" ).list();
					assertEquals( 1, results.size() );
					results = session.createQuery( "from Preisregelung" ).list();
					assertEquals( 1, results.size() );
					Preisregelung preisregelung = (Preisregelung) results.get( 0 );
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
					List results = session.createQuery( "from Tranchenmodell" ).list();
					assertEquals( 0, results.size() );
					results = session.createQuery( "from Tranche" ).list();
					assertEquals( 0, results.size() );
					results = session.createQuery( "from X" ).list();
					assertEquals( 0, results.size() );
					results = session.createQuery( "from Y" ).list();
					assertEquals( 0, results.size() );

					results = session.createQuery( "from Preisregelung" ).list();
					assertEquals( 1, results.size() );
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
					List results = session.createQuery( "from Tranchenmodell" ).list();
					assertEquals( 1, results.size() );
					results = session.createQuery( "from Preisregelung" ).list();
					assertEquals( 1, results.size() );
					Preisregelung preisregelung = (Preisregelung) results.get( 0 );
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

					List results = session.createQuery( "from Tranche" ).list();
					assertEquals( 1, results.size() );
					results = session.createQuery( "from Tranchenmodell" ).list();
					assertEquals( 1, results.size() );
					results = session.createQuery( "from X" ).list();
					assertEquals( 1, results.size() );
					results = session.createQuery( "from Y" ).list();
					assertEquals( 1, results.size() );
					results = session.createQuery( "from Preisregelung" ).list();
					assertEquals( 1, results.size() );
					Preisregelung preisregelung = (Preisregelung) results.get( 0 );
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
					List results = session.createQuery( "from Tranchenmodell" ).list();
					assertEquals( 1, results.size() );
					Tranchenmodell tranchenmodell = (Tranchenmodell) results.get( 0 );
					assertEquals( t.getId(), tranchenmodell.getId() );
					results = session.createQuery( "from Preisregelung" ).list();
					assertEquals( 1, results.size() );
					Preisregelung preisregelung = (Preisregelung) results.get( 0 );
					assertEquals( tranchenmodell, preisregelung.getTranchenmodell() );
					results = session.createQuery( "from Tranche" ).list();
					assertEquals( 0, results.size() );
					results = session.createQuery( "from X" ).list();
					assertEquals( 0, results.size() );
					results = session.createQuery( "from Y" ).list();
					assertEquals( 0, results.size() );
				}
		);

	}

	@Test
	@JiraKey(value = "HHH-9091")
	public void testDirectAndNestedAssociationsOrphanedWhileManaged(SessionFactoryScope scope) {

		Preisregelung p = scope.fromTransaction(
				session -> {
					List results = session.createQuery( "from Tranchenmodell" ).list();
					assertEquals( 1, results.size() );
					results = session.createQuery( "from Preisregelung" ).list();
					assertEquals( 1, results.size() );
					Preisregelung preisregelung = (Preisregelung) results.get( 0 );
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
					Preisregelung preisregelung = (Preisregelung) session.get(
							Preisregelung.class,
							p.getId()
					);
					assertNull( preisregelung.getTranchenmodell() );
					List results = session.createQuery( "from Tranchenmodell" ).list();
					assertEquals( 0, results.size() );
					results = session.createQuery( "from Tranche" ).list();
					assertEquals( 0, results.size() );
					results = session.createQuery( "from X" ).list();
					assertEquals( 0, results.size() );
					results = session.createQuery( "from Y" ).list();
					assertEquals( 0, results.size() );

					results = session.createQuery( "from Preisregelung" ).list();
					assertEquals( 1, results.size() );
				}
		);
	}
}
