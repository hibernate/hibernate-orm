/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.language;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.tool.language.domain.Address;
import org.hibernate.tool.language.domain.Company;
import org.hibernate.tool.language.domain.Employee;
import org.hibernate.tool.language.internal.MetamodelJsonSerializerImpl;

import org.hibernate.testing.orm.domain.animal.AnimalDomainModel;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.persistence.AccessType;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.Metamodel;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

public class MetamodelJsonSerializerTest {
	private static final ObjectMapper mapper = new ObjectMapper();

	static {
		mapper.enable( SerializationFeature.INDENT_OUTPUT );
	}

	private static final boolean DEBUG = false; // set to true to enable debug output of the generated JSON

	@Test
	public void testSimpleDomainModel() {
		final Metadata metadata = new MetadataSources().addAnnotatedClass( Address.class )
				.addAnnotatedClass( Company.class )
				.addAnnotatedClass( Employee.class )
				.buildMetadata();
		try (final SessionFactory sf = metadata.buildSessionFactory()) {
			try {
				final JsonNode root = toJson( sf.getMetamodel() );

				// Entities

				final JsonNode entities = root.get( "entities" );
				assertThat( entities.isArray() ).isTrue();
				assertThat( entities.size() ).isEqualTo( 2 );

				JsonNode companyNode = findByName( entities, Company.class.getSimpleName() );
				assertThat( companyNode ).isNotNull();
				assertThat( companyNode.get( "class" ).asText() ).isEqualTo( Company.class.getName() );
				assertThat( companyNode.get( "identifierAttribute" ).asText() ).isEqualTo( "id" );
				assertAttributes( Company.class, companyNode.get( "attributes" ), AccessType.FIELD );

				JsonNode employeeNode = findByName( entities, Employee.class.getSimpleName() );
				assertThat( employeeNode ).isNotNull();
				assertThat( employeeNode.get( "class" ).asText() ).isEqualTo( Employee.class.getName() );
				assertAttributes( Employee.class, employeeNode.get( "attributes" ), AccessType.PROPERTY );

				// Embeddables

				final JsonNode embeddables = root.get( "embeddables" );
				assertThat( embeddables.isArray() ).isTrue();
				assertThat( embeddables.size() ).isEqualTo( 1 );

				JsonNode addressNode = findByName( embeddables, Address.class.getSimpleName() );
				assertThat( addressNode ).isNotNull();
				assertAttributes( Address.class, addressNode.get( "attributes" ), AccessType.FIELD );

				// Mapped superclasses

				final JsonNode superclasses = root.get( "mappedSuperclasses" );
				assertThat( superclasses.isArray() ).isTrue();
				assertThat( superclasses.isEmpty() ).isTrue();
			}
			catch (JsonProcessingException e) {
				fail( "Encountered an exception during JSON processing", e );
			}
		}
	}

	@Test
	public void testMappedSuperclasses() {
		// We need entities that extend mapped-superclasses, otherwise they will just be ignored
		final Metadata metadata = new MetadataSources().addAnnotatedClass( MappedSuperWithEmbeddedId.class )
				.addAnnotatedClass( Entity1.class )
				.addAnnotatedClass( MappedSuperWithoutId.class )
				.addAnnotatedClass( Entity2.class )
				.buildMetadata();
		try (final SessionFactory sf = metadata.buildSessionFactory()) {
			try {
				final JsonNode root = toJson( sf.getMetamodel() );

				final JsonNode superclasses = root.get( "mappedSuperclasses" );
				assertThat( superclasses.isArray() ).isTrue();
				assertThat( superclasses.size() ).isEqualTo( 2 );

				JsonNode withId = findByName( superclasses, MappedSuperWithEmbeddedId.class.getSimpleName() );
				assertThat( withId ).isNotNull();
				assertThat( withId.get( "class" ).asText() ).isEqualTo( MappedSuperWithEmbeddedId.class.getName() );
				assertThat( withId.get( "identifierAttribute" ).asText() ).isEqualTo( "embeddedId" );
				assertAttributes( MappedSuperWithEmbeddedId.class, withId.get( "attributes" ), AccessType.FIELD );

				JsonNode withoutId = findByName( superclasses, MappedSuperWithoutId.class.getSimpleName() );
				assertThat( withoutId ).isNotNull();
				assertThat( withoutId.get( "class" ).asText() ).isEqualTo( MappedSuperWithoutId.class.getName() );
				assertThat( withoutId.has( "identifierAttribute" ) ).isFalse();

				// double check entities.superClass contains the mapped superclasses
				assertThat( root.get( "entities" )
									.findValues( "superType" )
									.stream()
									.map( JsonNode::asText ) ).containsOnly(
						MappedSuperWithEmbeddedId.class.getTypeName(),
						MappedSuperWithoutId.class.getTypeName()
				);
			}
			catch (JsonProcessingException e) {
				fail( "Encountered an exception during JSON processing", e );
			}
		}
	}

