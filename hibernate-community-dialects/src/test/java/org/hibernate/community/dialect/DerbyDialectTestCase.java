/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.orm.test.dialect.LimitQueryOptions;
import org.hibernate.query.spi.Limit;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testing of patched support for Derby limit and offset queries; see HHH-3972
 *
 * @author Evan Leonard
 */
@SessionFactory
@DomainModel(annotatedClasses = DerbyDialectTestCase.Constrained.class)
public class DerbyDialectTestCase {

	@Test
	@JiraKey(value = "HHH-3972")
	public void testInsertLimitClause(SessionFactoryScope scope) {
		final int limit = 50;
		final String input = "select * from tablename t where t.cat = 5";
		final String expected = "select * from tablename t where t.cat = 5 fetch first ? rows only";

		final String actual = withLimit( input, toRowSelection( 0, limit ) );
		assertThat( actual ).isEqualTo( expected );
	}

	@Test
	@JiraKey(value = "HHH-3972")
	public void testInsertLimitWithOffsetClause(SessionFactoryScope scope) {
		final int limit = 50;
		final int offset = 200;
		final String input = "select * from tablename t where t.cat = 5";
		final String expected = "select * from tablename t where t.cat = 5 offset ? rows fetch next ? rows only";

		final String actual = withLimit( input, toRowSelection( offset, limit ) );
		assertThat( actual ).isEqualTo( expected );
	}

	@Test
	@JiraKey(value = "HHH-3972")
	public void testInsertLimitWithForUpdateClause(SessionFactoryScope scope) {
		final int limit = 50;
		final int offset = 200;
		final String input = "select c11 as col1, c12 as col2, c13 as col13 from t1 for update of c11, c13";
		final String expected = "select c11 as col1, c12 as col2, c13 as col13 from t1 offset ? rows fetch next ? rows only for update of c11, c13";

		final String actual = withLimit( input, toRowSelection( offset, limit ) );
		assertThat( actual ).isEqualTo( expected );
	}

	@Test
	@JiraKey(value = "HHH-3972")
	public void testInsertLimitWithWithClause(SessionFactoryScope scope) {
		final int limit = 50;
		final int offset = 200;
		final String input = "select c11 as col1, c12 as col2, c13 as col13 from t1 where flight_id between 'AA1111' and 'AA1112' with rr";
		final String expected = "select c11 as col1, c12 as col2, c13 as col13 from t1 where flight_id between 'AA1111' and 'AA1112' offset ? rows fetch next ? rows only with rr";

		final String actual = withLimit( input, toRowSelection( offset, limit ) );
		assertThat( actual ).isEqualTo( expected );
	}

	@Test
	@JiraKey(value = "HHH-3972")
	public void testInsertLimitWithForUpdateAndWithClauses(SessionFactoryScope scope) {
		final int limit = 50;
		final int offset = 200;
		final String input = "select c11 as col1, c12 as col2, c13 as col13 from t1 where flight_id between 'AA1111' and 'AA1112' for update of c11,c13 with rr";
		final String expected = "select c11 as col1, c12 as col2, c13 as col13 from t1 where flight_id between 'AA1111' and 'AA1112' offset ? rows fetch next ? rows only for update of c11,c13 with rr";

		final String actual = withLimit( input, toRowSelection( offset, limit ) );
		assertThat( actual ).isEqualTo( expected );
	}

	@RequiresDialect(DerbyDialect.class)
	@Test
	void testInsertConflictOnConstraintDoNothing(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
		scope.inTransaction( s -> s.persist( new Constrained() ) );
		scope.inTransaction( s -> s.createMutationQuery(
						"insert into Constrained(id, name, count) values (4,'Gavin',69) on conflict on constraint count_name_key do nothing" )
				.executeUpdate() );
		scope.inSession( s -> Assertions.assertEquals( 69,
				s.createSelectionQuery( "select count from Constrained", int.class ).getSingleResult() ) );
	}

	private String withLimit(String sql, Limit limit) {
		return new DerbyDialect().getLimitHandler().processSql( sql, -1, null, new LimitQueryOptions( limit ) );
	}

	private Limit toRowSelection(int firstRow, int maxRows) {
		Limit selection = new Limit();
		selection.setFirstRow( firstRow );
		selection.setMaxRows( maxRows );
		return selection;
	}

	@Entity(name = "Constrained")
	@Table(uniqueConstraints = @UniqueConstraint(name = "count_name_key", columnNames = {"count", "name"}))
	static class Constrained {
		@Id
		@GeneratedValue
		long id;
		String name = "Gavin";
		int count = 69;
	}

}
