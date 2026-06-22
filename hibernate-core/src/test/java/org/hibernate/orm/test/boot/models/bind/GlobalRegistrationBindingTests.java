/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.NamedEntityGraphDefinition;
import org.hibernate.annotations.CollectionTypeRegistration;
import org.hibernate.annotations.CompositeTypeRegistration;
import org.hibernate.annotations.ConverterRegistration;
import org.hibernate.annotations.EmbeddableInstantiatorRegistration;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Imported;
import org.hibernate.annotations.JavaTypeRegistration;
import org.hibernate.annotations.JdbcTypeRegistration;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.TypeRegistration;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.jpa.SpecHints;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.util.uuid.IdGeneratorCreationContext;
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
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.Fetch;
import jakarta.persistence.FetchType;
import jakarta.persistence.FieldResult;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedNativeStatement;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedStatement;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.QueryHint;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.StoredProcedureParameter;
import jakarta.persistence.TableGenerator;

import static org.hibernate.orm.test.boot.models.bind.BindingTestingHelper.buildCategorizedDomainModel;
import static org.hibernate.id.OptimizableGenerator.INCREMENT_PARAM;
import static org.hibernate.id.OptimizableGenerator.INITIAL_PARAM;
import static org.hibernate.id.enhanced.SequenceStyleGenerator.SEQUENCE_PARAM;
import static org.hibernate.id.enhanced.TableGenerator.SEGMENT_COLUMN_PARAM;
import static org.hibernate.id.enhanced.TableGenerator.SEGMENT_VALUE_PARAM;
import static org.hibernate.id.enhanced.TableGenerator.TABLE_PARAM;
import static org.hibernate.id.enhanced.TableGenerator.VALUE_COLUMN_PARAM;
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

					final var sequenceGenerator = metadataCollector.getIdentifierGenerator( "global_seq" );
					assertThat( sequenceGenerator.getStrategy() ).isEqualTo( SequenceStyleGenerator.class.getName() );
					assertThat( sequenceGenerator.getParameters() )
							.containsEntry( SEQUENCE_PARAM, "global_sequence" )
							.containsEntry( INITIAL_PARAM, "7" )
							.containsEntry( INCREMENT_PARAM, "13" );
					final var tableGenerator = metadataCollector.getIdentifierGenerator( "global_table" );
					assertThat( tableGenerator.getStrategy() )
							.isEqualTo( org.hibernate.id.enhanced.TableGenerator.class.getName() );
					assertThat( tableGenerator.getParameters() )
							.containsEntry( TABLE_PARAM, "global_id_table" )
							.containsEntry( SEGMENT_COLUMN_PARAM, "segment_name" )
							.containsEntry( SEGMENT_VALUE_PARAM, "global_segment" )
							.containsEntry( VALUE_COLUMN_PARAM, "next_value" )
							.containsEntry( INITIAL_PARAM, "4" )
							.containsEntry( INCREMENT_PARAM, "17" );
					final var genericGenerator = metadataCollector.getIdentifierGenerator(
							GlobalIdentifierGenerator.class.getName()
					);
					assertThat( genericGenerator.getStrategy() ).isEqualTo( GlobalIdentifierGenerator.class.getName() );
					assertThat( genericGenerator.getParameters() ).containsEntry( "role", "global" );

					assertThat( metadataCollector.getAttributeConverterManager()
							.findRegisteredConversion( GlobalConverted.class ) ).isNotNull();
					assertThat( metadataCollector.getAttributeConverterManager()
							.findRegisteredConversion( PlainConverted.class ) ).isNotNull();
					assertThat( metadataCollector.getImports() )
							.containsEntry( "GlobalRegistrationAlias", GlobalRegistrationEntity.class.getName() );

					final var jpaQuery = metadataCollector.getNamedHqlQueryMapping( "globalJpaQuery" );
					assertThat( jpaQuery.getHqlString() ).isEqualTo( "from GlobalRegistrationEntity" );
					assertThat( jpaQuery.getHints() ).containsEntry( "global.query.hint", "jpa-query" );
					assertThat( metadataCollector.getNamedHqlQueryMapping( "globalJpaStatement" ).getHqlString() )
							.isEqualTo( "update GlobalRegistrationEntity set id = id" );
					assertThat( metadataCollector.getNamedHqlQueryMapping( "globalHibernateQuery" ).getHqlString() )
							.isEqualTo( "from GlobalRegistrationEntity" );
					final var nativeQuery = metadataCollector.getNamedNativeQueryMapping( "globalNativeQuery" );
					assertThat( nativeQuery.getSqlQueryString() ).isEqualTo( "select * from global_registration_entities" );
					assertThat( nativeQuery.getHints() ).containsEntry( "global.query.hint", "native-query" );
					assertThat( metadataCollector.getNamedNativeQueryMapping( "globalNativeStatement" ).getSqlQueryString() )
							.isEqualTo( "update global_registration_entities set id = id" );
					assertThat( metadataCollector.getNamedNativeQueryMapping( "globalMappedNativeQuery" ).getResultSetMappingName() )
							.isEqualTo( "globalIdMapping" );
					final var procedure = metadataCollector.getNamedProcedureCallMapping( "globalProcedure" );
					assertThat( procedure.getProcedureName() ).isEqualTo( "global_registration_procedure" );
					assertThat( procedure.getHints() ).containsEntry( SpecHints.HINT_SPEC_QUERY_TIMEOUT, "2500" );
					final var resultSetMapping = metadataCollector.getResultSetMapping( "globalIdMapping" );
					assertThat( resultSetMapping.getRegistrationName() ).isEqualTo( "globalIdMapping" );
					assertThat( resultSetMapping.getLocation() ).isNull();
					assertThat( metadataCollector.getResultSetMapping( "globalEntityMapping" ).getRegistrationName() )
							.isEqualTo( "globalEntityMapping" );
					assertThat( metadataCollector.getResultSetMapping( "globalConstructorMapping" ).getRegistrationName() )
							.isEqualTo( "globalConstructorMapping" );
					final var entityGraph = metadataCollector.getNamedEntityGraph( "globalGraph" );
					assertThat( entityGraph.entityName() ).isEqualTo( "GlobalRegistrationEntity" );
					assertThat( entityGraph.source() ).isEqualTo( NamedEntityGraphDefinition.Source.JPA );
					assertThat( entityGraph.graphCreator() ).isNotNull();
					assertThat( entityGraph.graphCreator().getClass().getSimpleName() ).isEqualTo( "NamedGraphCreatorJpa" );
					verifyJpaFetchGraphContribution( entityGraph );
					final var parsedEntityGraph = metadataCollector.getNamedEntityGraph( "globalParsedGraph" );
					assertThat( parsedEntityGraph.entityName() ).isEqualTo( "GlobalRegistrationEntity" );
					assertThat( parsedEntityGraph.source() ).isEqualTo( NamedEntityGraphDefinition.Source.PARSED );
					assertThat( parsedEntityGraph.graphCreator().getClass().getSimpleName() )
							.isEqualTo( "NamedGraphCreatorParsed" );
					final var filterDefinition = metadataCollector.getFilterDefinition( "globalFilter" );
					assertThat( filterDefinition.getDefaultFilterCondition() )
							.isEqualTo( "name = :name" );
					assertThat( filterDefinition.isAutoEnabled() ).isTrue();
					assertThat( filterDefinition.isAppliedToLoadByKey() ).isTrue();
					assertThat( filterDefinition.getParameterNames() ).containsExactly( "name" );
					assertThat( metadataCollector.getFetchProfile( "globalFetchProfile" ).getFetches() )
							.anySatisfy( (fetch) -> {
								assertThat( fetch.getEntity() ).isEqualTo( GlobalRegistrationEntity.class.getName() );
								assertThat( fetch.getAssociation() ).isEqualTo( "parent" );
							} );

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

					final RootClass uuidEntityBinding = (RootClass) metadataCollector.getEntityBinding(
							UuidGeneratedEntity.class.getName()
					);
					final BasicValue uuidIdentifier = (BasicValue) uuidEntityBinding.getIdentifier();
					assertThat( uuidIdentifier.getCustomIdGeneratorCreator() ).isNotNull();
					final Generator uuidGenerator = uuidIdentifier.getCustomIdGeneratorCreator().createGenerator(
							new IdGeneratorCreationContext( context.getMetadata(), uuidEntityBinding )
					);
					assertThat( uuidGenerator ).isInstanceOf( org.hibernate.id.uuid.UuidGenerator.class );
					assertThat( ( (org.hibernate.id.uuid.UuidGenerator) uuidGenerator ).getValueGenerator() )
							.isInstanceOf( StandardRandomStrategy.class );
				},
				scope.getRegistry(),
				GlobalRegistrationEntity.class,
				UuidGeneratedEntity.class,
				PlainConverter.class
		);
	}

	@Test
	void testSqlResultSetMappingHelperAnnotationCapture() {
		final var categorizedModel = buildCategorizedDomainModel( GlobalRegistrationEntity.class, PlainConverter.class );
		final var mappings = categorizedModel.getGlobalRegistrations().getSqlResultSetMappingRegistrations();

		final var columnMapping = mappings.get( "globalIdMapping" ).configuration();
		assertThat( columnMapping.columns() ).singleElement().satisfies( (columnResult) -> {
			assertThat( columnResult.name() ).isEqualTo( "id" );
			assertThat( columnResult.type() ).isEqualTo( Integer.class );
		} );

		final var entityMapping = mappings.get( "globalEntityMapping" ).configuration();
		assertThat( entityMapping.entities() ).singleElement().satisfies( (entityResult) -> {
			assertThat( entityResult.entityClass() ).isEqualTo( GlobalRegistrationEntity.class );
			assertThat( entityResult.fields() ).singleElement().satisfies( (fieldResult) -> {
				assertThat( fieldResult.name() ).isEqualTo( "id" );
				assertThat( fieldResult.column() ).isEqualTo( "entity_id" );
			} );
		} );

		final var constructorMapping = mappings.get( "globalConstructorMapping" ).configuration();
		assertThat( constructorMapping.classes() ).singleElement().satisfies( (constructorResult) -> {
			assertThat( constructorResult.targetClass() ).isEqualTo( GlobalProjection.class );
			assertThat( constructorResult.columns() ).singleElement().satisfies( (columnResult) -> {
				assertThat( columnResult.name() ).isEqualTo( "projection_id" );
				assertThat( columnResult.type() ).isEqualTo( Integer.class );
			} );
		} );
	}

	@SuppressWarnings("unchecked")
	private static void verifyJpaFetchGraphContribution(NamedEntityGraphDefinition entityGraph) {
		final List<?> fetchContributions = readField( entityGraph.graphCreator(), "fetchContributions" );
		assertThat( fetchContributions ).singleElement().satisfies( (contribution) -> {
			assertThat( (String) readField( contribution, "graphName" ) ).isEqualTo( "globalGraph" );
			assertThat( (String) readField( contribution, "attributeName" ) ).isEqualTo( "parent" );
			assertThat( (String[]) readField( contribution, "subgraphNames" ) ).isEmpty();
			assertThat( (List<Object>) readField( contribution, "options" ) )
					.contains(
							FetchType.EAGER,
							CacheStoreMode.BYPASS,
							new jakarta.persistence.BatchSize( 5 )
					);
		} );
	}

	@SuppressWarnings("unchecked")
	private static <T> T readField(Object target, String fieldName) {
		try {
			final var field = target.getClass().getDeclaredField( fieldName );
			field.setAccessible( true );
			return (T) field.get( target );
		}
		catch (IllegalAccessException | NoSuchFieldException e) {
			throw new AssertionError( e );
		}
	}

	@Entity(name = "GlobalRegistrationEntity")
	@jakarta.persistence.Table(name = "global_registration_entities")
	@Imported(rename = "GlobalRegistrationAlias")
	@SequenceGenerator(name = "global_seq", sequenceName = "global_sequence", initialValue = 7, allocationSize = 13)
	@TableGenerator(
			name = "global_table",
			table = "global_id_table",
			pkColumnName = "segment_name",
			pkColumnValue = "global_segment",
			valueColumnName = "next_value",
			initialValue = 3,
			allocationSize = 17
	)
	@GenericGenerator(type = GlobalIdentifierGenerator.class, parameters = @Parameter(name = "role", value = "global"))
	@NamedQuery(
			name = "globalJpaQuery",
			query = "from GlobalRegistrationEntity",
			hints = @QueryHint(name = "global.query.hint", value = "jpa-query")
	)
	@NamedStatement(name = "globalJpaStatement", statement = "update GlobalRegistrationEntity set id = id")
	@NamedNativeQuery(
			name = "globalNativeQuery",
			query = "select * from global_registration_entities",
			hints = @QueryHint(name = "global.query.hint", value = "native-query")
	)
	@NamedNativeStatement(name = "globalNativeStatement", statement = "update global_registration_entities set id = id")
	@NamedNativeQuery(
			name = "globalMappedNativeQuery",
			query = "select id as id from global_registration_entities",
			resultSetMapping = "globalIdMapping"
	)
	@NamedStoredProcedureQuery(
			name = "globalProcedure",
			procedureName = "global_registration_procedure",
			parameters = @StoredProcedureParameter(name = "name", mode = ParameterMode.IN, type = String.class),
			resultSetMappings = "globalIdMapping",
			hints = @QueryHint(name = SpecHints.HINT_SPEC_QUERY_TIMEOUT, value = "2500")
	)
	@SqlResultSetMapping(name = "globalIdMapping", columns = @ColumnResult(name = "id", type = Integer.class))
	@SqlResultSetMapping(
			name = "globalEntityMapping",
			entities = @EntityResult(
					entityClass = GlobalRegistrationEntity.class,
					fields = @FieldResult(name = "id", column = "entity_id")
			)
	)
	@SqlResultSetMapping(
			name = "globalConstructorMapping",
			classes = @ConstructorResult(
					targetClass = GlobalProjection.class,
					columns = @ColumnResult(name = "projection_id", type = Integer.class)
			)
	)
	@org.hibernate.annotations.NamedQuery(name = "globalHibernateQuery", query = "from GlobalRegistrationEntity")
	@NamedEntityGraph(name = "globalGraph", attributeNodes = @NamedAttributeNode("id"))
	@org.hibernate.annotations.NamedEntityGraph(name = "globalParsedGraph", graph = "parent")
	@FilterDef(
			name = "globalFilter",
			defaultCondition = "name = :name",
			autoEnabled = true,
			applyToLoadByKey = true,
			parameters = @ParamDef(name = "name", type = String.class)
	)
	@FetchProfile(
			name = "globalFetchProfile",
			fetchOverrides = @FetchProfile.FetchOverride(
					entity = GlobalRegistrationEntity.class,
					association = "parent"
			)
	)
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
		@ManyToOne
		@JoinColumn(name = "parent_id")
		@Fetch(graph = "globalGraph", type = FetchType.EAGER, batchSize = 5, cacheStoreMode = CacheStoreMode.BYPASS)
		private GlobalRegistrationEntity parent;
	}

	public static class GlobalIdentifierGenerator implements IdentifierGenerator {
		@Override
		public Object generate(SharedSessionContractImplementor session, Object object) {
			return 1;
		}
	}

	@Entity(name = "UuidGeneratedEntity")
	public static class UuidGeneratedEntity {
		@Id
		@UuidGenerator(style = UuidGenerator.Style.RANDOM)
		private UUID id;
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

	public record GlobalProjection(Integer id) {
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