	@Test
	public void testStandardDomainModelInheritance() {
		final Class<?>[] annotatedClasses = AnimalDomainModel.INSTANCE.getAnnotatedClasses();
		final Metadata metadata = new MetadataSources().addAnnotatedClasses( annotatedClasses ).buildMetadata();
		try (final SessionFactory sf = metadata.buildSessionFactory()) {
			try {
				final Metamodel metamodel = sf.getMetamodel();
				final JsonNode root = toJson( metamodel );

				final Set<EntityType<?>> metamodelEntities = metamodel.getEntities();

				final JsonNode entities = root.get( "entities" );
				assertThat( entities.isArray() ).isTrue();
				assertThat( entities.size() ).isEqualTo( metamodelEntities.size() );

				for ( EntityType<?> entity : metamodelEntities ) {
					final String name = entity.getName();
					final JsonNode entityNode = findByName( entities, name );
					assertThat( entityNode ).isNotNull();
					assertThat( entityNode.get( "class" ).asText() ).isEqualTo( entity.getJavaType().getTypeName() );
					assertThat(
							entityNode.get( "identifierAttribute" ).asText()
					).isEqualTo( entity.getId( entity.getIdType().getJavaType() ).getName() );

					final IdentifiableType<?> superType = entity.getSupertype();
					if ( superType != null ) {
						assertThat( entityNode.get( "superType" ).asText() )
								.isEqualTo( superType.getJavaType().getTypeName() );
					}
					else {
						assertThat( entityNode.has( "superType" ) ).isFalse();
					}

					assertAttributes( entity.getJavaType(), entityNode.get( "attributes" ), AccessType.PROPERTY );
				}
			}
			catch (JsonProcessingException e) {
				fail( "Encountered an exception during JSON processing", e );
			}
		}
	}

	private static JsonNode toJson(Metamodel metamodel) throws JsonProcessingException {
		final String result = MetamodelJsonSerializerImpl.INSTANCE.toString( metamodel );
		final JsonNode jsonNode;
		try {
			jsonNode = mapper.readTree( result );
			if ( DEBUG ) {
				System.out.println( mapper.writeValueAsString( jsonNode ) );
			}
			return jsonNode;
		}
		catch (JsonProcessingException e) {
			if ( DEBUG ) {
				System.out.println( result );
			}
			throw e;
		}
	}

	// Helper to find node by name in a JSON array node
	private static JsonNode findByName(JsonNode array, String name) {
		assertThat( array.isArray() ).isTrue();
		for ( JsonNode n : array ) {
			if ( n.get( "name" ).asText().equals( name ) ) {
				return n;
			}
		}
		return null;
	}

	// Helper to check attributes
	static void assertAttributes(Class<?> clazz, JsonNode attributesNode, AccessType accessType) {
		final Set<String> jsonAttrs = attributesNode.findValues( "name" ).stream().map( JsonNode::asText ).collect(
				Collectors.toSet() );
		for ( MemberInfo member : getPersistentMembers( clazz, accessType ) ) {
			final String attrName = member.name();
			assertThat( jsonAttrs ).contains( attrName );
			final JsonNode attrNode = findByName( attributesNode, attrName );
			assertThat( attrNode ).isNotNull();
			assertType( attrNode.get( "type" ).asText(), member.type() );
		}
	}

	static void assertType(String actual, Class<?> expected) {
		// some types are implicitly converted when mapping to the database
		if ( expected == java.util.Date.class ) {
			expected = java.sql.Date.class;
		}

		// using startsWith as plural attributes also contain the element name in brackets
		assertThat( actual ).startsWith( expected.getTypeName() );
	}

	// Very simple helper to derive persistent members from a clazz (good enough but not be feature-complete)
	static MemberInfo[] getPersistentMembers(Class<?> clazz, AccessType accessType) {
		if ( accessType == AccessType.FIELD ) {
			return Arrays.stream( clazz.getDeclaredFields() )
					.filter( field -> !Modifier.isStatic( field.getModifiers() ) )
					.map( field -> {
						final String name = field.getName();
						return new MemberInfo( name, field.getType() );
					} )
					.toArray( MemberInfo[]::new );
		}
		else {
			return Arrays.stream( clazz.getDeclaredMethods() )
					.filter( method -> !Modifier.isStatic( method.getModifiers() ) )
					.filter( method -> method.getParameterCount() == 0 )
					.filter( method -> method.getName().startsWith( "get" ) || method.getName().startsWith( "is" ) )
					.map( method -> {
						final String name = method.getName();
						// Convert "getFoo" or "isFoo" to "foo"
						final String fieldName = name.startsWith( "get" ) ?
								name.substring( 3 ) :
								name.substring( 2 );
						return new MemberInfo( getJavaBeansFieldName( fieldName ), method.getReturnType() );
					} )
					.toArray( MemberInfo[]::new );
		}
	}

	record MemberInfo(String name, Class<?> type) {
	}

	static String getJavaBeansFieldName(String name) {
		if ( name.length() > 1 && Character.isUpperCase( name.charAt( 1 ) ) && Character.isUpperCase( name.charAt( 0 ) ) ) {
			return name;
		}
		final char[] chars = name.toCharArray();
		chars[0] = Character.toLowerCase( chars[0] );
		return new String( chars );
	}

	@MappedSuperclass
	static class MappedSuperWithEmbeddedId {
		@EmbeddedId
		private Address embeddedId;
	}

	@Entity
	static class Entity1 extends MappedSuperWithEmbeddedId {
	}

	@MappedSuperclass
	static class MappedSuperWithoutId {
		private String createdBy;

		private LocalDateTime createdAt;
	}

	@Entity
	static class Entity2 extends MappedSuperWithoutId {
		@Id
		private Long id;
	}
}
