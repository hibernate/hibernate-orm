package org.hibernate.metamodel.internal.source.annotations.xml.mocker;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;

import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.internal.jandex.Default;
import org.hibernate.metamodel.source.internal.jandex.DefaultConfigurationHelper;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntity;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.Test;

/**
 * @author Strong Liu
 */
public class DefaultConfigurationHelperTest extends AbstractMockerTest {
	@Test
	public void applyNullDefaultToEntity() {
		JaxbEntity entity = new JaxbEntity();
		entity.setClazz( "Entity" );
		DefaultConfigurationHelper.INSTANCE.applyDefaults( entity, null );
		assertNull( entity.getTable() );
		assertEquals( "Entity", entity.getClazz() );
	}

	@Test
	public void applyDefaultToEntity() {
		Default defaults = new Default();
		defaults.setPackageName( "org.test" );
		defaults.setSchema( "schema" );
		defaults.setMetadataComplete( true );
		JaxbEntity entity = new JaxbEntity();
		entity.setClazz( "Entity" );
		DefaultConfigurationHelper.INSTANCE.applyDefaults( entity, defaults );
		assertNotNull( entity.getTable() );
		assertNull( entity.getTable().getSchema() );
		assertNull( entity.getTable().getCatalog() );
		assertTrue( entity.isMetadataComplete() );
		assertEquals( "org.test.Entity", entity.getClazz() );
		DefaultConfigurationHelper.INSTANCE.applyDefaults( entity.getTable(), defaults );
		assertEquals( "schema", entity.getTable().getSchema() );
		assertNull( entity.getTable().getCatalog() );
	}

	@Test
	public void testDefaultCascadePersist() {
		Default defaults = new Default();
		defaults.setCascadePersist( true );
		Index index = getIndex();
		Map<DotName, List<AnnotationInstance>> annotations = new HashMap<DotName, List<AnnotationInstance>>();
		annotations.putAll( index.getClassByName( DotName.createSimple( Parent.class.getName() ) ).annotations() );
		assertEquals( 4, annotations.size() );
		assertEquals( 1, annotations.get( JPADotNames.ENTITY ).size() );
		assertEquals( 1, annotations.get( JPADotNames.ID ).size() );
		assertEquals( 1, annotations.get( JPADotNames.ONE_TO_MANY ).size() );
		assertEquals( 1, annotations.get( JPADotNames.MANY_TO_ONE ).size() );

		DefaultConfigurationHelper.INSTANCE.applyDefaults( annotations, defaults );

		assertEquals( 4, annotations.size() );
		assertEquals( 1, annotations.get( JPADotNames.ENTITY ).size() );
		assertEquals( 1, annotations.get( JPADotNames.ID ).size() );
		assertEquals( 1, annotations.get( JPADotNames.ONE_TO_MANY ).size() );
		assertEquals( 1, annotations.get( JPADotNames.MANY_TO_ONE ).size() );
		AnnotationInstance oneToMany = annotations.get( JPADotNames.ONE_TO_MANY ).get( 0 );
		String[] cascadeTypes = oneToMany.value( "cascade" ).asEnumArray();
		assertArrayEquals( new String[] { "ALL", "DETACH", "PERSIST" }, cascadeTypes );
		AnnotationInstance manyToOne = annotations.get( JPADotNames.MANY_TO_ONE ).get( 0 );
		cascadeTypes = manyToOne.value( "cascade" ).asEnumArray();
		assertArrayEquals( new String[] { "PERSIST" }, cascadeTypes );

		annotations.clear();
		annotations.putAll( index.getClassByName( DotName.createSimple( Child.class.getName() ) ).annotations() );
		assertEquals( 3, annotations.size() );
		assertEquals( 1, annotations.get( JPADotNames.ENTITY ).size() );
		assertEquals( 1, annotations.get( JPADotNames.ID ).size() );
		assertEquals( 1, annotations.get( JPADotNames.MANY_TO_ONE ).size() );

		DefaultConfigurationHelper.INSTANCE.applyDefaults( annotations, defaults );

		assertEquals( 3, annotations.size() );
		assertEquals( 1, annotations.get( JPADotNames.ENTITY ).size() );
		assertEquals( 1, annotations.get( JPADotNames.ID ).size() );
		assertEquals( 1, annotations.get( JPADotNames.MANY_TO_ONE ).size() );

		manyToOne = annotations.get( JPADotNames.MANY_TO_ONE ).get( 0 );
		cascadeTypes = manyToOne.value( "cascade" ).asEnumArray();
		assertArrayEquals( new String[] { "PERSIST", "ALL", "DETACH" }, cascadeTypes );
	}

