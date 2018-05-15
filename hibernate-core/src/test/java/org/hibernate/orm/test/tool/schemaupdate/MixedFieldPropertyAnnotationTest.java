/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) {DATE}, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.util.EnumSet;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.RequiresDialect;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-9849")
@RequiresDialect(dialectClass = MySQLDialect.class, matchSubTypes = true)
public class MixedFieldPropertyAnnotationTest extends BaseSchemaUnitTestCase {
	protected ServiceRegistry serviceRegistry;
	protected MetadataImplementor metadata;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { MyEntity.class };
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.applySetting( Environment.GLOBALLY_QUOTED_IDENTIFIERS, "false" );
	}

	@Override
	protected void beforeEach(SchemaScope scope) {
		scope.withSchemaExport( schemaExport ->
										schemaExport.create( EnumSet.of( TargetType.STDOUT, TargetType.DATABASE ) ) );
	}

	@SchemaTest
	public void testUpdateSchema(SchemaScope scope) {
		scope.withSchemaUpdate( schemaUpdate ->
										schemaUpdate.execute( EnumSet.of( TargetType.STDOUT, TargetType.DATABASE ) ) );
	}

	@Entity
	@Table(name = "MyEntity")
	class MyEntity {

		@Id
		public int getId() {
			return 0;
		}

		@Column(name = "Ul")
		public int getValue() {
			return 0;
		}

		public void setId(final int _id) {
		}

		public void setValue(int value) {
		}

	}
}
