/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.annotations.CollectionTypeRegistration;
import org.hibernate.annotations.CompositeTypeRegistration;
import org.hibernate.annotations.ConverterRegistration;
import org.hibernate.annotations.EmbeddableInstantiatorRegistration;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JavaTypeRegistration;
import org.hibernate.annotations.JdbcTypeRegistration;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.TypeRegistration;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IncrementGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserCollectionType;
import org.hibernate.usertype.UserType;

import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests that persistence-unit scoped registrations collected during
/// categorization are applied to Hibernate's boot metadata collector.
///
/// @author Steve Ebersole
public class GlobalRegistrationBindingTests {
	@Test
	@ServiceRegistry
	void testGlobalRegistrations(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final var metadataCollector = context.getMetadataCollector();

					assertThat( metadataCollector.getIdentifierGenerator( "global_seq" ).getStrategy() )
							.isEqualTo( SequenceStyleGenerator.class.getName() );
					assertThat( metadataCollector.getIdentifierGenerator( "global_table" ).getStrategy() )
							.isEqualTo( org.hibernate.id.enhanced.TableGenerator.class.getName() );
					assertThat( metadataCollector.getIdentifierGenerator( "global_increment" ).getStrategy() )
							.isEqualTo( IncrementGenerator.class.getName() );

					assertThat( metadataCollector.getAttributeConverterManager()
							.findRegisteredConversion( GlobalConverted.class ) ).isNotNull();
					assertThat( metadataCollector.getAttributeConverterManager()
							.findRegisteredConversion( PlainConverted.class ) ).isNotNull();

					assertThat( metadataCollector.getNamedHqlQueryMapping( "globalJpaQuery" ).getHqlString() )
							.isEqualTo( "from GlobalRegistrationEntity" );
					assertThat( metadataCollector.getNamedHqlQueryMapping( "globalHibernateQuery" ).getHqlString() )
							.isEqualTo( "from GlobalRegistrationEntity" );
					assertThat( metadataCollector.getNamedNativeQueryMapping( "globalNativeQuery" ).getSqlQueryString() )
							.isEqualTo( "select * from global_registration_entities" );
					assertThat( metadataCollector.getNamedEntityGraph( "globalGraph" ).entityName() )
							.isEqualTo( "GlobalRegistrationEntity" );

					final var typeConfiguration = metadataCollector.getTypeConfiguration();
					assertThat( typeConfiguration.getJavaTypeRegistry().findDescriptor( GlobalJavaTypeDomain.class ) )
							.isInstanceOf( GlobalJavaType.class );
					assertThat( typeConfiguration.getJdbcTypeRegistry().getDescriptor( GlobalJdbcType.CODE ) )
							.isInstanceOf( GlobalJdbcType.class );

					assertThat( metadataCollector.findRegisteredUserType( GlobalUserTypeDomain.class ) )
							.isEqualTo( GlobalUserType.class );
					assertThat( metadataCollector.findRegisteredCompositeUserType( GlobalEmbeddable.class ) )
							.isEqualTo( GlobalCompositeUserType.class );
					assertThat( metadataCollector.findRegisteredEmbeddableInstantiator( GlobalInstantiatedEmbeddable.class ) )
							.isEqualTo( GlobalInstantiator.class );

					final var collectionTypeRegistration = metadataCollector.findCollectionTypeRegistration(
							CollectionClassification.BAG
					);
					assertThat( collectionTypeRegistration.implementation() ).isEqualTo( GlobalCollectionType.class );
					assertThat( collectionTypeRegistration.parameters() ).containsEntry( "role", "global" );

					final PersistentClass entityBinding = metadataCollector.getEntityBinding( GlobalRegistrationEntity.class.getName() );
					assertThat( entityBinding ).isNotNull();
				},
				scope.getRegistry(),
				GlobalRegistrationEntity.class,
				PlainConverter.class
		);
	}

	@Entity(name = "GlobalRegistrationEntity")
	@jakarta.persistence.Table(name = "global_registration_entities")
	@SequenceGenerator(name = "global_seq", sequenceName = "global_sequence")
	@TableGenerator(name = "global_table", table = "global_id_table")
	@GenericGenerator(name = "global_increment", type = IncrementGenerator.class)
	@NamedQuery(name = "globalJpaQuery", query = "from GlobalRegistrationEntity")
	@NamedNativeQuery(name = "globalNativeQuery", query = "select * from global_registration_entities")
	@org.hibernate.annotations.NamedQuery(name = "globalHibernateQuery", query = "from GlobalRegistrationEntity")
	@NamedEntityGraph(name = "globalGraph", attributeNodes = @NamedAttributeNode("id"))
	@ConverterRegistration(domainType = GlobalConverted.class, converter = GlobalConverter.class, autoApply = true)
	@JavaTypeRegistration(javaType = GlobalJavaTypeDomain.class, descriptorClass = GlobalJavaType.class)
	@JdbcTypeRegistration(registrationCode = GlobalJdbcType.CODE, value = GlobalJdbcType.class)
	@TypeRegistration(basicClass = GlobalUserTypeDomain.class, userType = GlobalUserType.class)
	@CompositeTypeRegistration(embeddableClass = GlobalEmbeddable.class, userType = GlobalCompositeUserType.class)
	@CollectionTypeRegistration(
			classification = CollectionClassification.BAG,
			type = GlobalCollectionType.class,
			parameters = @Parameter(name = "role", value = "global")
	)
	@EmbeddableInstantiatorRegistration(
			embeddableClass = GlobalInstantiatedEmbeddable.class,
			instantiator = GlobalInstantiator.class
	)
	public static class GlobalRegistrationEntity {
		@Id
		private Integer id;
	}

	public record GlobalConverted(String value) {
	}

	public static class GlobalConverter implements AttributeConverter<GlobalConverted, String> {
		@Override
		public String convertToDatabaseColumn(GlobalConverted attribute) {
			return attribute == null ? null : attribute.value();
		}

		@Override
		public GlobalConverted convertToEntityAttribute(String dbData) {
			return dbData == null ? null : new GlobalConverted( dbData );
		}
	}

	public record PlainConverted(String value) {
	}

	@Converter(autoApply = true)
	public static class PlainConverter implements AttributeConverter<PlainConverted, String> {
		@Override
		public String convertToDatabaseColumn(PlainConverted attribute) {
			return attribute == null ? null : attribute.value();
		}

		@Override
		public PlainConverted convertToEntityAttribute(String dbData) {
			return dbData == null ? null : new PlainConverted( dbData );
		}
	}

	public record GlobalJavaTypeDomain(String value) {
	}

	public static class GlobalJavaType extends AbstractClassJavaType<GlobalJavaTypeDomain> {
		public GlobalJavaType() {
			super( GlobalJavaTypeDomain.class );
		}

		@Override
		public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
			return indicators.getTypeConfiguration().getJdbcTypeRegistry().getDescriptor( SqlTypes.VARCHAR );
		}

		@Override
		public GlobalJavaTypeDomain fromString(CharSequence string) {
			return string == null ? null : new GlobalJavaTypeDomain( string.toString() );
		}

		@Override
		public <X> X unwrap(GlobalJavaTypeDomain value, Class<X> type, WrapperOptions options) {
			if ( value == null ) {
				return null;
			}
			if ( type.isAssignableFrom( String.class ) ) {
				return type.cast( value.value() );
			}
			throw unknownUnwrap( type );
		}

		@Override
		public <X> GlobalJavaTypeDomain wrap(X value, WrapperOptions options) {
			if ( value == null ) {
				return null;
			}
			if ( value instanceof String string ) {
				return new GlobalJavaTypeDomain( string );
			}
			throw unknownWrap( value.getClass() );
		}
	}

	public static class GlobalJdbcType extends VarcharJdbcType {
		static final int CODE = 42_424;

		@Override
		public int getJdbcTypeCode() {
			return CODE;
		}
	}

	public record GlobalUserTypeDomain(String value) {
	}

	public static class GlobalUserType implements UserType<GlobalUserTypeDomain> {
		@Override
		public int getSqlType() {
			return SqlTypes.VARCHAR;
		}

		@Override
		public Class<GlobalUserTypeDomain> returnedClass() {
			return GlobalUserTypeDomain.class;
		}

		@Override
		public GlobalUserTypeDomain nullSafeGet(
				ResultSet rs,
				int position,
				WrapperOptions options) throws SQLException {
			final String value = rs.getString( position );
			return value == null ? null : new GlobalUserTypeDomain( value );
		}

		@Override
		public void nullSafeSet(
				PreparedStatement st,
				GlobalUserTypeDomain value,
				int index,
				WrapperOptions options) throws SQLException {
			st.setString( index, value == null ? null : value.value() );
		}

		@Override
		public GlobalUserTypeDomain deepCopy(GlobalUserTypeDomain value) {
			return value;
		}

		@Override
		public boolean isMutable() {
			return false;
		}
	}

	public record GlobalCompositeDomain(String name) {
	}

	@Embeddable
	public static class GlobalEmbeddable {
		private String name;
	}

	public static class GlobalCompositeUserType implements CompositeUserType<GlobalCompositeDomain> {
		@Override
		public Object getPropertyValue(GlobalCompositeDomain component, int property) throws HibernateException {
			return component.name();
		}

		@Override
		public GlobalCompositeDomain instantiate(ValueAccess values) {
			return new GlobalCompositeDomain( values.getValue( 0, String.class ) );
		}

		@Override
		public Class<?> embeddable() {
			return GlobalEmbeddable.class;
		}

		@Override
		public Class<GlobalCompositeDomain> returnedClass() {
			return GlobalCompositeDomain.class;
		}

		@Override
		public boolean equals(GlobalCompositeDomain x, GlobalCompositeDomain y) {
			return java.util.Objects.equals( x, y );
		}

		@Override
		public int hashCode(GlobalCompositeDomain x) {
			return java.util.Objects.hashCode( x );
		}

		@Override
		public GlobalCompositeDomain deepCopy(GlobalCompositeDomain value) {
			return value;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public Serializable disassemble(GlobalCompositeDomain value) {
			return value == null ? null : value.name();
		}

		@Override
		public GlobalCompositeDomain assemble(Serializable cached, Object owner) {
			return cached == null ? null : new GlobalCompositeDomain( (String) cached );
		}

		@Override
		public GlobalCompositeDomain replace(GlobalCompositeDomain detached, GlobalCompositeDomain managed, Object owner) {
			return detached;
		}
	}

	@Embeddable
	public static class GlobalInstantiatedEmbeddable {
		private String value;
	}

	public static class GlobalInstantiator implements org.hibernate.metamodel.spi.EmbeddableInstantiator {
		@Override
		public Object instantiate(ValueAccess values) {
			final GlobalInstantiatedEmbeddable embeddable = new GlobalInstantiatedEmbeddable();
			embeddable.value = values.getValue( 0, String.class );
			return embeddable;
		}

		@Override
		public boolean isInstance(Object object) {
			return object instanceof GlobalInstantiatedEmbeddable;
		}

		@Override
		public boolean isSameClass(Object object) {
			return object != null && object.getClass().equals( GlobalInstantiatedEmbeddable.class );
		}
	}

	public static class GlobalCollectionType implements UserCollectionType {
		@Override
		public CollectionClassification getClassification() {
			return CollectionClassification.LIST;
		}

		@Override
		public Class<?> getCollectionClass() {
			return java.util.ArrayList.class;
		}

		@Override
		public PersistentCollection<?> instantiate(
				SharedSessionContractImplementor session,
				CollectionPersister persister) throws HibernateException {
			return null;
		}

		@Override
		public PersistentCollection<?> wrap(
				SharedSessionContractImplementor session,
				Object collection) {
			return null;
		}

		@Override
		public java.util.Iterator<?> getElementsIterator(Object collection) {
			return java.util.Collections.emptyIterator();
		}

		@Override
		public boolean contains(Object collection, Object entity) {
			return false;
		}

		@Override
		public Object indexOf(Object collection, Object entity) {
			return null;
		}

		@Override
		public Object replaceElements(
				Object original,
				Object target,
				CollectionPersister persister,
				Object owner,
				java.util.Map copyCache,
				SharedSessionContractImplementor session) throws HibernateException {
			return target;
		}

		@Override
		public Object instantiate(int anticipatedSize) {
			return new java.util.ArrayList<>();
		}
	}
}
