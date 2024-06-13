/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.DuplicateMappingException;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.annotations.CollectionTypeRegistration;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.boot.internal.NamedProcedureCallDefinitionImpl;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.NamedEntityGraphDefinition;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.TypeDefinitionRegistry;
import org.hibernate.boot.model.convert.spi.ConverterAutoApplyHandler;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterRegistry;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.model.internal.AnnotatedClassType;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.source.spi.LocalMetadataBuildingContext;
import org.hibernate.boot.query.NamedHqlQueryDefinition;
import org.hibernate.boot.query.NamedNativeQueryDefinition;
import org.hibernate.boot.query.NamedProcedureCallDefinition;
import org.hibernate.boot.query.NamedResultSetMappingDescriptor;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserCollectionType;
import org.hibernate.usertype.UserType;

import jakarta.persistence.AttributeConverter;

/**
 * An in-flight representation of {@link org.hibernate.boot.Metadata} while it is being built.
 *
 * @author Steve Ebersole
 *
 * @since 5.0
 */
public interface InFlightMetadataCollector extends MetadataImplementor {
	BootstrapContext getBootstrapContext();

	/**
	 * Add the {@link PersistentClass} for an entity mapping.
	 *
	 * @param persistentClass The entity metadata
	 *
	 * @throws DuplicateMappingException Indicates there was already an entry
	 * corresponding to the given entity name.
	 */
	void addEntityBinding(PersistentClass persistentClass) throws DuplicateMappingException;

	/**
	 * A map of {@link PersistentClass} by entity name.
	 * Needed for {@link SecondPass} handling.
	 */
	Map<String, PersistentClass> getEntityBindingMap();

	void registerComponent(Component component);

	void registerGenericComponent(Component component);

	void registerEmbeddableSubclass(XClass superclass, XClass subclass);

	List<XClass> getEmbeddableSubclasses(XClass superclass);

	/**
	 * Adds an import (for use in HQL).
	 *
	 * @param importName The name to be used in HQL
	 * @param className The fully-qualified name of the class
	 *
	 * @throws DuplicateMappingException If className already is mapped to another
	 * entity name in this repository.
	 */
	void addImport(String importName, String className) throws DuplicateMappingException;

	/**
	 * Add collection mapping metadata to this repository.
	 *
	 * @param collection The collection metadata
	 *
	 * @throws DuplicateMappingException Indicates there was already an entry
	 * corresponding to the given collection role
	 */
	void addCollectionBinding(Collection collection) throws DuplicateMappingException;

	/**
	 * Adds table metadata to this repository returning the created
	 * metadata instance.
	 *
	 * @param schema The named schema in which the table belongs (or null).
	 * @param catalog The named catalog in which the table belongs (or null).
	 * @param name The table name
	 * @param subselect A select statement which defines a logical table, much
	 * like a DB view.
	 * @param isAbstract Is the table abstract (i.e. not really existing in the DB)?
	 *
	 * @return The created table metadata, or the existing reference.
	 */
	Table addTable(
			String schema,
			String catalog,
			String name,
			String subselect,
			boolean isAbstract,
			MetadataBuildingContext buildingContext);

	/**
	 * Adds a 'denormalized table' to this repository.
	 *
	 * @param schema The named schema in which the table belongs (or null).
	 * @param catalog The named catalog in which the table belongs (or null).
	 * @param name The table name
	 * @param isAbstract Is the table abstract (i.e. not really existing in the DB)?
	 * @param subselect A select statement which defines a logical table, much
	 * like a DB view.
	 * @param includedTable The "common" table
	 *
	 * @return The created table metadata.
	 *
	 * @throws DuplicateMappingException If such a table mapping already exists.
	 */
	Table addDenormalizedTable(
			String schema,
			String catalog,
			String name,
			boolean isAbstract,
			String subselect,
			Table includedTable,
			MetadataBuildingContext buildingContext) throws DuplicateMappingException;

	/**
	 * Adds metadata for a named query to this repository.
	 *
	 * @param query The metadata
	 *
	 * @throws DuplicateMappingException If a query already exists with that name.
	 */
	void addNamedQuery(NamedHqlQueryDefinition query) throws DuplicateMappingException;

	/**
	 * Adds metadata for a named SQL query to this collector.
	 */
	void addNamedNativeQuery(NamedNativeQueryDefinition query) throws DuplicateMappingException;

	/**
	 * Adds the metadata for a named SQL result set mapping to this collector.
	 */
	void addResultSetMapping(NamedResultSetMappingDescriptor resultSetMappingDefinition) throws DuplicateMappingException;

