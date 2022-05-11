/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.resultmapping;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;

/**
 * @author Nathan Xu
 */
@Jpa(
		annotatedClasses = NamedNativeQueryWithCountColumnTest.Sample.class,
		properties = @Setting(name = AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, value = "true")
)
@TestForIssue(jiraKey = "HHH-15070")
class NamedNativeQueryWithCountColumnTest {

	@Test
	void testNoNullPointerExceptionThrown(EntityManagerFactoryScope scope) {
		scope.inTransaction( em ->em.createNamedQuery( "sample.count", Long.class ) );
	}

	@SqlResultSetMapping(
		name = "mapping",
		columns = @ColumnResult( name = "cnt" )
	)
	@NamedNativeQuery(
		name = "sample.count",
		resultSetMapping = "mapping",
		query = "SELECT count(*) AS cnt FROM Sample"
	)
	@Entity(name = "Sample")
	static class Sample {

		@Id
		Long id;

	}
}
