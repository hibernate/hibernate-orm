/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate.foreignkeys.definition;

import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = H2Dialect.class)
public class ForeignKeyDefinitionManyToOneTest
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

		@ManyToOne
		@JoinColumn(foreignKey = @ForeignKey(name = "thingy", foreignKeyDefinition = "foreign key /* FK */ (thing_id) references Thing"))
		public Thing thing;
	}

	@Entity(name = "Thing")
	public static class Thing {
		@Id
		public Integer id;
	}

	@Override
	protected boolean validate(String fileContent) {
		return fileContent.contains( "/* FK */" );
	}
}