	/**
	 * Adds metadata for a named stored procedure call to this collector.
	 */
	void addNamedProcedureCallDefinition(NamedProcedureCallDefinition definition) throws DuplicateMappingException;

	/**
	 * Adds metadata for a named entity graph to this repository
	 *
	 * @param namedEntityGraphDefinition The procedure call information
	 *
	 * @throws DuplicateMappingException If an entity graph already exists with that name.
	 */
	void addNamedEntityGraph(NamedEntityGraphDefinition namedEntityGraphDefinition);

	/**
	 * Adds a type definition to this metadata repository.
	 *
	 * @param typeDefinition The named type definition to add.
	 *
	 * @throws DuplicateMappingException If a {@link TypeDefinition} already exists with that name.
	 *
	 * @deprecated Use {@link #getTypeDefinitionRegistry()} instead
	 *
	 * @see #getTypeDefinitionRegistry()
	 */
	@Deprecated
	void addTypeDefinition(TypeDefinition typeDefinition);

	/**
	 * Access to the {@link TypeDefinitionRegistry}, which may be used to add
	 * type definitions to this metadata repository.
	 */
	TypeDefinitionRegistry getTypeDefinitionRegistry();

	/**
	 * Adds a filter definition to this repository.
	 *
	 * @param definition The filter definition to add.
	 *
	 * @throws DuplicateMappingException If a {@link FilterDefinition} already exists with that name.
	 */
	void addFilterDefinition(FilterDefinition definition);

