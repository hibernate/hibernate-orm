/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemacreation.collections;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import org.hibernate.orm.test.tool.schemacreation.BaseSchemaCreationTestCase;

import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

/**
 * @author Andrea Boriero
 */
public class OrderListSchemaCreationTest extends BaseSchemaCreationTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Item.class };
	}

	@SchemaTest
	public void testElementAndCollectionTableAreCreated(SchemaScope scope) throws Exception {

		assertThatTablesAreCreated(
				"item (id bigint not null, primary key (id))",
				"image (image_name varchar(255), images_order integer not null, item_id bigint not null, primary key (item_id, images_order))"
		);

		assertThatActionIsGenerated(
				"alter table image add constraint (.*) foreign key \\(item_id\\) references item"
		);
	}

	@Entity(name = "Item")
	@Table(name = "ITEM")
	public static class Item {
		@Id
		private Long id;

		@ElementCollection
		@CollectionTable(name = "IMAGE", joinColumns = @JoinColumn(name = "ITEM_ID"))
		@OrderColumn
		@Column(name = "IMAGE_NAME")
		private List<String> images = new ArrayList<>();

	}
}
