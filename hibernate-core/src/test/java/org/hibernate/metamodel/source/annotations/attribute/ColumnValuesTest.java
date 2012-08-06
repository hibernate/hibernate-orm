package org.hibernate.metamodel.source.annotations.attribute;

import static junit.framework.Assert.assertTrue;

import javax.persistence.Column;

import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.JandexHelper;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.internal.StandardServiceRegistryImpl;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for attributes in Column annotation
 *
 * @author Anderson Parra
 */
public class ColumnValuesTest {

	private StandardServiceRegistryImpl serviceRegistry;
	private ClassLoaderService classLoaderService;

	@Before
	public void setUp() {
		serviceRegistry = (StandardServiceRegistryImpl) new ServiceRegistryBuilder().buildServiceRegistry();
		classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
	}

	@After
	public void tearDown() {
		serviceRegistry.destroy();
	}
	
	@Test
	public void shoudUniquePresent() {
		class Foo {
			@Column(name="BAR", unique=true)
			private String bar;
		}

		Index index = JandexHelper.indexForClass(classLoaderService, Foo.class);
		ClassInfo classInfo = index.getClassByName(DotName.createSimple(Foo.class.getName()));

		ColumnValues columnValues = new ColumnValues(JandexHelper.getSingleAnnotation(classInfo, JPADotNames.COLUMN));
		assertTrue(columnValues.isUnique());
	}
}
