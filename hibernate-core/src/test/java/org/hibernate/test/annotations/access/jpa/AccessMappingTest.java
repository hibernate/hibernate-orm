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

import java.util.Arrays;

import org.junit.Test;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.property.BasicPropertyAccessor;
import org.hibernate.property.DirectPropertyAccessor;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestMethod;
import org.hibernate.tuple.entity.EntityTuplizer;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Tests verifying the correct behaviour for the usage of {@code @javax.persistence.Access}.
 *
 * @author Hardy Ferentschik
 */
@SuppressWarnings({ "deprecation" })
public class AccessMappingTest extends BaseCoreFunctionalTestMethod{

    @Test
	@FailureExpectedWithNewMetamodel
    public void testInconsistentAnnotationPlacement() throws Exception {
        try {
			getSF( Course1.class, Student.class );
            fail( "@Id and @OneToMany are not placed consistently in test entities. SessionFactory creation should fail." );
        }
        catch ( MappingException e ) {
            // success
        }
    }

	@Test
	public void testFieldAnnotationPlacement() throws Exception {
		SessionFactoryImplementor factory =getSF( Course6.class, Student.class );
		EntityTuplizer tuplizer = factory.getEntityPersister( Course6.class.getName() )
				.getEntityMetamodel()
				.getTuplizer();
		assertTrue(
				"Field access should be used.",
				tuplizer.getIdentifierGetter() instanceof DirectPropertyAccessor.DirectGetter
		);
	}

    @Test
    public void testPropertyAnnotationPlacement() throws Exception {
        Class<?> classUnderTest = Course7.class;
        SessionFactoryImplementor factory = getSF( classUnderTest, Student.class );
        EntityTuplizer tuplizer = factory.getEntityPersister( classUnderTest.getName() )
                .getEntityMetamodel()
                .getTuplizer();
        assertTrue(
				"Property access should be used.",
				tuplizer.getIdentifierGetter() instanceof BasicPropertyAccessor.BasicGetter
		);
    }

    @Test
    public void testExplicitPropertyAccessAnnotationsOnProperty() throws Exception {
        Class<?> classUnderTest = Course2.class;
        SessionFactoryImplementor factory = getSF( classUnderTest, Student.class );
        EntityTuplizer tuplizer = factory.getEntityPersister( classUnderTest.getName() )
                .getEntityMetamodel()
                .getTuplizer();
        assertTrue(
				"Property access should be used.",
				tuplizer.getIdentifierGetter() instanceof BasicPropertyAccessor.BasicGetter
		);
    }

    @Test
	@FailureExpectedWithNewMetamodel
    public void testExplicitPropertyAccessAnnotationsOnField() throws Exception {
        try {
           getSF( Course4.class, Student.class );
            fail( "@Id and @OneToMany are not placed consistently in test entities. SessionFactory creation should fail." );
        }
        catch ( MappingException e ) {
            // success
        }
    }

    @Test
    public void testExplicitPropertyAccessAnnotationsWithHibernateStyleOverride() throws Exception {
        Class<?> classUnderTest = Course3.class;
        SessionFactoryImplementor factory = getSF( classUnderTest, Student.class );
        EntityTuplizer tuplizer = factory.getEntityPersister( classUnderTest.getName() )
                .getEntityMetamodel()
                .getTuplizer();
        assertTrue(
                "Field access should be used.",
                tuplizer.getIdentifierGetter() instanceof DirectPropertyAccessor.DirectGetter
        );

        assertTrue(
				"Property access should be used.", tuplizer.getGetter( 0 ) instanceof BasicPropertyAccessor.BasicGetter
		);
    }

    @Test
	@FailureExpectedWithNewMetamodel
    public void testExplicitPropertyAccessAnnotationsWithJpaStyleOverride() throws Exception {
        Class<?> classUnderTest = Course5.class;
		SessionFactoryImplementor factory = getSF( classUnderTest, Student.class );
		EntityTuplizer tuplizer = factory.getEntityPersister( classUnderTest.getName() )
                .getEntityMetamodel()
                .getTuplizer();
        assertTrue(
                "Field access should be used.",
                tuplizer.getIdentifierGetter() instanceof DirectPropertyAccessor.DirectGetter
        );

        assertTrue(
				"Property access should be used.", tuplizer.getGetter( 0 ) instanceof BasicPropertyAccessor.BasicGetter
		);
    }

    @Test
    public void testDefaultFieldAccessIsInherited() throws Exception {
        Class classUnderTest = User.class;
		SessionFactoryImplementor factory =getSF( classUnderTest, Person.class, Being.class );
        EntityTuplizer tuplizer = factory.getEntityPersister( classUnderTest.getName() )
                .getEntityMetamodel()
                .getTuplizer();
        assertTrue(
				"Field access should be used since the default access mode gets inherited",
				tuplizer.getIdentifierGetter() instanceof DirectPropertyAccessor.DirectGetter
		);
    }

    @Test
	@FailureExpectedWithNewMetamodel
    public void testDefaultPropertyAccessIsInherited() throws Exception {
		SessionFactoryImplementor factory = getSF( Horse.class, Animal.class );

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
    }

	@TestForIssue(jiraKey = "HHH-5004")
	@Test
	@FailureExpectedWithNewMetamodel
	public void testAccessOnClassAndId() throws Exception {
		getSF( Course8.class, Student.class );
	}

	private SessionFactoryImplementor getSF(Class ... classes){
		getTestConfiguration().getAnnotatedClasses().addAll( Arrays.asList( classes ) );
		return getSessionFactoryHelper().getSessionFactory();
	}
}
