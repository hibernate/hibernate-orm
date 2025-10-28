/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance.tck2_2.joincolumn;

import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.transaction.TransactionUtil2.inTransaction;

/**
 * @author Steve Ebersole
 */
@RequiresDialect(H2Dialect.class )
@FailureExpected( jiraKey = "tck-challenge" )
@ServiceRegistry( settings = {@Setting(name = AvailableSettings.HBM2DDL_AUTO, value = "create-drop")} )
public class JoinColumnTest {

	@Test
	public void testIt(ServiceRegistryScope scope) {

		try (SessionFactoryImplementor sf = (SessionFactoryImplementor) new MetadataSources( scope.getRegistry() )
				.addAnnotatedClass( Company.class )
				.addAnnotatedClass( Location.class )
				.buildMetadata()
				.buildSessionFactory()) {
			try {
				inTransaction(
						sf,
						session -> {
							final Company acme = new Company( 1, "Acme Corp" );
							new Location( 1, "86-215", acme );
							new Location( 2, "20-759", acme );

							session.persist( acme );
						}
				);

				inTransaction(
						sf,
						session -> {
							final Company acme = session.find( Company.class, 1 );
							assert acme.getLocations().size() == 2;

							// this fails.  however it is due to a number of bad assumptions
							// in the TCK:

//								First the spec says:
//
//								{quote}
//								The relationship modeling annotation constrains the use of the cascade=REMOVE specification. The
//								cascade=REMOVE specification should only be applied to associations that are specified as One-
//								ToOne or OneToMany. Applications that apply cascade=REMOVE to other associations are not por-
//								table.
//								{quote}
//
//								Here the test is applying cascade=REMOVE to a ManyToOne.
//
//								Secondly, the spec says:
//
//								{quote}
//								The persistence provider runtime is permitted to perform synchronization to the database at other times
//								as well when a transaction is active and the persistence context is joined to the transaction.
//								{quote}
//
//
//								In other words, the provider is actually legal to perform the database delete immediately.  Since
//								the TCK deletes the Company first, a provider is legally able to perform the database delete on
//								Company immediately.  However the TCK test as defined makes this impossible:
//									1) simply deleting Company won't work since Location rows still reference it.  Locations
//										would need to be deleted first (cascade the remove to the @OneToMany `locations`
//										attribute), or
//									2) perform a SQL update, updating the COMP_ID columns in the Location table to be null
//										so that deleting Company wont cause FK violations.  But again this is made impossible
//										by the TCK because it defines the column as non-nullable (and not even in the @JoinColumn
// 										btw which would be another basis for challenge).
							session.remove( acme );
							for ( Location location : acme.getLocations() ) {
								session.remove( location );
							}
						}
				);
			}
			finally {
				inTransaction(
						sf,
						session -> {
							session.createQuery( "delete Location" ).executeUpdate();
							session.createQuery( "delete Company" ).executeUpdate();
						}
				);
			}
		}
	}

}
