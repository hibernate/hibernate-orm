/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pagination;

import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HHH-5715 bug test case: Duplicated entries when using select distinct with join and pagination. The bug has to do
 * with new {@link SQLServerDialect} that uses row_number function for pagination
 *
 * @author Valotasios Yoryos
 */
@JiraKey(value = "HHH-5715")
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/pagination/EntryTag.hbm.xml"
)
@SessionFactory
public class DistinctSelectTest {
	private static final int NUM_OF_USERS = 30;

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Tag> tags = new ArrayList<>();

					for ( int i = 0; i < 5; i++ ) {
						Tag tag = new Tag( "Tag: " + SafeRandomUUIDGenerator.safeRandomUUID() );
						tags.add( tag );
						session.persist( tag );
					}

					for ( int i = 0; i < NUM_OF_USERS; i++ ) {
						Entry e = new Entry( "Entry: " + SafeRandomUUIDGenerator.safeRandomUUID() );
						e.getTags().addAll( tags );
						session.persist( e );
					}
				}
		);
	}

	@Test
	public void testDistinctSelectWithJoin(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					List<Entry> entries = session.createQuery(
									"select distinct e from Entry e join e.tags t where t.surrogate is not null order by e.name",
									Entry.class )
							.setFirstResult( 10 ).setMaxResults( 5 ).list();

					// System.out.println(entries);
					Entry firstEntry = entries.remove( 0 );
					assertThat( entries )
							.describedAs(
									"The list of entries should not contain duplicated Entry objects as we've done a distinct select" )
							.doesNotContain( firstEntry );
				}
		);
	}
}
