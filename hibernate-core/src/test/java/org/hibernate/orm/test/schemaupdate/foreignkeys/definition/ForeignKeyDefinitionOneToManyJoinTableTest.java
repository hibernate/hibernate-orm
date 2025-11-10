/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.foreignkeys.definition;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
public class ForeignKeyDefinitionOneToManyJoinTableTest extends AbstractForeignKeyDefinitionTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Box.class, Thing.class };
	}

	@Override
	protected boolean validate(String fileContent) {
		return fileContent.contains( "/* Thing_FK */" )
			&& fileContent.contains( "/* Box_FK */" );
	}

	@Entity(name = "Box")
	public static class Box {
		@Id
		public Integer id;
	}

	@Entity(name = "Thing")
	public static class Thing {
		@Id
		public Integer id;

		@OneToMany
		@JoinTable(name = "box_thing",
				joinColumns = @JoinColumn(name = "box_id"),
				inverseJoinColumns = @JoinColumn(name = "thing_id"),
				foreignKey = @ForeignKey(
						name = "thingy",
						foreignKeyDefinition = "foreign key /* Thing_FK */ (thing_id) references Thing"
				),
				inverseForeignKey = @ForeignKey(
						name = "boxy",
						foreignKeyDefinition = "foreign key /* Box_FK */ (box_id) references Box"
				)
		)
		public List<Thing> things = new ArrayList<>();
	}
}
