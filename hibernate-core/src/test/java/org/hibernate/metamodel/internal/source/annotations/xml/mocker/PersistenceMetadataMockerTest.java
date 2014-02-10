/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc..
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
package org.hibernate.metamodel.internal.source.annotations.xml.mocker;

import javax.persistence.AccessType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.Index;
import org.junit.Test;

import org.hibernate.metamodel.source.internal.jandex.PseudoJpaDotNames;

import static org.junit.Assert.assertEquals;

/**
 * @author Strong Liu
 */
public class PersistenceMetadataMockerTest extends AbstractMockerTest {
	@Test
	public void testPersistenceMetadata() {
		Index index = getMockedIndex( "persistence-metadata.xml" );
		assertHasAnnotation( index, null, PseudoJpaDotNames.DEFAULT_ACCESS, 1 );
		assertAnnotationValue(index,null, PseudoJpaDotNames.DEFAULT_ACCESS,new AnnotationValueChecker(){
			@Override
			public void check(AnnotationInstance annotationInstance) {
			assertEquals( AccessType.FIELD.toString(), annotationInstance.value().asEnum());
			}
		});
	}
}
