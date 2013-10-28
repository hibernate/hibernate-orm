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
package org.hibernate.jpamodelgen.test.xmlonly;

import org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor;
import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.hibernate.jpamodelgen.test.util.WithProcessorOption;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;

/**
 * @author Hardy Ferentschik
 */
@WithClasses({ Car.class, Course.class, Option.class, Period.class, Teacher.class, Tire.class, XmlOnly.class })
@WithProcessorOption(key = JPAMetaModelEntityProcessor.PERSISTENCE_XML_OPTION,
		value = "org/hibernate/jpamodelgen/test/xmlonly/persistence.xml")
public class XmlOnlyTest extends CompilationTest {
	@Test
	public void testMetaModelGeneratedForXmlConfiguredEntity() {
		assertMetamodelClassGeneratedFor( XmlOnly.class );
	}

	@Test
	public void testMetaModelGeneratedForManyToManyFieldAccessWithoutTargetEntity() {
		assertPresenceOfFieldInMetamodelFor( Course.class, "qualifiedTeachers", "Type should be inferred from field" );
		assertPresenceOfFieldInMetamodelFor( Teacher.class, "qualifiedFor", "Type should be inferred from field" );
	}

	@Test
	public void testMetaModelGeneratedForOneToManyPropertyAccessWithoutTargetEntity() {
		assertPresenceOfFieldInMetamodelFor( Car.class, "tires", "Type should be inferred from field" );
		assertPresenceOfFieldInMetamodelFor( Tire.class, "car", "Type should be inferred from field" );
	}

	@Test
	public void testMetaModelGeneratedForEmbeddable() {
		assertPresenceOfFieldInMetamodelFor( Option.class, "period", "Embedded expected" );
		assertPresenceOfFieldInMetamodelFor( Period.class, "start", "Embedded expected" );
		assertPresenceOfFieldInMetamodelFor( Period.class, "end", "Embedded expected" );
	}
}
