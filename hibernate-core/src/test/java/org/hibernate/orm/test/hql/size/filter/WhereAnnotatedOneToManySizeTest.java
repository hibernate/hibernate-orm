/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql.size.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.community.dialect.TiDBDialect;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@JiraKey(value = "HHH-14585")
@DomainModel(annotatedClasses = { Region.class, City.class })
@SessionFactory
public class WhereAnnotatedOneToManySizeTest {


	@BeforeEach
	public void createTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			Region lazio = new Region();
			lazio.setId( 1 );
			lazio.setName( "Lazio" );

			Region lombardy = new Region();
			lombardy.setId( 2 );
			lombardy.setName( "Lombardy" );

			City rome = new City();
			rome.setId( 1 );
			rome.setName( "Rome" );
			rome.setDeleted( false );
			rome.setRegion( lazio );

			City gradoli = new City();
			gradoli.setId( 2 );
			gradoli.setName( "Gradoli" );
			gradoli.setDeleted( true );
			gradoli.setRegion( lazio );

			City milan = new City();
			milan.setId( 3 );
			milan.setName( "Milan" );
			milan.setDeleted( false );
			milan.setRegion( lombardy );

			City pavia = new City();
			pavia.setId( 4 );
			pavia.setName( "Pavia" );
			pavia.setDeleted( false );
			pavia.setRegion( lombardy );

			lazio.getCities().add( rome );
			lazio.getCities().add( gradoli );

			lombardy.getCities().add( milan );
			lombardy.getCities().add( pavia );

			session.persist( lazio );
			session.persist( lombardy );

			session.persist( rome );
			session.persist( gradoli );
			session.persist( milan );
			session.persist( pavia );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@SkipForDialect(dialectClass = DB2Dialect.class,
			reason = "DB2 does not support correlated subqueries in the ORDER BY clause")
	@SkipForDialect(dialectClass = HANADialect.class,
			reason = "HANA db does not support correlated subqueries in the ORDER BY clause")
	@SkipForDialect(dialectClass = TiDBDialect.class,
			reason = "TiDB db does not support correlated subqueries in the ORDER BY clause")
	@SkipForDialect(dialectClass = SybaseDialect.class,
			matchSubTypes = true,
			reason = "Sybase db does not support subqueries in the ORDER BY clause")
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "Informix does not support correlated subqueries in the ORDER BY clause")
	public void orderBy_sizeOf(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			Query<Object[]> query = session.createQuery(
					"select r, size(r.cities) from Region r order by size(r.cities) desc" );
			List<Object[]> result = query.getResultList();
			assertThat( result ).extracting( f -> f[0] ).extracting( "name" ).containsExactly( "Lombardy", "Lazio" );
			assertThat( result ).extracting( f -> f[1] ).containsExactly( 2, 1 );
		} );
	}

	@Test
	public void project_sizeOf(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			Query<Integer> query = session.createQuery(
					"SELECT size(r.cities) FROM Region r", Integer.class );
			List<Integer> cityCounts = query.getResultList();
			assertThat( cityCounts ).containsExactlyInAnyOrder( 1, 2 );
		} );
	}

}
