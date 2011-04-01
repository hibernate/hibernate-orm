package org.hibernate.metamodel.source.annotations;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.Test;

import org.hibernate.AnnotationException;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
public class ConfiguredClassHierarchyBuilderTest extends BaseUnitTestCase {

	@Test
	public void testSingleEntity() {
		Index index = indexForClass( Foo.class );
		ConfiguredClassHierarchyBuilder builder = new ConfiguredClassHierarchyBuilder();
		Set<ConfiguredClassHierarchy> hierarchies = builder.createEntityHierarchies( index );
		assertEquals( "There should be only one hierarchy", 1, hierarchies.size() );

		Iterator<ConfiguredClass> iter = hierarchies.iterator().next().iterator();
		ClassInfo info = iter.next().getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( Foo.class.getName() ), info.name() );
		assertFalse( iter.hasNext() );
	}

	@Test
	public void testSimpleInheritance() {
		Index index = indexForClass( B.class, A.class );
		ConfiguredClassHierarchyBuilder builder = new ConfiguredClassHierarchyBuilder();
		Set<ConfiguredClassHierarchy> hierarchies = builder.createEntityHierarchies( index );
		assertEquals( "There should be only one hierarchy", 1, hierarchies.size() );

		Iterator<ConfiguredClass> iter = hierarchies.iterator().next().iterator();
		ClassInfo info = iter.next().getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( A.class.getName() ), info.name() );
		info = iter.next().getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( B.class.getName() ), info.name() );
		assertFalse( iter.hasNext() );
	}

	@Test
	public void testMultipleHierarchies() {
		Index index = indexForClass( B.class, A.class, Foo.class );
		ConfiguredClassHierarchyBuilder builder = new ConfiguredClassHierarchyBuilder();
		Set<ConfiguredClassHierarchy> hierarchies = builder.createEntityHierarchies( index );
		assertEquals( "There should be only one hierarchy", 2, hierarchies.size() );
	}

	@Test
	public void testMappedSuperClass() {
		Index index = indexForClass( MappedSubClass.class, MappedSuperClass.class, UnmappedSubClass.class );
		ConfiguredClassHierarchyBuilder builder = new ConfiguredClassHierarchyBuilder();
		Set<ConfiguredClassHierarchy> hierarchies = builder.createEntityHierarchies( index );
		assertEquals( "There should be only one hierarchy", 1, hierarchies.size() );

		Iterator<ConfiguredClass> iter = hierarchies.iterator().next().iterator();
		ClassInfo info = iter.next().getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( MappedSuperClass.class.getName() ), info.name() );
		info = iter.next().getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( UnmappedSubClass.class.getName() ), info.name() );
		assertFalse( iter.hasNext() );
		info = iter.next().getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( MappedSubClass.class.getName() ), info.name() );
		assertFalse( iter.hasNext() );
	}

	@Test(expected = AnnotationException.class)
	public void testEntityAndMappedSuperClassAnnotations() {
		Index index = indexForClass( EntityAndMappedSuperClass.class );
		ConfiguredClassHierarchyBuilder builder = new ConfiguredClassHierarchyBuilder();
		builder.createEntityHierarchies( index );
	}

	private Index indexForClass(Class<?>... classes) {
		Indexer indexer = new Indexer();
		for ( Class<?> clazz : classes ) {
			InputStream stream = getClass().getClassLoader().getResourceAsStream(
					clazz.getName().replace( '.', '/' ) + ".class"
			);
			try {
				indexer.index( stream );
			}
			catch ( IOException e ) {
				fail( "Unable to index" );
			}
		}
		return indexer.complete();
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

	@MappedSuperclass
	public class MappedSuperClass {
		@Id
		@GeneratedValue
		private int id;
	}

	public class UnmappedSubClass extends MappedSuperClass {
		private String unmappedProperty;
	}

	@Entity
	public class MappedSubClass extends UnmappedSubClass {
		private String mappedProperty;
	}

	@Entity
	@MappedSuperclass
	public class EntityAndMappedSuperClass {
	}
}


