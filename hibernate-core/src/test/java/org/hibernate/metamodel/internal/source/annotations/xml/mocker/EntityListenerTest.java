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

import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.internal.jandex.PseudoJpaDotNames;

import org.junit.Test;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

import static org.junit.Assert.assertEquals;

/**
 * @author Strong Liu
 */
public class EntityListenerTest extends AbstractMockerTest {
	@Test
	public void basicEntityListenerMockTest() {
		Index index = getMockedIndex( "listener.xml" );
		DotName itemName = DotName.createSimple( Item.class.getName() );
		DotName itemListenerName = DotName.createSimple( ItemListener.class.getName() );
		ClassInfo itemClassInfo = index.getClassByName( itemName );
		assertEquals( 2, itemClassInfo.annotations().size() );
		//entity
		assertHasAnnotation( index, itemName, JPADotNames.ENTITY );
		assertHasAnnotation( index, itemName, JPADotNames.ENTITY_LISTENERS );
		//listener
		assertHasAnnotation( index, itemListenerName, JPADotNames.PRE_PERSIST );
		assertHasAnnotation( index, itemListenerName, JPADotNames.POST_PERSIST );
		//assert global configurations
		assertHasAnnotation( index, PseudoJpaDotNames.DEFAULT_DELIMITED_IDENTIFIERS );
		assertHasAnnotation( index, PseudoJpaDotNames.DEFAULT_ACCESS );
		assertHasAnnotation( index, PseudoJpaDotNames.DEFAULT_ENTITY_LISTENERS );
		assertHasAnnotation( index, PseudoJpaDotNames.DEFAULT_PRE_PERSIST );
		assertHasAnnotation( index, PseudoJpaDotNames.DEFAULT_POST_PERSIST );
	}
}
