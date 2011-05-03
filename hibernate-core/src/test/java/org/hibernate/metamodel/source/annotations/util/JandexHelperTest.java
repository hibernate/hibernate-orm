package org.hibernate.metamodel.source.annotations.util;

import java.util.List;
import java.util.Map;
import javax.persistence.Basic;
import javax.persistence.Column;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
}


