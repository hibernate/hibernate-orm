/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate.foreignkeys.definition;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = H2Dialect.class)
public class ForeignKeyDefinitionOneToManyJoinTableTest
		extends AbstractForeignKeyDefinitionTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Box.class,
				Thing.class,
		};
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

	@Override
	protected boolean validate(String fileContent) {
		return fileContent.contains( "/* Thing_FK */" ) && fileContent.contains(
				"/* Box_FK */" );
	}
}
