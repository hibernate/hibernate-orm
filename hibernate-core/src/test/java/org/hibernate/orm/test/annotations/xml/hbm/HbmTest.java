/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.xml.hbm;

import java.util.HashSet;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;


/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				PrimeMinister.class,
				Sky.class,
		},
		xmlMappings = {
				"org/hibernate/orm/test/annotations/xml/hbm/Government.hbm.xml",
				"org/hibernate/orm/test/annotations/xml/hbm/CloudType.hbm.xml",
		}
)
@SessionFactory
public class HbmTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from Government" ).list().forEach( gov -> {
						session.remove( gov );

					} );
					session.createQuery( "from PrimeMinister" ).list().forEach( p -> {
						session.remove( p );
					} );
					session.createMutationQuery( "delete from EarthSky" ).executeUpdate();
					session.createMutationQuery( "delete from CloudType" ).executeUpdate();
				}
		);
	}

	@Test
	public void testManyToOne(SessionFactoryScope scope) throws Exception {
		scope.inTransaction(
				session -> {
					Government gov = new Government();
					gov.setName( "Liberals" );
					session.persist( gov );
					PrimeMinister pm = new PrimeMinister();
					pm.setName( "Murray" );
					pm.setCurrentGovernment( gov );
					session.persist( pm );
				}
		);
	}

	@Test
	public void testOneToMany(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Government gov = new Government();
					gov.setName( "Liberals" );
					Government gov2 = new Government();
					gov2.setName( "Liberals2" );
					session.persist( gov );
					session.persist( gov2 );
					PrimeMinister pm = new PrimeMinister();
					pm.setName( "Murray" );
					pm.setCurrentGovernment( gov );
					pm.setGovernments( new HashSet() );
					pm.getGovernments().add( gov2 );
					pm.getGovernments().add( gov );
					gov.setPrimeMinister( pm );
					gov2.setPrimeMinister( pm );
					session.persist( pm );
					session.flush();
				}
		);
	}

	@Test
	public void testManyToMany(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					CloudType type = new CloudType();
					type.setName( "Cumulus" );
					Sky sky = new Sky();
					session.persist( type );
					sky.getCloudTypes().add( type );
					session.persist( sky );
					session.flush();
				}
		);
	}
}