	@Test
	public void testDefaultSchemaToAnnotationInstance() {
		Default defaults = new Default();
		defaults.setSchema( "hib_schema" );
		defaults.setCatalog( "hib_catalog" );
		Index index = getIndex();
		Map<DotName, List<AnnotationInstance>> annotations = new HashMap<DotName, List<AnnotationInstance>>();
		annotations.putAll( index.getClassByName( DotName.createSimple( Parent.class.getName() ) ).annotations() );
		assertEquals( 4, annotations.size() );
		assertEquals( 1, annotations.get( JPADotNames.ENTITY ).size() );
		assertEquals( 1, annotations.get( JPADotNames.ID ).size() );
		assertEquals( 1, annotations.get( JPADotNames.ONE_TO_MANY ).size() );
		assertEquals( 1, annotations.get( JPADotNames.MANY_TO_ONE ).size() );
		DefaultConfigurationHelper.INSTANCE.applyDefaults( annotations, defaults );
		assertEquals( 5, annotations.size() );
		assertEquals( 1, annotations.get( JPADotNames.ENTITY ).size() );
		assertEquals( 1, annotations.get( JPADotNames.ID ).size() );
		assertEquals( 1, annotations.get( JPADotNames.ONE_TO_MANY ).size() );
		assertEquals( 1, annotations.get( JPADotNames.MANY_TO_ONE ).size() );
		assertEquals( 1, annotations.get( JPADotNames.TABLE ).size() );
		AnnotationInstance table = annotations.get( JPADotNames.TABLE ).get( 0 );
		assertEquals( "hib_schema", table.value( "schema" ).asString() );
		assertEquals( "hib_catalog", table.value( "catalog" ).asString() );

		annotations.clear();
		annotations.putAll( index.getClassByName( DotName.createSimple( Name.class.getName() ) ).annotations() );
		DefaultConfigurationHelper.INSTANCE.applyDefaults( annotations, defaults );
		assertEquals( 1, annotations.size() );
		assertEquals( 1, annotations.get( JPADotNames.SECONDARY_TABLES ).size() );
		AnnotationInstance[] secondaryTables = annotations.get( JPADotNames.SECONDARY_TABLES )
				.get( 0 )
				.value()
				.asNestedArray();
		assertEquals( 2, secondaryTables.length );
		AnnotationInstance secondaryTable = secondaryTables[0];
		String name = secondaryTable.value( "name" ).asString();
		if ( name.equals( "sec1" ) ) {
			assertSt1( secondaryTable );
			assertSt2( secondaryTables[1] );
		}
		else {
			assertSt1( secondaryTables[1] );
			assertSt2( secondaryTable );
		}


	}

	private void assertSt1(AnnotationInstance secondaryTable) {
		assertEquals( "hib_schema", secondaryTable.value( "schema" ).asString() );
		assertEquals( "sec1_catalog", secondaryTable.value( "catalog" ).asString() );
	}

	private void assertSt2(AnnotationInstance secondaryTable) {
		assertEquals( "sec2_schema", secondaryTable.value( "schema" ).asString() );
		assertEquals( "hib_catalog", secondaryTable.value( "catalog" ).asString() );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Parent.class, Child.class, Name.class };
	}

	@SecondaryTables( {
			@SecondaryTable(name = "sec1", catalog = "sec1_catalog"),
			@SecondaryTable(name = "sec2", schema = "sec2_schema")
	})
	class Name {
	}

	@Entity
	class Parent {
		@Id
		long id;
		@OneToMany(cascade = { CascadeType.ALL, CascadeType.DETACH, CascadeType.PERSIST })
		Set<Child> children = new HashSet<Child>();
		@ManyToOne
		Parent parent;


	}

	@Entity
	class Child {
		@Id
		long id;
		@ManyToOne(cascade = { CascadeType.ALL, CascadeType.DETACH })
		Parent parent;

	}

}
