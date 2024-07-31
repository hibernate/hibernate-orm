/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bootstrap.spi.metadatabuildercontributor;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
@JiraKey( value = "HHH-12589" )
public class SqlFunctionMetadataBuilderContributorClassTest
		extends AbstractSqlFunctionMetadataBuilderContributorTest {

	@Override
	protected Object matadataBuilderContributor() {
		return SqlFunctionMetadataBuilderContributor.class;
	}
}
