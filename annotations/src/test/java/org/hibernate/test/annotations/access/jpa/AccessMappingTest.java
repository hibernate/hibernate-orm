//$Id: AccessTest.java 15025 2008-08-11 09:14:39Z hardy.ferentschik $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.test.annotations.access.jpa;

import junit.framework.TestCase;

import org.hibernate.EntityMode;
import org.hibernate.MappingException;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.property.BasicPropertyAccessor;
import org.hibernate.property.DirectPropertyAccessor;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.entity.PojoEntityTuplizer;


/**
 * @author Hardy Ferentschik
 */
public class AccessMappingTest extends TestCase {

	public void testInconsistentAnnotationPlacement() throws Exception {
		AnnotationConfiguration cfg = new AnnotationConfiguration();
		cfg.addAnnotatedClass( Course.class );
		cfg.addAnnotatedClass( Student.class );
		try {
			cfg.buildSessionFactory();
			fail( "@Id and @OneToMany are not placed consistently in test entities. SessionFactory creation should fail." );
		}
		catch ( MappingException e ) {
			// success
		}
	}

	public void testFieldAnnotationPlacement() throws Exception {
		AnnotationConfiguration cfg = new AnnotationConfiguration();
		Class<?> classUnderTest = CourseFieldAccess.class;
		cfg.addAnnotatedClass( classUnderTest );
		cfg.addAnnotatedClass( Student.class );
		SessionFactoryImplementor factory = ( SessionFactoryImplementor ) cfg.buildSessionFactory();
		EntityMetamodel metaModel = factory.getEntityPersister( classUnderTest.getName() )
				.getEntityMetamodel();
		PojoEntityTuplizer tuplizer = ( PojoEntityTuplizer ) metaModel.getTuplizer( EntityMode.POJO );
		assertTrue(
				"Field access should be used.",
				tuplizer.getIdentifierGetter() instanceof DirectPropertyAccessor.DirectGetter
		);
	}

	public void testPropertyAnnotationPlacement() throws Exception {
		AnnotationConfiguration cfg = new AnnotationConfiguration();
		Class<?> classUnderTest = CoursePropertyAccess.class;
		cfg.addAnnotatedClass( classUnderTest );
		cfg.addAnnotatedClass( Student.class );
		SessionFactoryImplementor factory = ( SessionFactoryImplementor ) cfg.buildSessionFactory();
		EntityMetamodel metaModel = factory.getEntityPersister( classUnderTest.getName() )
				.getEntityMetamodel();
		PojoEntityTuplizer tuplizer = ( PojoEntityTuplizer ) metaModel.getTuplizer( EntityMode.POJO );
		assertTrue(
				"Property access should be used.",
				tuplizer.getIdentifierGetter() instanceof BasicPropertyAccessor.BasicGetter
		);
	}

	public void testExplicitPropertyAccessAnnotationsOnProperty() throws Exception {
		AnnotationConfiguration cfg = new AnnotationConfiguration();
		Class<?> classUnderTest = CourseExplicitPropertyAccess.class;
		cfg.addAnnotatedClass( classUnderTest );
		cfg.addAnnotatedClass( Student.class );
		SessionFactoryImplementor factory = ( SessionFactoryImplementor ) cfg.buildSessionFactory();
		EntityMetamodel metaModel = factory.getEntityPersister( classUnderTest.getName() )
				.getEntityMetamodel();
		PojoEntityTuplizer tuplizer = ( PojoEntityTuplizer ) metaModel.getTuplizer( EntityMode.POJO );
		assertTrue(
				"Property access should be used.",
				tuplizer.getIdentifierGetter() instanceof BasicPropertyAccessor.BasicGetter
		);
	}

	public void testExplicitPropertyAccessAnnotationsOnField() throws Exception {
		AnnotationConfiguration cfg = new AnnotationConfiguration();
		cfg.addAnnotatedClass( CourseExplicitPropertyAccess3.class );
		cfg.addAnnotatedClass( Student.class );
		try {
			cfg.buildSessionFactory();
			fail( "@Id and @OneToMany are not placed consistently in test entities. SessionFactory creation should fail." );
		}
		catch ( MappingException e ) {
			// success
		}
	}

	public void testExplicitPropertyAccessAnnotationsWithHibernateStyleOverride() throws Exception {
		AnnotationConfiguration cfg = new AnnotationConfiguration();
		Class<?> classUnderTest = CourseExplicitPropertyAccess2.class;
		cfg.addAnnotatedClass( classUnderTest );
		cfg.addAnnotatedClass( Student.class );
		SessionFactoryImplementor factory = ( SessionFactoryImplementor ) cfg.buildSessionFactory();
		EntityMetamodel metaModel = factory.getEntityPersister( classUnderTest.getName() )
				.getEntityMetamodel();
		PojoEntityTuplizer tuplizer = ( PojoEntityTuplizer ) metaModel.getTuplizer( EntityMode.POJO );
		assertTrue(
				"Field access should be used.",
				tuplizer.getIdentifierGetter() instanceof DirectPropertyAccessor.DirectGetter
		);

		assertTrue(
				"Property access should be used.",
				tuplizer.getGetter( 0 ) instanceof BasicPropertyAccessor.BasicGetter
		);
	}

	public void testExplicitPropertyAccessAnnotationsWithJpaStyleOverride() throws Exception {
		AnnotationConfiguration cfg = new AnnotationConfiguration();
		Class<?> classUnderTest = CourseExplicitPropertyAccess4.class;
		cfg.addAnnotatedClass( classUnderTest );
		cfg.addAnnotatedClass( Student.class );
		SessionFactoryImplementor factory = ( SessionFactoryImplementor ) cfg.buildSessionFactory();
		EntityMetamodel metaModel = factory.getEntityPersister( classUnderTest.getName() )
				.getEntityMetamodel();
		PojoEntityTuplizer tuplizer = ( PojoEntityTuplizer ) metaModel.getTuplizer( EntityMode.POJO );
		assertTrue(
				"Field access should be used.",
				tuplizer.getIdentifierGetter() instanceof DirectPropertyAccessor.DirectGetter
		);

		assertTrue(
				"Property access should be used.",
				tuplizer.getGetter( 0 ) instanceof BasicPropertyAccessor.BasicGetter
		);
	}
}