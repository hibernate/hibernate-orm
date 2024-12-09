/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.basic;

import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.Clob;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.json.JsonValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Christian Beikov
 * @author Yanming Zhou
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

	private final Map<String, String> stringMap;
	private final Map<StringNode, StringNode> objectMap;
	private final List<StringNode> list;
	private final String json;

	protected JsonMappingTests(boolean supportsObjectMapKey) {
		this.stringMap = Map.of( "name", "ABC" );
		this.objectMap = supportsObjectMapKey ? Map.of(
				new StringNode( "name" ),
				new StringNode( "ABC" )
		) : null;
		this.list = List.of( new StringNode( "ABC" ) );
		this.json = "{\"name\":\"abc\"}";
	}

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					session.persist( new EntityWithJson( 1, stringMap, objectMap, list, json ) );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					session.remove( session.find( EntityWithJson.class, 1 ) );
				}
		);
	}

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor( EntityWithJson.class );
		final JdbcTypeRegistry jdbcTypeRegistry = mappingMetamodel.getTypeConfiguration().getJdbcTypeRegistry();

		final BasicAttributeMapping stringMapAttribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping(
				"stringMap" );
		final BasicAttributeMapping objectMapAttribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping(
				"objectMap" );
		final BasicAttributeMapping listAttribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping(
				"list" );
		final BasicAttributeMapping jsonAttribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping(
				"jsonString" );

		assertThat( stringMapAttribute.getJavaType().getJavaTypeClass(), equalTo( Map.class ) );
		assertThat( objectMapAttribute.getJavaType().getJavaTypeClass(), equalTo( Map.class ) );
		assertThat( listAttribute.getJavaType().getJavaTypeClass(), equalTo( List.class ) );
		assertThat( jsonAttribute.getJavaType().getJavaTypeClass(), equalTo( String.class ) );

		final JdbcType jsonType = jdbcTypeRegistry.getDescriptor( SqlTypes.JSON );
		assertThat( stringMapAttribute.getJdbcMapping().getJdbcType(), isA( (Class<JdbcType>) jsonType.getClass() ) );
		assertThat( objectMapAttribute.getJdbcMapping().getJdbcType(), isA( (Class<JdbcType>) jsonType.getClass() ) );
		assertThat( listAttribute.getJdbcMapping().getJdbcType(), isA( (Class<JdbcType>) jsonType.getClass() ) );
		assertThat( jsonAttribute.getJdbcMapping().getJdbcType(), isA( (Class<JdbcType>) jsonType.getClass() ) );
	}

	@Test
	public void verifyReadWorks(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					EntityWithJson entityWithJson = session.find( EntityWithJson.class, 1 );
					assertThat( entityWithJson.stringMap, is( stringMap ) );
					assertThat( entityWithJson.objectMap, is( objectMap ) );
					assertThat( entityWithJson.list, is( list ) );
					assertThat( entityWithJson.jsonNode, is( nullValue() ) );
					assertThat( entityWithJson.jsonValue, is( nullValue() ) );
				}
		);
	}

	@Test
	public void verifyMergeWorks(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					session.merge( new EntityWithJson( 2, null, null, null, null ) );
				}
		);

		scope.inTransaction(
				(session) -> {
					EntityWithJson entityWithJson = session.find( EntityWithJson.class, 2 );
					assertThat( entityWithJson.stringMap, is( nullValue() ) );
					assertThat( entityWithJson.objectMap, is( nullValue() ) );
					assertThat( entityWithJson.list, is( nullValue() ) );
					assertThat( entityWithJson.jsonString, is( nullValue() ) );
					assertThat( entityWithJson.jsonNode, is( nullValue() ) );
					assertThat( entityWithJson.jsonValue, is( nullValue() ) );
				}
		);
	}

	@Test
	@JiraKey("HHH-16682")
	public void verifyDirtyChecking(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					EntityWithJson entityWithJson = session.find( EntityWithJson.class, 1 );
					entityWithJson.stringMap.clear();
				}
		);
		scope.inTransaction(
				(session) -> {
					EntityWithJson entityWithJson = session.find( EntityWithJson.class, 1 );
					assertThat( entityWithJson.stringMap.isEmpty(), is( true ) );
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = DerbyDialect.class, reason = "Derby doesn't support comparing CLOBs with the = operator")
	@SkipForDialect(dialectClass = AbstractHANADialect.class, matchSubTypes = true, reason = "HANA doesn't support comparing LOBs with the = operator")
	@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true, reason = "Sybase doesn't support comparing LOBs with the = operator")
	@SkipForDialect(dialectClass = OracleDialect.class, matchSubTypes = true, reason = "Oracle doesn't support comparing JSON with the = operator")
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Altibase doesn't support comparing CLOBs with the = operator")
	public void verifyComparisonWorks(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					// PostgreSQL returns the JSON slightly formatted
					String alternativeJson = "{\"name\": \"abc\"}";
					EntityWithJson entityWithJson = session.createQuery(
									"from EntityWithJson e where e.stringMap = :param",
									EntityWithJson.class
							)
							.setParameter( "param", stringMap )
							.getSingleResult();
					assertThat( entityWithJson, notNullValue() );
					assertThat( entityWithJson.stringMap, is( stringMap ) );
					assertThat( entityWithJson.objectMap, is( objectMap ) );
					assertThat( entityWithJson.list, is( list ) );
					assertThat( entityWithJson.jsonString, isOneOf( json, alternativeJson ) );
					Object nativeJson = session.createNativeQuery(
									"select jsonString from EntityWithJson",
									Object.class
							)
							.getResultList()
							.get( 0 );
					final String jsonText;
					try {
						if ( nativeJson instanceof Blob ) {
							final Blob blob = (Blob) nativeJson;
							jsonText = new String(
									blob.getBytes( 1L, (int) blob.length() ),
									StandardCharsets.UTF_8
							);
						}
						else if ( nativeJson instanceof Clob ) {
							final Clob jsonClob = (Clob) nativeJson;
							jsonText = jsonClob.getSubString( 1L, (int) jsonClob.length() );
						}
						else {
							jsonText = (String) nativeJson;
						}
					}
					catch (Exception e) {
						throw new RuntimeException( e );
					}
					assertThat( jsonText, isOneOf( json, alternativeJson ) );
				}
		);
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-18709")
	public void verifyCriteriaUpdateQueryWorks(SessionFactoryScope scope) {
		final Map<String, String> newMap = Map.of( "name", "ABC" );
		final List<StringNode> newList = List.of( new StringNode( "ABC" ) );
		final String newJson = "{\"count\":123}";
		scope.inTransaction( session -> {
			final HibernateCriteriaBuilder builder = session.getCriteriaBuilder();
			final CriteriaUpdate<EntityWithJson> criteria = builder.createCriteriaUpdate( EntityWithJson.class );
			final Root<EntityWithJson> root = criteria.from( EntityWithJson.class );
			criteria.set( root.get( "stringMap" ), newMap );
			criteria.set( "list", newList );
			criteria.set( root.get( "jsonString" ), newJson );
			criteria.where( builder.equal( root.get( "id" ), 1 ) );
			final MutationQuery query = session.createMutationQuery( criteria );
			final int count = query.executeUpdate();
			assertThat( count, is( 1 ) );
		} );
		scope.inSession( session -> {
			final EntityWithJson entityWithJson = session.find( EntityWithJson.class, 1 );
			assertThat( entityWithJson.stringMap, is( newMap ) );
			assertThat( entityWithJson.list, is( newList ) );
			assertThat( entityWithJson.jsonString.replaceAll( "\\s", "" ), is( newJson ) );
		} );
	}

	@Entity(name = "EntityWithJson")
	@Table(name = "EntityWithJson")
	public static class EntityWithJson {
		@Id
		private Integer id;

		//tag::basic-json-example[]
		@JdbcTypeCode( SqlTypes.JSON )
		private Map<String, String> stringMap;
		//end::basic-json-example[]

		@JdbcTypeCode( SqlTypes.JSON )
		private Map<StringNode, StringNode> objectMap;

		@JdbcTypeCode( SqlTypes.JSON )
		private List<StringNode> list;

		@JdbcTypeCode( SqlTypes.JSON )
		private String jsonString;

		@JdbcTypeCode( SqlTypes.JSON )
		private JsonNode jsonNode;

		@JdbcTypeCode( SqlTypes.JSON )
		private JsonValue jsonValue;

		public EntityWithJson() {
		}

		public EntityWithJson(
				Integer id,
				Map<String, String> stringMap,
				Map<StringNode, StringNode> objectMap,
				List<StringNode> list,
				String jsonString) {
			this.id = id;
			this.stringMap = stringMap;
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
