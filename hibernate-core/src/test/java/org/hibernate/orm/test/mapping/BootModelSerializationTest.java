/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.sql.Types;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureParameter;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;

import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.CompositeType;
import org.hibernate.annotations.CollectionType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SortComparator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.serial.MetadataSerialization;
import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.serial.internal.MappingModelGraphIndex;
import org.hibernate.boot.serial.internal.MappingResolutionEnvironmentFingerprint;
import org.hibernate.boot.serial.internal.MappingResolutionSnapshot;
import org.hibernate.boot.pipeline.internal.ResolvedMappingImplementor;
import org.hibernate.boot.mapping.internal.context.MappingResolutionState;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.MappingRole;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;

import org.hibernate.orm.test.idgen.GeneratorSettingsImpl;
import org.hibernate.orm.test.mapping.collections.custom.declaredtype.HeadList;
import org.hibernate.orm.test.mapping.collections.custom.declaredtype.HeadListType;
import org.hibernate.orm.test.mapping.collections.custom.declaredtype.IHeadList;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DomainModel( annotatedClasses = {
		BootModelSerializationTest.Publisher.class,
		BootModelSerializationTest.Book.class,
		BootModelSerializationTest.TableGeneratedEntity.class,
		BootModelSerializationTest.CustomGeneratedEntity.class,
		BootModelSerializationTest.GenericGeneratedEntity.class,
		BootModelSerializationTest.GenericStringEntity.class,
		BootModelSerializationTest.GenericIntegerEntity.class,
		BootModelSerializationTest.GenericIntegerIdEntity.class,
		BootModelSerializationTest.GenericAmountEntity.class,
		BootModelSerializationTest.PublicationDetails.class,
		BootModelSerializationTest.SpecialPublicationDetails.class
} )
@ServiceRegistry(settings = {
		@Setting(name = org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_AUTO, value = "create-drop"),
		@Setting(name = org.hibernate.cfg.MappingSettings.METADATA_SERIALIZATION_ENABLED, value = "true")
})
class BootModelSerializationTest {
	@Test
	void serializationRequiresFactoryReadyMetadata(DomainModelScope scope) {
		final var resolved = (ResolvedMappingImplementor) scope.getDomainModel();
		assertThatThrownBy( () -> MetadataSerialization.serialize( resolved.getResolvedMapping().metadata() ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "factory-ready" );
	}

	@Test
	void restorationRejectsMissingAndUnknownResolutionRoles(DomainModelScope scope) {
		final var resolved = (ResolvedMappingImplementor) scope.getDomainModel();
		final var product = resolved.getResolvedMapping();
		final var metadata = (MetadataImpl) product.metadata();
		final var snapshot = product.mappingResolutionDetailsCollector().freeze( metadata );

		final MappingRole existingRole = snapshot.basicValueRecipes().keySet().iterator().next();
		final var missingRecipe = new LinkedHashMap<>( snapshot.basicValueRecipes() );
		missingRecipe.remove( existingRole );
		assertThatThrownBy( () -> restoreSnapshot(
				new MappingResolutionSnapshot( missingRecipe, snapshot.environmentFingerprint() ),
				metadata
		) )
				.isInstanceOf( IllegalStateException.class )
				.hasMessageContaining( existingRole.getFullPath() );

		final var unknownRole = new LinkedHashMap<>( snapshot.basicValueRecipes() );
		final MappingRole missingRole = MappingRole.entity( "missing" ).appendAttribute( "value" );
		unknownRole.put( missingRole, snapshot.basicValueRecipes().get( existingRole ) );
		assertThatThrownBy( () -> restoreSnapshot(
				new MappingResolutionSnapshot( unknownRole, snapshot.environmentFingerprint() ),
				metadata
		) )
				.isInstanceOf( IllegalStateException.class )
				.hasMessageContaining( missingRole.getFullPath() );
	}

	@Test
	void restorationRejectsIncompatibleResolutionEnvironment(DomainModelScope scope) {
		final var product = ( (ResolvedMappingImplementor) scope.getDomainModel() ).getResolvedMapping();
		final var snapshot = product.mappingResolutionDetailsCollector().freeze( (MetadataImpl) product.metadata() );
		final var expected = snapshot.environmentFingerprint();
		final var incompatible = new MappingResolutionEnvironmentFingerprint(
				"org.example.IncompatibleDialect",
				expected.preferredBooleanJdbcType(),
				expected.preferredDurationJdbcType(),
				expected.preferredUuidJdbcType(),
				expected.preferredInstantJdbcType(),
				expected.preferredArrayJdbcType(),
				expected.preferJavaTimeJdbcTypes(),
				expected.preferNativeEnumTypes(),
				expected.preferLocaleLanguageTag(),
				expected.defaultTimeZoneStorage(),
				expected.wrapperArrayHandling(),
				expected.nationalizedCharacterData(),
				expected.legacyXmlFormat()
		);
		assertThatThrownBy( () -> restoreSnapshot(
				new MappingResolutionSnapshot( snapshot.basicValueRecipes(), incompatible ),
				(MetadataImpl) product.metadata()
		) )
				.isInstanceOf( IllegalStateException.class )
				.hasMessageContaining( "Incompatible mapping-resolution environment" );
	}

	@Test
	void restorationReportsMissingConverterClassWithMappingRole(DomainModelScope scope) {
		final var product = ( (ResolvedMappingImplementor) scope.getDomainModel() ).getResolvedMapping();
		final var metadata = (MetadataImpl) product.metadata();
		final var snapshot = product.mappingResolutionDetailsCollector().freeze( metadata );
		final var recipes = new LinkedHashMap<>( snapshot.basicValueRecipes() );
		final var converterEntry = recipes.entrySet().stream()
				.filter( entry -> entry.getValue().converter() != null )
				.findFirst()
				.orElseThrow();
		final var recipe = converterEntry.getValue();
		recipes.put(
				converterEntry.getKey(),
				new org.hibernate.boot.serial.internal.BasicValueRestorationRecipe(
						recipe.source(),
						recipe.role(),
						recipe.ownerName(),
						recipe.propertyName(),
						recipe.softDelete(),
						recipe.softDeleteStrategy(),
						recipe.typeParameters(),
						recipe.explicitTypeName(),
						new org.hibernate.boot.serial.internal.AttributeConverterRestorationRecipe(
								"org.example.MissingConverter",
								null
						),
						recipe.timeZoneStorageType(),
						recipe.enumerationStyle(),
						recipe.temporalPrecision(),
						recipe.jdbcTypeCode(),
						recipe.resolvedJavaTypeName(),
						recipe.explicitJavaTypeDescriptorClassName(),
						recipe.explicitJdbcTypeDescriptorClassName(),
						recipe.explicitMutabilityPlanClassName(),
						recipe.attributeImmutable(),
						recipe.attributeMutabilityPlanClassName()
				)
		);

		assertThatThrownBy( () -> restoreSnapshot(
				new MappingResolutionSnapshot( recipes, snapshot.environmentFingerprint() ),
				metadata
		) )
				.isInstanceOf( IllegalStateException.class )
				.hasMessageContaining( converterEntry.getKey().getFullPath() )
				.hasMessageContaining( "attribute converter" )
				.hasMessageContaining( "org.example.MissingConverter" );
	}

	private static void restoreSnapshot(MappingResolutionSnapshot snapshot, MetadataImpl metadata) {
		final var buildingContext = metadata.getTypeConfiguration().getMetadataBuildingContext();
		snapshot.restore(
				metadata,
				buildingContext.getServiceComponents(),
				new MappingResolutionState(
						metadata,
						metadata.getDatabase(),
						metadata.getMappingResolutionOptions(),
						buildingContext.getTypeDefinitionRegistry()
				),
				buildingContext
		);
	}

	@Test
	void restoredMetadataCanBuildSessionFactory(DomainModelScope scope) {
		final var archiveBytes = new ByteArrayOutputStream();
		MetadataSerialization.serialize( scope.getDomainModel() ).writeTo( archiveBytes );
		final var restored = MetadataSerialization.read( new ByteArrayInputStream( archiveBytes.toByteArray() ) ).restore(
				scope.getDomainModel().getMappingResolutionOptions().getServiceRegistry()
		);
		assertGenericValueResolution( restored.getMetadata(), GenericStringEntity.class );
		assertGenericValueResolution( restored.getMetadata(), GenericIntegerEntity.class );
		assertGenericMappedSuperclassResolution( restored.getMetadata() );

		try ( var sessionFactory = restored.buildSessionFactory() ) {
			final var genericEntityType = sessionFactory.getMetamodel().entity( GenericStringEntity.class );
			assertThat( ( (ManagedDomainType<?>) genericEntityType )
					.findConcreteGenericAttribute( "genericValue" ).getJavaType() ).isEqualTo( String.class );
			final var secondGenericEntityType = sessionFactory.getMetamodel().entity( GenericIntegerEntity.class );
			assertThat( ( (ManagedDomainType<?>) secondGenericEntityType )
					.findConcreteGenericAttribute( "genericValue" ).getJavaType() ).isEqualTo( Integer.class );
			final var genericIdType = sessionFactory.getMetamodel().entity( GenericIntegerIdEntity.class );
			assertThat( ( (ManagedDomainType<?>) genericIdType )
					.findConcreteGenericAttribute( "id" ).getJavaType() ).isEqualTo( Integer.class );
			final var genericAmountType = sessionFactory.getMetamodel().embeddable( GenericAmount.class );
			assertThat( ( (ManagedDomainType<?>) genericAmountType )
					.findConcreteGenericAttribute( "amount" ).getJavaType() ).isEqualTo( BigDecimal.class );
			assertThat( BootModelSerializationTest_.GenericMappedSuper_.id ).isNotNull();
			assertThat( BootModelSerializationTest_.GenericMappedSuper_.genericValue ).isNotNull();
			assertThat( BootModelSerializationTest_.GenericIdentifierMappedSuper_.id ).isNotNull();
			try ( var session = sessionFactory.openSession() ) {
				final var transaction = session.beginTransaction();
				final var publisher = new Publisher();
				publisher.id = 1;
				publisher.name = "Hibernate";
				final var book = new Book();
				book.title = "Hibernate ORM";
				book.slug = new Slug( "hibernate-orm" );
				book.author = new AuthorName( "Steve", "Ebersole" );
				book.publisher = publisher;
				book.isbn = new Isbn();
				book.isbn.groupCode = "978";
				book.isbn.publisherCode = "Red Hat";
				book.publicationDetails = new PublicationDetails();
				book.publicationDetails.imprint = "Hibernate";
				book.tags = new TreeSet<>( List.of( "orm", "java" ) );
				book.customTags = new HeadList<>();
				book.customTags.addAll( List.of( "archive", "type" ) );
				session.persist( publisher );
				session.persist( book );
				session.persist( new TableGeneratedEntity() );
				session.persist( new CustomGeneratedEntity() );
				session.persist( new GenericGeneratedEntity() );
				transaction.commit();
			}
			try ( var session = sessionFactory.openSession() ) {
				final Book book = session.createQuery( "from Book", Book.class ).getSingleResult();
				assertThat( book.title ).isEqualTo( "Hibernate ORM" );
				assertThat( book.slug ).isEqualTo( new Slug( "hibernate-orm" ) );
				assertThat( book.generatedValue ).isEqualTo( "archive-restored" );
				assertThat( book.generatedAt ).isNotNull();
				assertThat( book.author ).isEqualTo( new AuthorName( "Steve", "Ebersole" ) );
				assertThat( book.tags ).containsExactly( "orm", "java" );
				assertThat( book.customTags ).containsExactly( "archive", "type" );
			}
		}
	}

	private static void assertGenericMappedSuperclassResolution(Metadata metadata) {
		final var resolvedMapping = (ResolvedMappingImplementor) metadata;
		boolean found = false;
		for ( var mappedSuperclass : resolvedMapping.getResolvedMapping().metadata().getMappedSuperclassMappingsCopy() ) {
			for ( var property : mappedSuperclass.getDeclaredProperties() ) {
				if ( property.getName().equals( "genericValue" ) ) {
					found = true;
					assertThat( ( (BasicValue) property.getValue() ).getResolution() )
							.as(
									"mapped-superclass declaration resolution for %s on %s",
									mappedSuperclass.getClassName(),
									mappedSuperclass.getSuperPersistentClass()
							)
							.isNotNull();
				}
			}
		}
		assertThat( found ).isTrue();
	}

	private static void assertGenericValueResolution(Metadata metadata, Class<?> entityClass) {
		final var resolvedMapping = (ResolvedMappingImplementor) metadata;
		final var binding = resolvedMapping.getResolvedMapping().metadata().getEntityBinding( entityClass.getName() );
		final var property = binding.getProperty( "genericValue" );
		assertThat( ( (BasicValue) property.getValue() ).getResolution() )
				.as( "resolution for %s at %s", entityClass.getName(), property.getMappingRole() )
				.isNotNull();
		for ( var declaredProperty : binding.getDeclaredProperties() ) {
			if ( declaredProperty.getName().equals( "genericValue" ) ) {
				assertThat( ( (BasicValue) declaredProperty.getValue() ).getResolution() )
						.as(
								"declared resolution for %s at %s (generic=%s, specialization=%s)",
								entityClass.getName(),
								declaredProperty.getMappingRole(),
								declaredProperty.isGeneric(),
								declaredProperty.isGenericSpecialization()
						)
						.isNotNull();
			}
		}
	}

	@Test
	void metadataCanBeRestoredFromExplicitSerialForm(DomainModelScope scope) {
		final MetadataImplementor domainModel = scope.getDomainModel();
		final var serialForm = MetadataSerialization.serialize( domainModel );
		final var archiveBytes = new ByteArrayOutputStream();
		serialForm.writeTo( archiveBytes );

		final var readSerialForm = MetadataSerialization.read(
				new ByteArrayInputStream( archiveBytes.toByteArray() )
		);
		final var restoredProduct = readSerialForm.restore(
				domainModel.getMappingResolutionOptions().getServiceRegistry()
		);
		final var restoredView = restoredProduct.getMetadata();
		assertThat( restoredView ).isInstanceOf( ResolvedMappingImplementor.class );
		final var restoredResolvedMapping = (ResolvedMappingImplementor) restoredView;
		final MetadataImpl restored = (MetadataImpl) restoredResolvedMapping.getResolvedMapping().metadata();
		assertThat( MappingModelGraphIndex.from( restored ).basicValuesByRole().values() )
				.flatExtracting( values -> values )
				.allSatisfy( value -> assertThat( value.getResolution() ).isNotNull() );

		final PersistentClass originalBook = domainModel.getEntityBindings()
				.stream()
				.filter( binding -> binding.getClassName().equals( Book.class.getName() ) )
				.findFirst()
				.orElseThrow();
		final PersistentClass restoredBook = restored.getEntityBinding( originalBook.getEntityName() );
		final var restoredTitle = (BasicValue) restoredBook.getProperty( "title" ).getValue();
		assertThat( restoredTitle.getJpaAttributeConverterDescriptor() ).isNull();
		assertThat( restoredTitle.resolve().getValueConverter() ).isNotNull();
		assertThat( restoredTitle.resolve().getDomainJavaType().getJavaTypeClass() ).isEqualTo( String.class );
		assertThat( restoredTitle.resolve().getRelationalJavaType().getJavaTypeClass() ).isEqualTo( String.class );
		assertThat( restoredBook.getTable().getName() ).isEqualTo( originalBook.getTable().getName() );
		assertThat( restoredBook.getClassDetails() ).isNotSameAs( originalBook.getClassDetails() );
		assertThat( restoredBook.getClassDetails().getClassName() )
				.isEqualTo( Book.class.getName() );
		assertThat( restored.getBootstrapContext().getModelsContext().getClassDetailsRegistry()
				.findClassDetails( Book.class.getName() ) )
				.isSameAs( restoredBook.getClassDetails() );
		assertThat( restored.getNamedHqlQueryMapping( "books" ).getRegistrationName() ).isEqualTo( "books" );
		assertThat( restored.getNamedNativeQueryMapping( "nativeBooks" ).getRegistrationName() ).isEqualTo( "nativeBooks" );
		assertThat( restored.getNamedProcedureCallMapping( "bookProcedure" ).getProcedureName() ).isEqualTo( "find_book" );
		assertThat( restored.getFetchProfile( "book.publisher" ).getFetches() ).hasSize( 1 );
		assertThat( restored.getFilterDefinition( "titleFilter" ).getParameterNames() )
				.containsExactlyInAnyOrder( "title", "slug" );
		assertThat( restored.getFilterDefinition( "titleFilter" ).getParameterResolver( "title" ).get() )
				.isEqualTo( "Hibernate" );
		assertThat( restored.getFilterDefinition( "titleFilter" ).getParameterJdbcMapping( "slug" ) )
				.isInstanceOfSatisfying(
						org.hibernate.type.CustomType.class,
						mapping -> assertThat( mapping.getJavaTypeDescriptor().getJavaTypeClass() )
								.isEqualTo( Slug.class )
				);
		assertThat( restored.getResultSetMapping( "book-title" ) ).isNotNull();
		assertThat( restored.getNamedEntityGraph( "book.graph" ) ).isNotNull();
		assertThat( restored.getCollectionBinding( Book.class.getName() + ".tags" ).getComparator() )
				.isInstanceOf( ReverseComparator.class );
		assertThat( restored.getCollectionBinding( Book.class.getName() + ".customTags" ).getCollectionType() )
				.isInstanceOfSatisfying(
						org.hibernate.type.CustomCollectionType.class,
						type -> assertThat( type.getUserType() ).isInstanceOf( HeadListType.class )
				);
		final var restoredPublicationDetails = restored.getComposites().stream()
				.filter( component -> PublicationDetails.class.getName().equals( component.getComponentClassName() ) )
				.findFirst()
				.orElseThrow();
		assertThat( restoredPublicationDetails.getDiscriminatorType() ).isNotNull();
		assertThat( restored.getEmbeddableDiscriminatorTypesMap() )
				.containsEntry( PublicationDetails.class, restoredPublicationDetails.getDiscriminatorType() );
		final var restoredCompositeUserType =
				( (org.hibernate.mapping.Component) restoredBook.getProperty( "author" ).getValue() )
						.getCompositeUserType();
		assertThat( restoredCompositeUserType ).isInstanceOf( AuthorNameCompositeUserType.class );
		assertThat( restored.getDatabase().getNamespaces() )
				.flatExtracting( namespace -> namespace.getTables() )
				.anyMatch( table -> table.getName().equals( "table_identifier" ) );
		final var customGeneratedBinding =
				(RootClass) restored.getEntityBinding( CustomGeneratedEntity.class.getName() );
		assertThat( customGeneratedBinding.getIdentifier() )
				.isInstanceOfSatisfying( org.hibernate.mapping.SimpleValue.class, identifier ->
						assertThat( identifier.getCustomIdGeneratorCreator() ).isNotNull() );
		final var genericGeneratedBinding =
				(RootClass) restored.getEntityBinding( GenericGeneratedEntity.class.getName() );
		assertThat( genericGeneratedBinding.getIdentifier() )
				.isInstanceOfSatisfying( org.hibernate.mapping.SimpleValue.class, identifier ->
						assertThat( identifier.getCustomIdGeneratorCreator() ).isNotNull() );

		assertThat( createIdentifierGenerator( customGeneratedBinding, restored ) )
				.isInstanceOf( CustomIdGenerator.class );
	}

	private static org.hibernate.generator.Generator createIdentifierGenerator(
			RootClass entityBinding,
			MetadataImpl metadata) {
		return GeneratorSettingsImpl.createIdentifierGenerator(
				entityBinding.getIdentifier(),
				metadata.getDatabase().getDialect(),
				entityBinding,
				entityBinding.getIdentifierProperty(),
				metadata
		);
	}

	@Entity(name = "Publisher")
	public static class Publisher {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name = "Book")
	@NamedQuery(name = "books", query = "from Book")
	@NamedNativeQuery(name = "nativeBooks", query = "select title from Book", resultSetMapping = "book-title")
	@NamedStoredProcedureQuery(
			name = "bookProcedure",
			procedureName = "find_book",
			parameters = @StoredProcedureParameter(name = "id", type = Integer.class, mode = ParameterMode.IN)
	)
	@NamedEntityGraph(name = "book.graph", attributeNodes = @NamedAttributeNode("publisher"))
	@SqlResultSetMapping(
			name = "book-title",
			columns = @ColumnResult(name = "title"),
			classes = @ConstructorResult(
					targetClass = TitleProjection.class,
					columns = @ColumnResult(name = "title")
			),
			entities = @EntityResult(
					entityClass = Book.class,
					fields = @FieldResult(name = "title", column = "title")
			)
	)
	@FetchProfile(name = "book.publisher", fetchOverrides = @FetchProfile.FetchOverride(
			entity = Book.class,
			association = "publisher",
			mode = FetchMode.JOIN
	))
	@FilterDef(name = "titleFilter", parameters = {
			@ParamDef(name = "title", type = String.class, resolver = TitleSupplier.class),
			@ParamDef(name = "slug", type = SlugUserType.class)
	})
	public static class Book {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Integer id;
		@Convert(converter = TitleConverter.class)
		private String title;
		@Type(SlugUserType.class)
		private Slug slug;
		@ArchiveGenerated("archive-restored")
		private String generatedValue;
		@DatabaseGenerated
		private Instant generatedAt;
		@CompositeType(AuthorNameCompositeUserType.class)
		private AuthorName author;
		@Embedded
		private Isbn isbn;
		@Embedded
		private PublicationDetails publicationDetails;
		@ManyToOne
		private Publisher publisher;
		@ElementCollection
		@SortComparator(ReverseComparator.class)
		private SortedSet<String> tags;
		@ElementCollection
		@CollectionType(type = HeadListType.class)
		private IHeadList<String> customTags;
	}

	public static class TitleSupplier implements Supplier<String> {
		@Override
		public String get() {
			return "Hibernate";
		}
	}

	public record TitleProjection(String title) {
	}

	public static class TitleConverter implements AttributeConverter<CharSequence, String> {
		@Override
		public String convertToDatabaseColumn(CharSequence attribute) {
			return attribute == null ? null : attribute.toString();
		}

		@Override
		public CharSequence convertToEntityAttribute(String dbData) {
			return dbData;
		}
	}

	public record Slug(String value) implements Serializable {
	}

	public static class SlugUserType implements UserType<Slug> {
		@Override
		public int getSqlType() {
			return Types.VARCHAR;
		}

		@Override
		public Class<Slug> returnedClass() {
			return Slug.class;
		}

		@Override
		public Slug nullSafeGet(
				java.sql.ResultSet resultSet,
				int position,
				WrapperOptions options) throws java.sql.SQLException {
			final String value = resultSet.getString( position );
			return value == null ? null : new Slug( value );
		}

		@Override
		public void nullSafeSet(
				java.sql.PreparedStatement statement,
				Slug value,
				int index,
				WrapperOptions options) throws java.sql.SQLException {
			if ( value == null ) {
				statement.setNull( index, Types.VARCHAR );
			}
			else {
				statement.setString( index, value.value() );
			}
		}

		@Override
		public Slug deepCopy(Slug value) {
			return value;
		}

		@Override
		public boolean isMutable() {
			return false;
		}
	}

	@ValueGenerationType(generatedBy = ArchiveValueGenerator.class)
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ java.lang.annotation.ElementType.FIELD, java.lang.annotation.ElementType.METHOD })
	public @interface ArchiveGenerated {
		String value();
	}

	public static class ArchiveValueGenerator implements BeforeExecutionGenerator {
		private final String value;

		public ArchiveValueGenerator(ArchiveGenerated configuration) {
			value = configuration.value();
		}

		@Override
		public Object generate(
				SharedSessionContractImplementor session,
				Object owner,
				Object currentValue,
				EventType eventType) {
			return value;
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			return EventTypeSets.INSERT_ONLY;
		}
	}

	@ValueGenerationType(generatedBy = DatabaseValueGenerator.class)
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ java.lang.annotation.ElementType.FIELD, java.lang.annotation.ElementType.METHOD })
	public @interface DatabaseGenerated {
	}

	public static class DatabaseValueGenerator implements OnExecutionGenerator {
		@Override
		public EnumSet<EventType> getEventTypes() {
			return EventTypeSets.INSERT_ONLY;
		}

		@Override
		public boolean referenceColumnsInSql(Dialect dialect) {
			return true;
		}

		@Override
		public boolean writePropertyValue() {
			return false;
		}

		@Override
		public String[] getReferencedColumnValues(Dialect dialect) {
			return new String[] { dialect.currentTimestamp() };
		}
	}

	public static class ReverseComparator implements Comparator<String> {
		@Override
		public int compare(String first, String second) {
			return second.compareTo( first );
		}
	}

	public record AuthorName(String firstName, String lastName) implements Serializable {
	}

	public static class AuthorNameMapper {
		private String firstName;
		private String lastName;
	}

	public static class AuthorNameCompositeUserType implements CompositeUserType<AuthorName> {
		@Override
		public Object getPropertyValue(AuthorName component, int property) {
			return switch ( property ) {
				case 0 -> component.firstName();
				case 1 -> component.lastName();
				default -> throw new IllegalArgumentException( "Unexpected property index " + property );
			};
		}

		@Override
		public AuthorName instantiate(ValueAccess values) {
			return new AuthorName(
					values.getValue( 0, String.class ),
					values.getValue( 1, String.class )
			);
		}

		@Override
		public Class<?> embeddable() {
			return AuthorNameMapper.class;
		}

		@Override
		public Class<AuthorName> returnedClass() {
			return AuthorName.class;
		}

		@Override
		public boolean equals(AuthorName x, AuthorName y) {
			return java.util.Objects.equals( x, y );
		}

		@Override
		public int hashCode(AuthorName x) {
			return x.hashCode();
		}

		@Override
		public AuthorName deepCopy(AuthorName value) {
			return value;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public Serializable disassemble(AuthorName value) {
			return value;
		}

		@Override
		public AuthorName assemble(Serializable cached, Object owner) {
			return (AuthorName) cached;
		}

		@Override
		public AuthorName replace(AuthorName detached, AuthorName managed, Object owner) {
			return detached;
		}
	}

	@Embeddable
	public static class Isbn {
		private String groupCode;
		private String publisherCode;
	}

	@Entity(name = "TableGeneratedEntity")
	public static class TableGeneratedEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "table-generator")
		@jakarta.persistence.TableGenerator(
				name = "table-generator",
				table = "table_identifier",
				pkColumnName = "table_name",
				valueColumnName = "next_id",
				allocationSize = 5
		)
		private Long id;
	}

	@Entity
	public static class CustomGeneratedEntity {
		@Id
		@CustomGenerated(41)
		private Long id;
	}

	@MappedSuperclass
	public static class GenericGeneratedBase {
		@Id
		@GenericGenerator(
				type = org.hibernate.id.IncrementGenerator.class,
				parameters = @Parameter(name = "initial_value", value = "5")
		)
		private Long id;
	}

	@Entity
	public static class GenericGeneratedEntity extends GenericGeneratedBase {
	}

	@MappedSuperclass
	public static class GenericMappedSuper<T> {
		@Id
		private Integer id;
		private T genericValue;
	}

	@Entity
	public static class GenericStringEntity extends GenericMappedSuper<String> {
	}

	@Entity
	public static class GenericIntegerEntity extends GenericMappedSuper<Integer> {
	}

	@MappedSuperclass
	public static class GenericIdentifierMappedSuper<T> {
		@Id
		private T id;
	}

	@Entity
	public static class GenericIntegerIdEntity extends GenericIdentifierMappedSuper<Integer> {
	}

	@Entity
	public static class GenericAmountEntity {
		@Id
		private Integer id;
		@Embedded
		private GenericAmount amount;
	}

	@MappedSuperclass
	public static class GenericAmountBase<T extends Number> {
		private T amount;
	}

	@Embeddable
	public static class GenericAmount extends GenericAmountBase<BigDecimal> {
	}

	@IdGeneratorType(CustomIdGenerator.class)
	@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
	@Target({ java.lang.annotation.ElementType.FIELD, java.lang.annotation.ElementType.METHOD })
	public @interface CustomGenerated {
		long value();
	}

	public static class CustomIdGenerator implements BeforeExecutionGenerator {
		private final long value;

		public CustomIdGenerator(CustomGenerated configuration) {
			value = configuration.value();
		}

		@Override
		public Object generate(
				SharedSessionContractImplementor session,
				Object owner,
				Object currentValue,
				EventType eventType) {
			return value;
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			return EventTypeSets.INSERT_ONLY;
		}
	}

	@Embeddable
	@DiscriminatorValue("publication")
	@DiscriminatorColumn(name = "publication_type")
	public static class PublicationDetails {
		private String imprint;
	}

	@Embeddable
	@DiscriminatorValue("special")
	public static class SpecialPublicationDetails extends PublicationDetails {
		private String edition;
	}
}
