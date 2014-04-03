/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.access.jpa;

import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.spi.LenientPersistentAttributeMemberResolver;
import org.hibernate.property.BasicPropertyAccessor;
import org.hibernate.property.DirectPropertyAccessor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tuple.entity.EntityTuplizer;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Tests verifying the correct behaviour for the usage of {@code @javax.persistence.Access}.
 *
 * @author Hardy Ferentschik
 */
@SuppressWarnings({ "deprecation" })
public class AccessMappingTest extends BaseUnitTestCase {
    private ServiceRegistry serviceRegistry;

    @Before
    public void setUp() {
        serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
    }

    @After
    public void tearDown() {
        if ( serviceRegistry != null ) {
            ServiceRegistryBuilder.destroy( serviceRegistry );
        }
    }

    @Test
    public void testInconsistentAnnotationPlacement() throws Exception {
        AnnotationConfiguration cfg = new AnnotationConfiguration();
        cfg.addAnnotatedClass( Course1.class );
        cfg.addAnnotatedClass( Student.class );
		SessionFactory sf = null;
        try {
           sf= cfg.buildSessionFactory( serviceRegistry );
            fail( "@Id and @OneToMany are not placed consistently in test entities. SessionFactory creation should fail." );
        }
        catch ( MappingException e ) {
            // success
        } finally {
			if(sf!=null){
				sf.close();
			}
		}
    }

    @Test
    public void testFieldAnnotationPlacement() throws Exception {
        AnnotationConfiguration cfg = new AnnotationConfiguration();
        Class<?> classUnderTest = Course6.class;
        cfg.addAnnotatedClass( classUnderTest );
        cfg.addAnnotatedClass( Student.class );
        SessionFactoryImplementor factory = (SessionFactoryImplementor) cfg.buildSessionFactory( serviceRegistry );
        EntityTuplizer tuplizer = factory.getEntityPersister( classUnderTest.getName() )
                .getEntityMetamodel()
                .getTuplizer();
        assertTrue(
                "Field access should be used.",
                tuplizer.getIdentifierGetter() instanceof DirectPropertyAccessor.DirectGetter
        );
		factory.close();
    }

    @Test
    public void testPropertyAnnotationPlacement() throws Exception {
        AnnotationConfiguration cfg = new AnnotationConfiguration();
        Class<?> classUnderTest = Course7.class;
        cfg.addAnnotatedClass( classUnderTest );
        cfg.addAnnotatedClass( Student.class );
        SessionFactoryImplementor factory = (SessionFactoryImplementor) cfg.buildSessionFactory( serviceRegistry );
        EntityTuplizer tuplizer = factory.getEntityPersister( classUnderTest.getName() )
                .getEntityMetamodel()
                .getTuplizer();
        assertTrue(
                "Property access should be used.",
                tuplizer.getIdentifierGetter() instanceof BasicPropertyAccessor.BasicGetter
        );
		factory.close();
    }

    @Test
    public void testExplicitPropertyAccessAnnotationsOnProperty() throws Exception {
        AnnotationConfiguration cfg = new AnnotationConfiguration();
        Class<?> classUnderTest = Course2.class;
        cfg.addAnnotatedClass( classUnderTest );
        cfg.addAnnotatedClass( Student.class );
        SessionFactoryImplementor factory = (SessionFactoryImplementor) cfg.buildSessionFactory( serviceRegistry );
        EntityTuplizer tuplizer = factory.getEntityPersister( classUnderTest.getName() )
                .getEntityMetamodel()
                .getTuplizer();
        assertTrue(
                "Property access should be used.",
                tuplizer.getIdentifierGetter() instanceof BasicPropertyAccessor.BasicGetter
        );
		factory.close();
    }

    @Test
    public void testExplicitPropertyAccessAnnotationsOnField() throws Exception {
        AnnotationConfiguration cfg = new AnnotationConfiguration();
        cfg.addAnnotatedClass( Course4.class );
        cfg.addAnnotatedClass( Student.class );
		SessionFactory sf= null;
        try {
			sf = cfg.buildSessionFactory( serviceRegistry );
			fail( "@Id and @OneToMany are not placed consistently in test entities. SessionFactory creation should fail." );
        }
        catch ( MappingException e ) {
            // success
        }
		finally {
			if ( sf != null ) {
				sf.close();
			}
		}
    }