	/**
	 * Add metadata pertaining to an auxiliary database object to this repository.
	 *
	 * @param auxiliaryDatabaseObject The metadata.
	 */
	void addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject);

	/**
	 * Add a {@link FetchProfile}.
	 */
	void addFetchProfile(FetchProfile profile);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// make sure these are account for better in metamodel

	void addIdentifierGenerator(IdentifierGeneratorDefinition generatorDefinition);

	/**
	 * Obtain the {@link ConverterRegistry} which may be
	 * used to register {@link AttributeConverter}s.
	 */
	ConverterRegistry getConverterRegistry();

	/**
	 * Apply the descriptor for an {@link AttributeConverter}
	 *
	 * @deprecated use {@link #getConverterRegistry()}
	 */
	@Deprecated(since = "6.2")
	void addAttributeConverter(ConverterDescriptor descriptor);

	/**
	 * Apply an {@link AttributeConverter}
	 *
	 * @deprecated use {@link #getConverterRegistry()}
	 */
	@Deprecated(since = "6.2")
	void addAttributeConverter(Class<? extends AttributeConverter<?,?>> converterClass);

	/**
	 * @deprecated use {@link #getConverterRegistry()}
	 */
	@Deprecated(since = "6.2")
	void addRegisteredConversion(RegisteredConversion conversion);

	/**
	 * @deprecated use {@link #getConverterRegistry()}
	 */
	@Deprecated(since = "6.2")
	ConverterAutoApplyHandler getAttributeConverterAutoApplyHandler();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// second passes

	void addSecondPass(SecondPass secondPass);

	void addSecondPass(SecondPass sp, boolean onTopOfTheQueue);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// stuff needed for annotation binding :(

	void addTableNameBinding(Identifier logicalName, Table table);
	void addTableNameBinding(
			String schema,
			String catalog,
			String logicalName,
			String realTableName,
			Table denormalizedSuperTable);
	String getLogicalTableName(Table ownerTable);
	String getPhysicalTableName(Identifier logicalName);
	String getPhysicalTableName(String logicalName);

	void addColumnNameBinding(Table table, Identifier logicalColumnName, Column column);
	void addColumnNameBinding(Table table, String logicalColumnName, Column column);
	String getPhysicalColumnName(Table table, Identifier logicalName) throws MappingException;
	String getPhysicalColumnName(Table table, String logicalName) throws MappingException;
	String getLogicalColumnName(Table table, Identifier physicalName);
	String getLogicalColumnName(Table table, String physicalName);

	void addDefaultIdentifierGenerator(IdentifierGeneratorDefinition generatorDefinition);

	void addDefaultQuery(NamedHqlQueryDefinition queryDefinition);

	void addDefaultNamedNativeQuery(NamedNativeQueryDefinition query);

	void addDefaultResultSetMapping(NamedResultSetMappingDescriptor definition);

	void addDefaultNamedProcedureCall(NamedProcedureCallDefinitionImpl procedureCallDefinition);

	AnnotatedClassType addClassType(XClass clazz);
	AnnotatedClassType getClassType(XClass clazz);

	void addMappedSuperclass(Class<?> type, MappedSuperclass mappedSuperclass);
	MappedSuperclass getMappedSuperclass(Class<?> type);

	PropertyData getPropertyAnnotatedWithMapsId(XClass persistentXClass, String propertyName);
	void addPropertyAnnotatedWithMapsId(XClass entity, PropertyData propertyAnnotatedElement);
	void addPropertyAnnotatedWithMapsIdSpecj(XClass entity, PropertyData specJPropertyData, String s);

	void addToOneAndIdProperty(XClass entity, PropertyData propertyAnnotatedElement);
	PropertyData getPropertyAnnotatedWithIdAndToOne(XClass persistentXClass, String propertyName);

	boolean isInSecondPass();

	NaturalIdUniqueKeyBinder locateNaturalIdUniqueKeyBinder(String entityName);
	void registerNaturalIdUniqueKeyBinder(String entityName, NaturalIdUniqueKeyBinder ukBinder);

	void registerValueMappingResolver(Function<MetadataBuildingContext,Boolean> resolver);

	void addJavaTypeRegistration(Class<?> javaType, JavaType<?> jtd);
	void addJdbcTypeRegistration(int typeCode, JdbcType jdbcType);

	void registerEmbeddableInstantiator(Class<?> embeddableType, Class<? extends EmbeddableInstantiator> instantiator);
	Class<? extends EmbeddableInstantiator> findRegisteredEmbeddableInstantiator(Class<?> embeddableType);

	void registerCompositeUserType(Class<?> embeddableType, Class<? extends CompositeUserType<?>> userType);
	Class<? extends CompositeUserType<?>> findRegisteredCompositeUserType(Class<?> embeddableType);

	void registerUserType(Class<?> embeddableType, Class<? extends UserType<?>> userType);
	Class<? extends UserType<?>> findRegisteredUserType(Class<?> basicType);

	void addCollectionTypeRegistration(CollectionTypeRegistration registrationAnnotation);
	void addCollectionTypeRegistration(CollectionClassification classification, CollectionTypeRegistrationDescriptor descriptor);
	CollectionTypeRegistrationDescriptor findCollectionTypeRegistration(CollectionClassification classification);

	interface DelayedPropertyReferenceHandler extends Serializable {
		void process(InFlightMetadataCollector metadataCollector);
	}
	void addDelayedPropertyReferenceHandler(DelayedPropertyReferenceHandler handler);
	void addPropertyReference(String entityName, String propertyName);
	void addUniquePropertyReference(String entityName, String propertyName);

	void addPropertyReferencedAssociation(String s, String propertyName, String syntheticPropertyName);
	String getPropertyReferencedAssociation(String entityName, String mappedBy);

	void addMappedBy(String name, String mappedBy, String propertyName);
	String getFromMappedBy(String ownerEntityName, String propertyName);

	interface EntityTableXref {
		void addSecondaryTable(LocalMetadataBuildingContext buildingContext, Identifier logicalName, Join secondaryTableJoin);
		void addSecondaryTable(QualifiedTableName logicalName, Join secondaryTableJoin);
		Table resolveTable(Identifier tableName);
		Table getPrimaryTable();
		Join locateJoin(Identifier tableName);
	}

	class DuplicateSecondaryTableException extends HibernateException {
		private final Identifier tableName;

		public DuplicateSecondaryTableException(Identifier tableName) {
			super(
					String.format(
							Locale.ENGLISH,
							"Table with that name [%s] already associated with entity",
							tableName.render()
					)
			);
			this.tableName = tableName;
		}
	}

	EntityTableXref getEntityTableXref(String entityName);
	EntityTableXref addEntityTableXref(
			String entityName,
			Identifier primaryTableLogicalName,
			Table primaryTable,
			EntityTableXref superEntityTableXref);
	Map<String,Join> getJoins(String entityName);


	class CollectionTypeRegistrationDescriptor {
		private final Class<? extends UserCollectionType> implementation;
		private final Map<String,String> parameters;

		public CollectionTypeRegistrationDescriptor(Class<? extends UserCollectionType> implementation, Map<String,String> parameters) {
			this.implementation = implementation;
			this.parameters = parameters;
		}

		public Class<? extends UserCollectionType> getImplementation() {
			return implementation;
		}

		public Map<String,String> getParameters() {
			return parameters;
		}
	}
}
