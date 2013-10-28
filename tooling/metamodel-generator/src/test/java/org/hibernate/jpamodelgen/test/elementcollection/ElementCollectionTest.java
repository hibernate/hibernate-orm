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
package org.hibernate.jpamodelgen.test.elementcollection;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.hibernate.jpamodelgen.test.util.WithMappingFiles;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMapAttributesInMetaModelFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertNoSourceFileGeneratedFor;

/**
 * @author Hardy Ferentschik
 */
public class ElementCollectionTest extends CompilationTest {

	@Test
	@TestForIssue(jiraKey = "METAGEN-8")
	@WithClasses({ House.class, Room.class })
	public void testElementCollectionOnMap() {
		assertMetamodelClassGeneratedFor( House.class );
		assertMetamodelClassGeneratedFor( Room.class );
		// side effect of METAGEN-8 was that a meta class for String was created!
		assertNoSourceFileGeneratedFor( String.class );
	}

	@Test
	@TestForIssue(jiraKey = "METAGEN-19")
	@WithClasses({ Hotel.class, Room.class, Cleaner.class })
	public void testMapKeyClass() {
		assertMetamodelClassGeneratedFor( Hotel.class );
		assertMapAttributesInMetaModelFor(
				Hotel.class, "roomsByName", String.class, Room.class, "Wrong type in map attribute."
		);

		assertMapAttributesInMetaModelFor(
				Hotel.class, "cleaners", Room.class, Cleaner.class, "Wrong type in map attribute."
		);
	}

	@Test
	@TestForIssue(jiraKey = "METAGEN-22")
	@WithClasses({ Hostel.class, Room.class, Cleaner.class })
	@WithMappingFiles("hostel.xml")
	public void testMapKeyClassXmlConfigured() {
		assertMetamodelClassGeneratedFor( Hostel.class );
		assertMapAttributesInMetaModelFor(
				Hostel.class, "roomsByName", String.class, Room.class, "Wrong type in map attribute."
		);

		assertMapAttributesInMetaModelFor(
				Hostel.class, "cleaners", Room.class, Cleaner.class, "Wrong type in map attribute."
		);
	}
}