    @Test
    public void testExplicitPropertyAccessAnnotationsWithHibernateStyleOverride() throws Exception {
		MetadataSources sources = new MetadataSources( serviceRegistry );
		sources.addAnnotatedClass( Course3.class )
				.addAnnotatedClass( Student.class );

		SessionFactoryImplementor factory = (SessionFactoryImplementor) sources.getMetadataBuilder()
				.with( LenientPersistentAttributeMemberResolver.INSTANCE )
				.build()
				.buildSessionFactory();

        EntityTuplizer tuplizer = factory.getEntityPersister( Course3.class.getName() )
                .getEntityMetamodel()
                .getTuplizer();
        assertTrue(
                "Field access should be used.",
                tuplizer.getIdentifierGetter() instanceof DirectPropertyAccessor.DirectGetter
        );

        assertTrue(
                "Property access should be used.",
                tuplizer.getGetter( 0 ) instanceof BasicPropertyAccessor.BasicGetter
        );
		factory.close();
    }

    @Test
    public void testExplicitPropertyAccessAnnotationsWithJpaStyleOverride() throws Exception {
        AnnotationConfiguration cfg = new AnnotationConfiguration();
        Class<?> classUnderTest = Course5.class;
        cfg.addAnnotatedClass( classUnderTest );
        cfg.addAnnotatedClass( Student.class );
        SessionFactoryImplementor factory = (SessionFactoryImplementor) cfg.buildSessionFactory( serviceRegistry );
        EntityTuplizer tuplizer = factory.getEntityPersister( classUnderTest.getName() )
                .getEntityMetamodel()
                .getTuplizer();
        assertTrue(
                "Field access should be used.",
                tuplizer.getIdentifierGetter() instanceof DirectPropertyAccessor.DirectGetter
        );

        assertTrue(
                "Property access should be used.",
                tuplizer.getGetter( 0 ) instanceof BasicPropertyAccessor.BasicGetter
        );
		factory.close();
    }

    @Test
    public void testDefaultFieldAccessIsInherited() throws Exception {
        AnnotationConfiguration cfg = new AnnotationConfiguration();
        Class<?> classUnderTest = User.class;
        cfg.addAnnotatedClass( classUnderTest );
        cfg.addAnnotatedClass( Person.class );
        cfg.addAnnotatedClass( Being.class );
        SessionFactoryImplementor factory = (SessionFactoryImplementor) cfg.buildSessionFactory( serviceRegistry );
        EntityTuplizer tuplizer = factory.getEntityPersister( classUnderTest.getName() )
                .getEntityMetamodel()
                .getTuplizer();
        assertTrue(
                "Field access should be used since the default access mode gets inherited",
                tuplizer.getIdentifierGetter() instanceof DirectPropertyAccessor.DirectGetter
        );
		factory.close();
    }

    @Test
    public void testDefaultPropertyAccessIsInherited() throws Exception {
        AnnotationConfiguration cfg = new AnnotationConfiguration();
        cfg.addAnnotatedClass( Horse.class );
        cfg.addAnnotatedClass( Animal.class );

        SessionFactoryImplementor factory = (SessionFactoryImplementor) cfg.buildSessionFactory( serviceRegistry );
        EntityTuplizer tuplizer = factory.getEntityPersister( Animal.class.getName() )
                .getEntityMetamodel()
                .getTuplizer();
        assertTrue(
                "Property access should be used since explicity configured via @Access",
                tuplizer.getIdentifierGetter() instanceof BasicPropertyAccessor.BasicGetter
        );

        tuplizer = factory.getEntityPersister( Horse.class.getName() )
                .getEntityMetamodel()
                .getTuplizer();
        assertTrue(
                "Field access should be used since the default access mode gets inherited",
                tuplizer.getGetter( 0 ) instanceof DirectPropertyAccessor.DirectGetter
        );
		factory.close();
    }

    @Test
	@TestForIssue(jiraKey = "HHH-5004")
    public void testAccessOnClassAndId() throws Exception {
        AnnotationConfiguration cfg = new AnnotationConfiguration();
        cfg.addAnnotatedClass( Course8.class );
        cfg.addAnnotatedClass( Student.class );
        cfg.buildSessionFactory( serviceRegistry ).close();
    }
}
