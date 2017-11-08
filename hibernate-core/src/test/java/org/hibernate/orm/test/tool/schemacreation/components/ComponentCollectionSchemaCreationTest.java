/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemacreation.components;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.orm.test.tool.schemacreation.BaseSchemaCreationTestCase;

import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

/**
 * @author Andrea Boriero
 */
public class ComponentCollectionSchemaCreationTest extends BaseSchemaCreationTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Item.class };
	}

	@SchemaTest
	public void testTableIsCreated(SchemaScope scope) {

		assertThatTablesAreCreated(
				"item (filename varchar(255) not null, format varchar(255), id bigint not null, size integer, primary key (id))"
		);
	}

	@Entity(name = "Item")
	@Table(name = "ITEM")
	public static class Item {
		@Id
		private Long id;

		Image image;
	}

	@Embeddable
	public class Image {
		@Column(nullable = false, name = "filename")
		String filename;

		@Column(name = "size")
		int size;

		@Column(name = "format")
		String format;
	}
}
