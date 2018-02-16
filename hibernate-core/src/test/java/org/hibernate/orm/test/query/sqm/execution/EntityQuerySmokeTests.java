/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.execution;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;
import org.hibernate.orm.test.support.domains.gambit.EntityOfBasics;
import org.hibernate.orm.test.support.domains.retail.Vendor;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.orm.test.support.domains.retail.ModelClasses.applyRetailModel;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class EntityQuerySmokeTests extends SessionFactoryBasedFunctionalTest {
	@Override
	public SessionFactoryImplementor produceSessionFactory() {
		final SessionFactoryImplementor factory = super.produceSessionFactory();

		// currently a problem with EntityEntry -> PC
		sessionFactoryScope().inTransaction(
				factory,
				session -> session.doWork(
						connection -> {
							final Statement statement = connection.createStatement();
							try {
								statement.execute(
										"insert into EntityOfBasics( id, gender, theInt, ordinal_gender, converted_gender ) values ( 1, 'MALE', -1, 1, 'M' )"
								);
								statement.execute(
										"insert into Vendor( id, name ) values ( 1, 'Acme Corp' )"
								);
								statement.execute(
										"insert into Product( id, vendor, sku, currentSellPrice ) values ( 1, 1, 'CJS-HJWDI-1234', 2 )"
								);
							}
							finally {
								try {
									statement.close();
								}
								catch (SQLException ignore) {
								}
							}
						}
				)
		);

		return factory;
	}

	@Test
	public void testRootEntitySelection() {
		sessionFactoryScope().inSession(
				session -> {
					final List result = session.createQuery( "select e from EntityOfBasics e" ).list();
					assertThat( result, hasSize( 1 ) );
					final Object value = result.get( 0 );
					assertThat( value, notNullValue() );
					final EntityOfBasics entity = cast(
							value,
							EntityOfBasics.class
					);
					assertThat( entity.getId(), is( 1 ) );
					assertThat( entity.getGender(), is( EntityOfBasics.Gender.MALE ) );
					assertThat( entity.getTheInt(), is( -1 ) );
					assertThat( entity.getTheInteger(), nullValue() );
					assertThat( entity.getOrdinalGender(), is( EntityOfBasics.Gender.FEMALE ) );
					assertThat( entity.getConvertedGender(), is( EntityOfBasics.Gender.MALE ) );
				}
		);
	}

	@Test
	public void testRootEntityAttributeSelection() {
		sessionFactoryScope().inSession(
				session -> {
					final List result = session.createQuery( "select e.id from EntityOfBasics e" ).list();
					assertThat( result, hasSize( 1 ) );
					final Object value = result.get( 0 );
					assertThat( value, notNullValue() );
					Integer id = cast(
							value,
							Integer.class
					);
					assertThat( id, is( 1 ) );
				}
		);
	}

	@Test
	public void testRootEntityAttributeReference() {
		sessionFactoryScope().inSession(
				session -> {
					final List result = session.createQuery( "select e from EntityOfBasics e where id = 1" ).list();
					assertThat( result, hasSize( 1 ) );
					final Object value = result.get( 0 );
					assertThat( value, notNullValue() );
					final EntityOfBasics entity = cast(
							value,
							EntityOfBasics.class
					);
					assertThat( entity.getId(), is( 1 ) );
					assertThat( entity.getGender(), is( EntityOfBasics.Gender.MALE ) );
					assertThat( entity.getTheInt(), is( -1 ) );
					assertThat( entity.getTheInteger(), nullValue() );
					assertThat( entity.getOrdinalGender(), is( EntityOfBasics.Gender.FEMALE ) );
					assertThat( entity.getConvertedGender(), is( EntityOfBasics.Gender.MALE ) );
				}
		);
	}

	@Test
	public void testRootEntityManyToOneSelection() {
		sessionFactoryScope().inSession(
				session -> {
					final List result = session.createQuery( "select p.vendor from Product p" ).list();
					assertThat( result, hasSize( 1 ) );
					final Object value = result.get( 0 );
					assertThat( value, notNullValue() );
					final Vendor entity = cast(
							value,
							Vendor.class
					);
					assertThat( entity.getId(), is( 1 ) );
					assertThat( entity.getName(), is( "Acme Corp") );
				}
		);
	}

	@Test
	public void testRootEntityManyToOneAttributeReference() {
		sessionFactoryScope().inSession(
				session -> {
					final List result = session.createQuery( "select p.vendor from Product p" ).list();
					assertThat( result, hasSize( 1 ) );
					final Object value = result.get( 0 );
					assertThat( value, notNullValue() );
					final Vendor entity = cast(
							value,
							Vendor.class
					);
					assertThat( entity.getId(), is( 1 ) );
					assertThat( entity.getName(), is( "Acme Corp") );
				}
		);
	}

	@Test
	public void testJoinedSubclassRoot() {
		sessionFactoryScope().inSession(
				session -> session.createQuery( "select p from Payment p" ).list()
		);
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( EntityOfBasics.class );
		applyRetailModel( metadataSources );
	}
}
