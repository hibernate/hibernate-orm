/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.tool.schema;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.tool.schema.internal.DefaultSchemaFilter;
import org.hibernate.tool.schema.internal.GroupedSchemaValidatorImpl;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10332")
@RequiresDialect(H2Dialect.class)
public class GroupedSchemaValidatorImplTest extends IndividuallySchemaValidatorImplTest {
	@Override
	protected void getSchemaValidator(MetadataImplementor metadata) {
		new GroupedSchemaValidatorImpl( tool, DefaultSchemaFilter.INSTANCE )
				.doValidation( metadata, executionOptions );
	}
}
