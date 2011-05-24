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
package org.hibernate.metamodel.source.annotations.util;

import static org.hamcrest.core.Is.is;

import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.LockModeType;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.internal.BasicServiceRegistryImpl;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static junit.framework.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
public class JandexHelperTest extends BaseUnitTestCase {
	private BasicServiceRegistryImpl serviceRegistry;
	private ClassLoaderService classLoaderService;

	@Before
	public void setUp() {
		serviceRegistry = (BasicServiceRegistryImpl) new ServiceRegistryBuilder().buildServiceRegistry();
		classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
	}

	@After
	public void tearDown() {
		serviceRegistry.destroy();
	}

	@Test
	public void testGetMemberAnnotations() {
		class Foo {
			@Column
			@Basic
			private String bar;

			private String fubar;
		}
		Index index = JandexHelper.indexForClass( classLoaderService, Foo.class );

		ClassInfo classInfo = index.getClassByName( DotName.createSimple( Foo.class.getName() ) );
		Map<DotName, List<AnnotationInstance>> memberAnnotations = JandexHelper.getMemberAnnotations(
				classInfo, "bar"
		);
		assertTrue(
				"property bar should defines @Column annotation",
				memberAnnotations.containsKey( DotName.createSimple( Column.class.getName() ) )
		);
		assertTrue(
				"property bar should defines @Basic annotation",
				memberAnnotations.containsKey( DotName.createSimple( Basic.class.getName() ) )
		);

		memberAnnotations = JandexHelper.getMemberAnnotations( classInfo, "fubar" );
		assertTrue( "there should be no annotations in fubar", memberAnnotations.isEmpty() );
	}

	@Test
	public void shouldRetrieveDefaultOfUnspecifiedAnnotationElement() {

		@NamedQuery(name="foo", query="bar")
		@SequenceGenerator(name="fu")
		class Foo {
		}

		Index index = JandexHelper.indexForClass(classLoaderService, Foo.class);
        for (AnnotationInstance query : index.getAnnotations( JPADotNames.NAMED_QUERY)) {
    		assertThat(JandexHelper.getValueAsEnum(index, query, "lockMode", LockModeType.class), is(LockModeType.NONE));
        }
        for (AnnotationInstance generator : index.getAnnotations( JPADotNames.SEQUENCE_GENERATOR)) {
            assertThat(JandexHelper.getValueAsInt(index, generator, "allocationSize"), is(50));
        }
	}

    @Test
    public void shouldRetrieveValueOfAnnotationElement() {

        @NamedQuery(name="foo", query="bar")
        class Foo {
        }

        Index index = JandexHelper.indexForClass(classLoaderService, Foo.class);
        for (AnnotationInstance query : index.getAnnotations( JPADotNames.NAMED_QUERY)) {
            assertThat(JandexHelper.getValueAsString(index, query, "name"), is("foo"));
        }
    }

    @Test
    public void shouldRetrieveValueOfEnumeratedAnnotationElement() {

        @NamedQuery(name="foo", query="bar", lockMode=LockModeType.OPTIMISTIC)
        class Foo {
        }

        Index index = JandexHelper.indexForClass(classLoaderService, Foo.class);
        for (AnnotationInstance query : index.getAnnotations( JPADotNames.NAMED_QUERY)) {
            assertThat(JandexHelper.getValueAsEnum(index, query, "lockMode", LockModeType.class), is(LockModeType.OPTIMISTIC));
        }
    }
}


