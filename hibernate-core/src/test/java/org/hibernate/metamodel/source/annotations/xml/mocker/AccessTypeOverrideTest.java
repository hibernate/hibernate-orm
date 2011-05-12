package org.hibernate.metamodel.source.annotations.xml.mocker;

import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;
import org.junit.Test;

import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.util.JandexHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Strong Liu
 */
public class AccessTypeOverrideTest extends AbstractMockerTest {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Department.class,
				Address.class
		};
	}

	@Test
	public void testDefaultAccessType() {
		Index index = getMockedIndex( "access2.xml" );
		DotName departmentName = DotName.createSimple( Department.class.getName() );
		ClassInfo department = index.getClassByName( departmentName );
		AnnotationInstance annotationInstance = JandexHelper.getSingleAnnotation( department, JPADotNames.ID );
		assertAnnotationTarget( FieldInfo.class, annotationInstance );

		annotationInstance = JandexHelper.getSingleAnnotation( department, JPADotNames.BASIC );
		assertAnnotationTarget( MethodInfo.class, annotationInstance );
		annotationInstance = JandexHelper.getSingleAnnotation( department, JPADotNames.COLUMN );
		assertAnnotationTarget( MethodInfo.class, annotationInstance );
		//there are two @ManyToOne, one is from annotation, the other is from xml and with @Access(PROPERTY)
		assertHasAnnotation( index, departmentName, JPADotNames.MANY_TO_ONE, 2 );
		AnnotationTarget target = MockHelper.getTarget(
				getServiceRegistry(), department, "employees", MockHelper.TargetType.PROPERTY
		);
		assertHasAnnotationOnTarget( target, JPADotNames.ACCESS, department );

		DotName addressName = DotName.createSimple( Address.class.getName() );
		ClassInfo address = index.getClassByName( addressName );
		assertHasAnnotationOnTarget( address, JPADotNames.ACCESS, address );
		target = MockHelper.getTarget(
				getServiceRegistry(), address, "city", MockHelper.TargetType.FIELD
		);
		assertHasAnnotationOnTarget( target, JPADotNames.ACCESS, address );
		AnnotationInstance city = JandexHelper.getSingleAnnotation( address, JPADotNames.BASIC );
		assertAnnotationTarget( FieldInfo.class,city );

	}

	@Test
	public void testBasicOverride() {
		Index index = getMockedIndex( "access.xml" );
		DotName departmentName = DotName.createSimple( Department.class.getName() );
		ClassInfo department = index.getClassByName( departmentName );
		AnnotationInstance annotationInstance = JandexHelper.getSingleAnnotation( department, JPADotNames.ID );
		assertAnnotationTarget( FieldInfo.class, annotationInstance );

		annotationInstance = JandexHelper.getSingleAnnotation( department, JPADotNames.BASIC );
		assertAnnotationTarget( MethodInfo.class, annotationInstance );
		annotationInstance = JandexHelper.getSingleAnnotation( department, JPADotNames.COLUMN );
		assertAnnotationTarget( MethodInfo.class, annotationInstance );
		//there are two @ManyToOne, one is from annotation, the other is from xml and with @Access(PROPERTY)
		assertHasAnnotation( index, departmentName, JPADotNames.MANY_TO_ONE, 2 );
		AnnotationTarget target = MockHelper.getTarget(
				getServiceRegistry(), department, "employees", MockHelper.TargetType.PROPERTY
		);
		assertHasAnnotationOnTarget( target, JPADotNames.ACCESS, department );

	}

	protected void assertHasAnnotationOnTarget(AnnotationTarget target, DotName annName, ClassInfo classInfo) {
		assertTrue(
				"ClassInfo[" + classInfo.name() + "] doesn't have annotation [" + annName + "]",
				classInfo.annotations().containsKey( annName )
		);
		List<AnnotationInstance> annotationInstanceList = classInfo.annotations().get( annName );
		boolean found = false;
		for ( AnnotationInstance annotationInstance : annotationInstanceList ) {
			if ( MockHelper.targetEquals( target, annotationInstance.target() ) ) {
				found = true;
				break;
			}
		}
		assertTrue( "No annotation[" + annName + "] is found on target[" + target + "]", found );

	}

	protected void assertAnnotationTarget(Class expectedTargetClass, AnnotationInstance annotationInstance) {
		if ( expectedTargetClass == null ) {
			assertNull( annotationInstance.target() );
		}
		else if ( annotationInstance.target() == null ) {
			fail( "expect target is " + expectedTargetClass.getSimpleName() + "but the target is null" );
		}
		else {
			assertEquals( expectedTargetClass, annotationInstance.target().getClass() );
		}
	}
}
