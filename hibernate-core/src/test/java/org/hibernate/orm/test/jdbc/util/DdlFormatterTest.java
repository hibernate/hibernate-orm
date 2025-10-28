/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jdbc.util;

import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import java.util.StringTokenizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Vlad Mihalcea
 */
@BaseUnitTest
public class DdlFormatterTest {
	private static final Logger log = Logger.getLogger( DdlFormatterTest.class );

	@Test
	public void testNoLoss() {
		assertNoLoss( "drop table post if exists" );
		assertNoLoss( "drop table post_comment if exists" );
		assertNoLoss( "drop table post_details if exists" );
		assertNoLoss( "drop table post_tag if exists" );
		assertNoLoss( "drop table tag if exists" );
		assertNoLoss( "create table post (id bigint not null, title varchar(255), primary key (id))" );
		assertNoLoss( "create table post_comment (id bigint not null, review varchar(255), post_id bigint, primary key (id))" );
		assertNoLoss( "create table post_details (id bigint not null, created_by varchar(255), created_on timestamp, primary key (id))" );
		assertNoLoss( "create table post_tag (post_id bigint not null, tag_id bigint not null)" );
		assertNoLoss( "create table tag (id bigint not null, name varchar(255), primary key (id))" );
		assertNoLoss( "alter table post_comment add constraint FKna4y825fdc5hw8aow65ijexm0 foreign key (post_id) references post" );
		assertNoLoss( "alter table post_details add constraint FKkl5eik513p1xiudk2kxb0v92u foreign key (id) references post" );
		assertNoLoss( "alter table post_tag add constraint FKac1wdchd2pnur3fl225obmlg0 foreign key (tag_id) references tag" );
		assertNoLoss( "alter table post_tag add constraint FKc2auetuvsec0k566l0eyvr9cs foreign key (post_id) references post" );
	}

	private void assertNoLoss(String query) {
		String formattedQuery = FormatStyle.DDL.getFormatter().format( query );
		StringTokenizer formatted = new StringTokenizer( formattedQuery, " \t\n\r\f()" );
		StringTokenizer plain = new StringTokenizer( query, " \t\n\r\f()" );

		log.debugf( "Original: {}", query );
		log.debugf( "Formatted: {}", formattedQuery );

		while ( formatted.hasMoreTokens() && plain.hasMoreTokens() ) {
			String plainToken = plain.nextToken();
			String formattedToken = formatted.nextToken();
			assertEquals( plainToken, formattedToken, "formatter did not return the same token" );
		}
		assertFalse( formatted.hasMoreTokens() );
		assertFalse( plain.hasMoreTokens() );
	}
}
