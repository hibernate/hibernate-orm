/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SybaseDialect;
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
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Christian Beikov
 */
@DomainModel(annotatedClasses = XmlMappingTests.EntityWithXml.class)
@SessionFactory
public abstract class XmlMappingTests {

	@ServiceRegistry(settings = @Setting(name = AvailableSettings.XML_FORMAT_MAPPER, value = "jaxb"))
	public static class Jaxb extends XmlMappingTests {

		public Jaxb() {
			super( true );
		}
	}

	@ServiceRegistry(settings = @Setting(name = AvailableSettings.XML_FORMAT_MAPPER, value = "jackson-xml"))
	public static class Jackson extends XmlMappingTests {

		public Jackson() {
			super( false );
		}
	}


	private final Map<String, StringNode> stringMap;
	private final Map<StringNode, StringNode> objectMap;
	private final List<StringNode> list;

	protected XmlMappingTests(boolean supportsObjectMapKey) {
		this.stringMap = Map.of( "name", new StringNode( "ABC" ) );
		this.objectMap = supportsObjectMapKey ? Map.of(
				new StringNode( "name" ),
				new StringNode( "ABC" )
		) : null;
		this.list = List.of( new StringNode( "ABC" ) );
	}

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					session.persist( new EntityWithXml( 1, stringMap, objectMap, list ) );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor( EntityWithXml.class );
		final JdbcTypeRegistry jdbcTypeRegistry = mappingMetamodel.getTypeConfiguration().getJdbcTypeRegistry();

		final BasicAttributeMapping stringMapAttribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping(
				"stringMap" );
		final BasicAttributeMapping objectMapAttribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping(
				"objectMap" );
		final BasicAttributeMapping listAttribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping(
				"list" );
		assertThat( stringMapAttribute.getJavaType().getJavaTypeClass(), equalTo( Map.class ) );
		assertThat( objectMapAttribute.getJavaType().getJavaTypeClass(), equalTo( Map.class ) );
		assertThat( listAttribute.getJavaType().getJavaTypeClass(), equalTo( List.class ) );

		final JdbcType xmlType = jdbcTypeRegistry.getDescriptor( SqlTypes.SQLXML );
		assertThat( stringMapAttribute.getJdbcMapping().getJdbcType(), isA( (Class<JdbcType>) xmlType.getClass() ) );
		assertThat( objectMapAttribute.getJdbcMapping().getJdbcType(), isA( (Class<JdbcType>) xmlType.getClass() ) );
		assertThat( listAttribute.getJdbcMapping().getJdbcType(), isA( (Class<JdbcType>) xmlType.getClass() ) );
	}

	@Test
	public void verifyReadWorks(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					EntityWithXml entityWithXml = session.find( EntityWithXml.class, 1 );
					assertThat( entityWithXml.stringMap, is( stringMap ) );
					assertThat( entityWithXml.objectMap, is( objectMap ) );
					assertThat( entityWithXml.list, is( list ) );
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = DerbyDialect.class, reason = "Derby doesn't support comparing CLOBs with the = operator")
	@SkipForDialect(dialectClass = HANADialect.class, matchSubTypes = true, reason = "HANA doesn't support comparing LOBs with the = operator")
	@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true, reason = "Sybase doesn't support comparing LOBs with the = operator")
	@SkipForDialect(dialectClass = OracleDialect.class, matchSubTypes = true, reason = "Oracle doesn't support comparing JSON with the = operator")
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Altibase doesn't support comparing CLOBs with the = operator")
	public void verifyComparisonWorks(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) ->  {
					EntityWithXml entityWithJson = session.createQuery(
									"from EntityWithXml e where e.stringMap = :param",
									EntityWithXml.class
							)
							.setParameter( "param", stringMap )
							.getSingleResult();
					assertThat( entityWithJson, notNullValue() );
					assertThat( entityWithJson.stringMap, is( stringMap ) );
					assertThat( entityWithJson.objectMap, is( objectMap ) );
					assertThat( entityWithJson.list, is( list ) );
				}
		);
	}

	@Entity(name = "EntityWithXml")
	@Table(name = "EntityWithXml")
	public static class EntityWithXml {
		@Id
		private Integer id;

		//tag::basic-xml-example[]
		@JdbcTypeCode( SqlTypes.SQLXML )
		private Map<String, StringNode> stringMap;
		//end::basic-xml-example[]

		@JdbcTypeCode( SqlTypes.SQLXML )
		private Map<StringNode, StringNode> objectMap;

		@JdbcTypeCode( SqlTypes.SQLXML )
		private List<StringNode> list;

		public EntityWithXml() {
		}

		public EntityWithXml(
				Integer id,
				Map<String, StringNode> stringMap,
				Map<StringNode, StringNode> objectMap,
				List<StringNode> list) {
			this.id = id;
			this.stringMap = stringMap;
			this.objectMap = objectMap;
			this.list = list;
		}
	}

	@XmlRootElement(name = "stringNode")
	public static class StringNode {
		private String string;

		public StringNode() {
		}

		public StringNode(String string) {
			this.string = string;
		}

		@XmlElement
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
