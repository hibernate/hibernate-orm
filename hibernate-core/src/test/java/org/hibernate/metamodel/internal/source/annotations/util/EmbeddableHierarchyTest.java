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
package org.hibernate.metamodel.internal.source.annotations.util;

import java.util.Iterator;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.junit.Ignore;
import org.junit.Test;

import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.internal.source.annotations.entity.EmbeddableClass;
import org.hibernate.metamodel.internal.source.annotations.entity.EmbeddableHierarchy;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;

/**
 * @author Hardy Ferentschik
 */
@Ignore("fails on openjdk")
public class EmbeddableHierarchyTest extends BaseAnnotationIndexTestCase {
	@Test
    @Ignore("HHH-6606 ignore for now")
	public void testEmbeddableHierarchy() {
		@Embeddable
		class A {
			String foo;
		}

		class B extends A {
		}

		@Embeddable
		class C extends B {
			String bar;
		}

		EmbeddableHierarchy hierarchy = createEmbeddableHierarchy(
				AccessType.FIELD, SingularAttributeBinding.NaturalIdMutability.NOT_NATURAL_ID,
				C.class,
				A.class,
				B.class
		);
		Iterator<EmbeddableClass> iter = hierarchy.iterator();
		ClassInfo info = iter.next().getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( A.class.getName() ), info.name() );
		info = iter.next().getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( B.class.getName() ), info.name() );
		info = iter.next().getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( C.class.getName() ), info.name() );
		assertFalse( iter.hasNext() );
		assertNotNull( hierarchy );
	}

	@Test(expected = AssertionFailure.class)
	public void testEmbeddableHierarchyWithNotAnnotatedEntity() {
		class NonAnnotatedEmbeddable {
		}

		createEmbeddableHierarchy( AccessType.FIELD,
				SingularAttributeBinding.NaturalIdMutability.NOT_NATURAL_ID, NonAnnotatedEmbeddable.class );
	}

	@Entity
	public class Foo {
		@Id
		@GeneratedValue
		private int id;
	}

	@Entity
	public class A {
		@Id
		@GeneratedValue
		private int id;
	}

	@Entity
	public class B extends A {
		private String name;
	}
}


