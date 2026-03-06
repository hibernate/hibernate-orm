/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.derived;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import org.hibernate.annotations.DerivedColumn;
import org.hibernate.testing.orm.junit.DialectFeatureChecks.SupportsGeneratedColumns;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = DerivedColumnTest.Thing.class)
@RequiresDialectFeature( feature = SupportsGeneratedColumns.class )
class DerivedColumnTest {

	@Test
	void test(EntityManagerFactoryScope scope) {
		Thing thing = new Thing();
		thing.number = 3;
		scope.inTransaction( s -> s.persist( thing ) );
		scope.inTransaction( s -> {
			Thing t = s.find( Thing.class, 1L );
			assertEquals( 12, t.computedNumber );
			assertEquals( 69, t.constantNumber );
		} );
	}

	@Entity(name = "DerivedColumnThing")
	@DerivedColumn(name = "computed",
			sqlType = Types.DOUBLE,
			value = "num*(num+1)",
			comment = "n(n+1)")
	@DerivedColumn(name = "const",
			table = "DerivedColumnExtraThing",
			sqlType = Types.SMALLINT,
			hidden = true,
			stored = false,
			value = "69",
			comment = "sixty-nine dude")
	@SecondaryTable( name = "DerivedColumnExtraThing" )
	static class Thing {
		@GeneratedValue @Id
		long id;
		@Column(name = "num")
		double number;

		@Column(name = "computed",
				insertable = false,
				updatable = false)
		int computedNumber;

		@Column(name = "const",
				table = "DerivedColumnExtraThing",
				insertable = false,
				updatable = false)
		int constantNumber;

		@ElementCollection
		@DerivedColumn( name = "str_len",
				sqlType = Types.INTEGER,
				value = "length(string)")
		@Column(name = "string")
		List<String> strings;
	}

}
