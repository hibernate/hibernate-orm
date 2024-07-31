/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.circulartypevariable;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import jakarta.persistence.metamodel.SetAttribute;

@JiraKey(value = "HHH-17253")
public class CircularTypeVariableTest extends CompilationTest {

    @Test
    @WithClasses({ RoleAccess.class, User.class })
    public void testCircularTypeVariable() {
        TestUtil.assertMetamodelClassGeneratedFor( RoleAccess.class );
        TestUtil.assertMetamodelClassGeneratedFor( User.class );
    }

}
