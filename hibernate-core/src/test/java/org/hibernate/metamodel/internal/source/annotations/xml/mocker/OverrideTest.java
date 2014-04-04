/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc..
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
package org.hibernate.metamodel.internal.source.annotations.xml.mocker;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.internal.jandex.Default;
import org.hibernate.metamodel.source.internal.jandex.EntityMocker;
import org.hibernate.metamodel.source.internal.jandex.IndexBuilder;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntity;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.Test;

/**
 * @author Strong Liu
 */
public class OverrideTest extends AbstractMockerTest {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Author.class,
				Book.class,
				Topic.class
		};
	}

	@Test
	public void testPersistenceUnitMetadataMetadataComplete() {
		JaxbEntity author = new JaxbEntity();
		author.setClazz( Author.class.getName() );
		IndexBuilder indexBuilder = getIndexBuilder();
		Default defaults = new Default();
		defaults.setMetadataComplete( true );
		EntityMocker entityMocker = new EntityMocker( indexBuilder, author, defaults );
		entityMocker.preProcess();
		entityMocker.process();
		Index index = indexBuilder.build( new Default() );
		DotName className = DotName.createSimple( Author.class.getName() );
		ClassInfo classInfo = index.getClassByName( className );
		assertEquals( 1, classInfo.annotations().size() );
		assertHasAnnotation( index, className, JPADotNames.ENTITY );
	}

	@Test
	public void testEntityMetadataComplete() {
		Index index = getMockedIndex( "entity-metadata-complete.xml" );
		DotName authorName = DotName.createSimple( Author.class.getName() );
		ClassInfo authorClassInfo = index.getClassByName( authorName );
		assertHasAnnotation( index, authorName, JPADotNames.ENTITY );
		assertHasAnnotation( index, authorName, JPADotNames.ID_CLASS );
		assertEquals( 2, authorClassInfo.annotations().size() );
		DotName bookName = DotName.createSimple( Book.class.getName() );
		assertHasAnnotation( index, bookName, JPADotNames.ENTITY );
	}

	@Test
	public void testOverrideToMappedSuperClass() {
		Index index = getMockedIndex( "override-to-mappedsuperclass.xml" );
		index.printAnnotations();
		DotName authorName = DotName.createSimple( Author.class.getName() );
		assertHasAnnotation( index, authorName, JPADotNames.ENTITY );
		assertHasNoAnnotation( index, authorName, JPADotNames.TABLE );
		DotName bookName = DotName.createSimple( Book.class.getName() );
		assertHasAnnotation( index, bookName, JPADotNames.MAPPED_SUPERCLASS );
		assertHasNoAnnotation( index, bookName, JPADotNames.TABLE );

	}

	@Test
	public void testPersistenceUnitDefaultsCascadePersistInAnnotation() {
		JaxbEntity author = new JaxbEntity();
		author.setClazz( Author.class.getName() );
		IndexBuilder indexBuilder = getIndexBuilder();
		Default defaults = new Default();
		defaults.setCascadePersist( true );
		EntityMocker entityMocker = new EntityMocker( indexBuilder, author, defaults );
		entityMocker.preProcess();
		entityMocker.process();
		Index index = indexBuilder.build( new Default() );
		DotName className = DotName.createSimple( Author.class.getName() );
		assertAnnotationValue(
				index, className, JPADotNames.ONE_TO_MANY, new CascadeAnnotationValueChecker( "PERSIST", "MERGE" )
		);
	}


	@Test
	public void testPersistenceUnitDefaultsCascadePersistInXML() {
		Index index = getMockedIndex( "AttributeOverride.xml" );
		DotName className = DotName.createSimple( Author.class.getName() );
		assertAnnotationValue(
				index,
				className,
				JPADotNames.ONE_TO_MANY,
				new CascadeAnnotationValueChecker( new String[] { "PERSIST", "ALL" } )
		);
	}

	protected class CascadeAnnotationValueChecker implements AnnotationValueChecker {
		private String[] expected = new String[0];

		public CascadeAnnotationValueChecker(String... expected) {
			this.expected = expected;
		}

		@Override
		public void check(AnnotationInstance annotationInstance) {
			AnnotationValue cascadeValue = annotationInstance.value( "cascade" );
			assertNotNull(
					"Cascade is null in @OneToMany, but should be added a Cascade persist", cascadeValue
			);
			String[] enumArray = cascadeValue.asEnumArray();
			assertEquals( expected.length, enumArray.length );
			assertArrayEquals( expected, enumArray );
		}
	}

	/**
	 * Entity has a @AttributeOverride on property topic
	 * and this property also has a <attribute-override> in orm.xml but with different name
	 * by jpa override rules, this two attribute-override should be merged into one @AttributeOverrides
	 */
	@Test
	public void testAttributeOverride() {
		Index index = getMockedIndex( "AttributeOverride.xml" );
		DotName className = DotName.createSimple( Book.class.getName() );
		index.printAnnotations();
		assertHasNoAnnotation(
				index,
				className,
				JPADotNames.ATTRIBUTE_OVERRIDE
		);
		assertAnnotationValue(
				index,
				className,
				JPADotNames.ATTRIBUTE_OVERRIDES, new AnnotationValueChecker() {
					@Override
					public void check(AnnotationInstance annotationInstance) {
						AnnotationValue value = annotationInstance.value();
						assertNotNull( value );
						AnnotationInstance[] annotationInstances = value.asNestedArray();
						assertEquals( 2, annotationInstances.length );
						AnnotationInstance ai = annotationInstances[0];
						String name = ai.value( "name" ).asString();
						AnnotationValue columnValue = ai.value( "column" ).asNested().value( "name" );
						if ( name.equals( "title" ) ) {
							assertEquals( "TOC_TITLE", columnValue.asString() );

						}
						else if ( name.equals( "summary" ) ) {
							assertEquals( "TOPIC_SUMMARY", columnValue.asString() );
						}
						else {
							fail( "AttributeOverride's name is " + name + ", should be either 'title' or 'summary'" );
						}
					}
				}
		);
	}

	@Test
	public void testSchemaInPersistenceMetadata() {
		Index index = getMockedIndex( "default-schema.xml" );
		index.printAnnotations();
		//Global Configuration should be accessed like this, not from ClassInfo
		List<AnnotationInstance> annotationInstanceList = index.getAnnotations( JPADotNames.TABLE_GENERATOR );
		assertNotNull( annotationInstanceList );
		assertEquals( 1, annotationInstanceList.size() );
		AnnotationInstance generator = annotationInstanceList.get( 0 );
		assertEquals( "TABLE_GEN", generator.value( "name" ).asString() );
		assertEquals( "ANNOTATION_CATALOG", generator.value( "catalog" ).asString() );
		assertEquals( "ANNOTATION_SCHEMA", generator.value( "schema" ).asString() );

		annotationInstanceList = index.getAnnotations( JPADotNames.SEQUENCE_GENERATOR );
		assertNotNull( annotationInstanceList );
		assertEquals( 1, annotationInstanceList.size() );
		generator = annotationInstanceList.get( 0 );
		assertEquals( "SEQ_GEN", generator.value( "name" ).asString() );
		assertEquals( "XML_CATALOG", generator.value( "catalog" ).asString() );
		assertEquals( "XML_SCHEMA", generator.value( "schema" ).asString() );
		assertEquals( 123, generator.value( "initialValue" ).asInt() );
		//Book and Author and Topic are all not defined @Table
		//but orm xml defines default schema and catalog in persistence-unit-metadata
		//so, we have to mock @Table for entities, Book and Author but not Topic which is a Embeddable
		annotationInstanceList = index.getAnnotations( JPADotNames.TABLE );
		assertNotNull( annotationInstanceList );
		assertEquals( 2, annotationInstanceList.size() );
		for ( AnnotationInstance table : annotationInstanceList ) {
			assertEquals( "XML_CATALOG", table.value( "catalog" ).asString() );
			assertEquals( "XML_SCHEMA", table.value( "schema" ).asString() );
		}

	}

	@Test
	public void testSchemaInEntityMapping() {
		Index index = getMockedIndex( "default-schema2.xml" );
		index.printAnnotations();
		//Global Configuration should be accessed like this, not from ClassInfo
		List<AnnotationInstance> annotationInstanceList = index.getAnnotations( JPADotNames.TABLE_GENERATOR );
		assertNotNull( annotationInstanceList );
		assertEquals( 1, annotationInstanceList.size() );
		AnnotationInstance generator = annotationInstanceList.get( 0 );
		assertEquals( "TABLE_GEN", generator.value( "name" ).asString() );
		assertEquals( "ANNOTATION_CATALOG", generator.value( "catalog" ).asString() );
		assertEquals( "ANNOTATION_SCHEMA", generator.value( "schema" ).asString() );

		annotationInstanceList = index.getAnnotations( JPADotNames.SEQUENCE_GENERATOR );
		assertNotNull( annotationInstanceList );
		assertEquals( 1, annotationInstanceList.size() );
		generator = annotationInstanceList.get( 0 );
		assertEquals( "SEQ_GEN", generator.value( "name" ).asString() );
		assertNull( generator.value( "catalog" ) );
		assertNull( generator.value( "schema" ) );
		assertEquals( 123, generator.value( "initialValue" ).asInt() );

		annotationInstanceList = index.getAnnotations( JPADotNames.TABLE );
		assertNotNull( annotationInstanceList );
		assertEquals( 0, annotationInstanceList.size() );

	}
}
