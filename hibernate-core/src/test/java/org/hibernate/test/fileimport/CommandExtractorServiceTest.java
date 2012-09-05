/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.fileimport;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.tool.hbm2ddl.ImportSqlCommandExtractor;
import org.hibernate.tool.hbm2ddl.MultipleLinesSqlCommandExtractor;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue( jiraKey = "HHH-2403" )
@RequiresDialect(value = H2Dialect.class,
		jiraKey = "HHH-6286",
		comment = "Only running the tests against H2, because the sql statements in the import file are not generic. " +
				"This test should actually not test directly against the db")
public class CommandExtractorServiceTest extends MultiLineImportFileTest {
	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.HBM2DDL_IMPORT_FILES, "/org/hibernate/test/fileimport/multi-line-statements.sql" );
	}

	@Override
	protected void prepareBasicRegistryBuilder(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		super.prepareBasicRegistryBuilder( serviceRegistryBuilder );
		serviceRegistryBuilder.addService( ImportSqlCommandExtractor.class, new MultipleLinesSqlCommandExtractor() );
	}
}
