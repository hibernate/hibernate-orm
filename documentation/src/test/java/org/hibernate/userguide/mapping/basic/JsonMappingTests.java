/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.userguide.mapping.basic;

import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;

/**
 * @author Christian Beikov
 */
@DomainModel(annotatedClasses = JsonMappingTests.EntityWithJson.class)
@SessionFactory
public abstract class JsonMappingTests {

	@ServiceRegistry(settings = @Setting(name = AvailableSettings.JSON_FORMAT_MAPPER, value = "jsonb"))
	public static class JsonB extends JsonMappingTests {

		public JsonB() {
			super( true );
		}
	}

	@ServiceRegistry(settings = @Setting(name = AvailableSettings.JSON_FORMAT_MAPPER, value = "jackson"))
	public static class Jackson extends JsonMappingTests {

		public Jackson() {
			super( false );
		}
	}

	private final boolean supportsObjectMapKey;

	protected JsonMappingTests(boolean supportsObjectMapKey) {
		this.supportsObjectMapKey = supportsObjectMapKey;
	}

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor( EntityWithJson.class );
		final JdbcTypeRegistry jdbcTypeRegistry = mappingMetamodel.getTypeConfiguration().getJdbcTypeRegistry();

		final BasicAttributeMapping payloadAttribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "payload" );
		final BasicAttributeMapping objectMapAttribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "objectMap" );
		final BasicAttributeMapping listAttribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "list" );
		final BasicAttributeMapping jsonAttribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "jsonString" );

		assertThat( payloadAttribute.getJavaType().getJavaTypeClass(), equalTo( Map.class ) );
		assertThat( objectMapAttribute.getJavaType().getJavaTypeClass(), equalTo( Map.class ) );
		assertThat( listAttribute.getJavaType().getJavaTypeClass(), equalTo( List.class ) );
		assertThat( jsonAttribute.getJavaType().getJavaTypeClass(), equalTo( String.class ) );

		final JdbcType jsonType = jdbcTypeRegistry.getDescriptor( SqlTypes.JSON );
		assertThat( payloadAttribute.getJdbcMapping().getJdbcType(), is( jsonType ) );
		assertThat( objectMapAttribute.getJdbcMapping().getJdbcType(), is( jsonType ) );
		assertThat( listAttribute.getJdbcMapping().getJdbcType(), is( jsonType ) );
		assertThat( jsonAttribute.getJdbcMapping().getJdbcType(), is( jsonType ) );

		Map<String, String> stringMap = Map.of( "name", "ABC" );
		Map<StringNode, StringNode> objectMap = supportsObjectMapKey ? Map.of( new StringNode( "name" ), new StringNode( "ABC" ) ) : null;
		List<StringNode> list = List.of( new StringNode( "ABC" ) );
		String json = "{\"name\":\"abc\"}";
		// PostgreSQL returns the JSON slightly formatted
		String alternativeJson = "{\"name\": \"abc\"}";

		scope.inTransaction(
				(session) -> {
					session.persist( new EntityWithJson( 1, stringMap, objectMap, list, json ) );
				}
		);

		scope.inTransaction(
				(session) ->  {
					EntityWithJson entityWithJson = session.find( EntityWithJson.class, 1 );
					assertThat( entityWithJson.payload, is( stringMap ) );
					assertThat( entityWithJson.objectMap, is( objectMap ) );
					assertThat( entityWithJson.list, is( list ) );
					assertThat( entityWithJson.jsonString, isOneOf( json, alternativeJson ) );
					String nativeJson = session.createNativeQuery(
									"select jsonString from EntityWithJson",
									String.class
							)
							.getResultList()
							.get( 0 );
					assertThat( nativeJson, isOneOf( json, alternativeJson ) );
				}
		);
	}

	@Entity(name = "EntityWithJson")
	@Table(name = "EntityWithJson")
	public static class EntityWithJson {
		@Id
		private Integer id;

		//tag::basic-json-example[]
		@JdbcTypeCode( SqlTypes.JSON )
		private Map<String, String> payload;
		//end::basic-json-example[]

		@JdbcTypeCode( SqlTypes.JSON )
		private Map<StringNode, StringNode> objectMap;

		@JdbcTypeCode( SqlTypes.JSON )
		private List<StringNode> list;

		@JdbcTypeCode( SqlTypes.JSON )
		private String jsonString;

		public EntityWithJson() {
		}

		public EntityWithJson(
				Integer id,
				Map<String, String> payload,
				Map<StringNode, StringNode> objectMap,
				List<StringNode> list,
				String jsonString) {
			this.id = id;
			this.payload = payload;
			this.objectMap = objectMap;
			this.list = list;
			this.jsonString = jsonString;
		}
	}

	public static class StringNode {
		private String string;

		public StringNode() {
		}

		public StringNode(String string) {
			this.string = string;
		}

		public String getString() {
			return string;
		}

		public void setString(String string) {
			this.string = string;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			StringNode that = (StringNode) o;

			return string != null ? string.equals( that.string ) : that.string == null;
		}

		@Override
		public int hashCode() {
			return string != null ? string.hashCode() : 0;
		}
	}
}
