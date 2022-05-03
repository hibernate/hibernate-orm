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
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

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

	private final boolean supportsObjectMapKey;

	protected XmlMappingTests(boolean supportsObjectMapKey) {
		this.supportsObjectMapKey = supportsObjectMapKey;
	}

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor( EntityWithXml.class);
		final JdbcTypeRegistry jdbcTypeRegistry = mappingMetamodel.getTypeConfiguration().getJdbcTypeRegistry();

		final BasicAttributeMapping stringMapAttribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "stringMap" );
		final BasicAttributeMapping objectMapAttribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "objectMap" );
		final BasicAttributeMapping listAttribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "list" );
		assertThat( stringMapAttribute.getJavaType().getJavaTypeClass(), equalTo( Map.class ) );
		assertThat( objectMapAttribute.getJavaType().getJavaTypeClass(), equalTo( Map.class ) );
		assertThat( listAttribute.getJavaType().getJavaTypeClass(), equalTo( List.class ) );

		final JdbcType xmlType = jdbcTypeRegistry.getDescriptor(SqlTypes.SQLXML);
		assertThat( stringMapAttribute.getJdbcMapping().getJdbcType(), is( xmlType ) );
		assertThat( objectMapAttribute.getJdbcMapping().getJdbcType(), is( xmlType ) );
		assertThat( listAttribute.getJdbcMapping().getJdbcType(), is( xmlType ) );

		Map<String, StringNode> stringMap = Map.of( "name", new StringNode( "ABC" ) );
		Map<StringNode, StringNode> objectMap = supportsObjectMapKey ? Map.of( new StringNode( "name" ), new StringNode( "ABC" ) ) : null;
		List<StringNode> list = List.of( new StringNode( "ABC" ) );
		scope.inTransaction(
				(session) -> {
					session.persist( new EntityWithXml( 1, stringMap, objectMap, list ) );
				}
		);

		scope.inTransaction(
				(session) -> {
					EntityWithXml entityWithXml = session.find( EntityWithXml.class, 1 );
					assertThat( entityWithXml.stringMap, is( stringMap ) );
					assertThat( entityWithXml.objectMap, is( objectMap ) );
					assertThat( entityWithXml.list, is( list ) );
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
