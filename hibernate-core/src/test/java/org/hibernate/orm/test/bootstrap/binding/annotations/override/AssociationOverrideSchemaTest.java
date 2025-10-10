/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.override;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.mapping.Table;
import org.hibernate.orm.test.util.SchemaUtil;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Lukasz Antoniak
 */
@RequiresDialect(H2Dialect.class)
@JiraKey(value = "HHH-6662")
@DomainModel(
		annotatedClasses = {
				Entry.class,
				BlogEntry.class
		}
)
@SessionFactory(
		createSecondarySchemas = true
)
public class AssociationOverrideSchemaTest {

	public static final String SCHEMA_NAME = "OTHER_SCHEMA";
	public static final String TABLE_NAME = "BLOG_TAGS";
	public static final String ID_COLUMN_NAME = "BLOG_ID";
	public static final String VALUE_COLUMN_NAME = "BLOG_TAG";


	@Test
	public void testJoinTableSchemaName(SessionFactoryScope scope) {
		for ( Table table : scope.getMetadataImplementor().collectTableMappings() ) {
			if ( TABLE_NAME.equals( table.getName() ) ) {
				assertThat( table.getSchema() ).isEqualTo( SCHEMA_NAME );
				return;
			}
		}
		fail("Table " + TABLE_NAME + " not found");
	}

	@Test
	public void testJoinTableJoinColumnName(SessionFactoryScope scope) {
		assertThat( SchemaUtil.isColumnPresent( TABLE_NAME, ID_COLUMN_NAME, scope.getMetadataImplementor() ) ).isTrue();
	}

	@Test
	public void testJoinTableColumnName(SessionFactoryScope scope) {
		assertThat(	SchemaUtil.isColumnPresent( TABLE_NAME, VALUE_COLUMN_NAME, scope.getMetadataImplementor() ) ).isTrue();
	}
}
