/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
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
package org.hibernate.jpamodelgen.test.separatecompilationunits;

import org.hibernate.jpamodelgen.test.separatecompilationunits.superclass.MappedSuperclass;
import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.IgnoreCompilationErrors;
import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "METAGEN-35")
public class SeparateCompilationUnitsTest extends CompilationTest {
	@Test
	@WithClasses(value = Entity.class, preCompile = MappedSuperclass.class)
	@IgnoreCompilationErrors
	public void testInheritance() throws Exception {
		// need to work with the source file. Entity_.class won't get generated, because the mapped superclass
		// will not be on the classpath
		String entityMetaModel = getMetaModelSourceAsString( Entity.class );
		assertTrue(
				entityMetaModel.contains(
						"extends org.hibernate.jpamodelgen.test.separatecompilationunits.superclass.MappedSuperclass"
				)
		);
	}
}
