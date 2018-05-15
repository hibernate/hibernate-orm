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
import javax.persistence.AttributeConverter;

import org.hibernate.DuplicateMappingException;
import org.hibernate.HibernateException;
import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterAutoApplyHandler;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.domain.EntityMappingHierarchy;
import org.hibernate.boot.model.domain.ResolutionContext;
import org.hibernate.boot.model.query.spi.NamedHqlQueryDefinition;
import org.hibernate.boot.model.query.spi.NamedNativeQueryDefinition;
import org.hibernate.boot.model.query.spi.NamedQueryDefinition;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.MappedAuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.model.resultset.spi.ResultSetMappingDefinition;
import org.hibernate.boot.model.source.spi.LocalMetadataBuildingContext;
import org.hibernate.cfg.AnnotatedClassType;
import org.hibernate.cfg.JPAIndexHolder;
import org.hibernate.cfg.PropertyData;
import org.hibernate.cfg.SecondPass;
import org.hibernate.cfg.UniqueConstraintHolder;
import org.hibernate.cfg.annotations.NamedEntityGraphDefinition;
import org.hibernate.cfg.annotations.NamedProcedureCallDefinition;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.naming.Identifier;
import org.hibernate.type.spi.BasicType;

/**
 * An in-flight representation of Metadata while Metadata is being built.
 *
 * @author Steve Ebersole
 *
 * @since 5.0
 */
public interface InFlightMetadataCollector extends MetadataImplementor {
	BootstrapContext getBootstrapContext();

	Database getDatabase();

	/**
	 * Add the EntityMappingHierarchy for an entity mapping hierarchy.
	 *
	 * @param entityMappingHierarchy The entity hierarchy metadata
	 *
	 * @throws DuplicateMappingException Indicates there was already an entry
	 * corresponding to the given entity name.
	 */
	void addEntityMappingHierarchy(EntityMappingHierarchy entityMappingHierarchy);

	/**
	 * Add the PersistentClass for an entity mapping.
	 *
	 * @param persistentClass The entity metadata
	 *
	 * @throws DuplicateMappingException Indicates there was already an entry
	 * corresponding to the given entity name.
	 */
	void addEntityBinding(PersistentClass persistentClass) throws DuplicateMappingException;

	/**
	 * Needed for SecondPass handling
	 */
	Map<String, PersistentClass> getEntityBindingMap();

	/**
	 * Adds an import (HQL entity rename).
	 *
	 * @param entityName The entity name being renamed.
	 * @param rename The rename
	 *
	 * @throws DuplicateMappingException If rename already is mapped to another
	 * entity name in this repository.
	 */
	void addImport(String entityName, String rename) throws DuplicateMappingException;

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
	MappedTable addTable(String schema, String catalog, String name, String subselect, boolean isAbstract);

	/**
	 * Adds a 'denormalized table' to this repository.
	 *
	 * @param schema The named schema in which the table belongs (or null).
	 * @param catalog The named catalog in which the table belongs (or null).
	 * @param name The table name
	 * @param isAbstract Is the table abstract (i.e. not really existing in the DB)?
	 * @param subselect A select statement which defines a logical table, much
	 * like a DB view.
	 * @param includedTable ???
	 *
	 * @return The created table metadata.
	 *
	 * @throws DuplicateMappingException If such a table mapping already exists.
	 */
	MappedTable addDenormalizedTable(
			String schema,
			String catalog,
			String name,
			boolean isAbstract,
			String subselect,
			MappedTable includedTable) throws DuplicateMappingException;

	/**
	 * Adds metadata for a named query definition to this repository.
	 *
	 * @param queryDefinition The metadata
	 *
	 * @throws DuplicateMappingException If a queryDefinition already exists with that name.
	 * @deprecated Use {@link #addNamedHqlQuery} instead
	 */
	@Deprecated
	default void addNamedQuery(NamedQueryDefinition queryDefinition) throws DuplicateMappingException {
		addNamedHqlQuery( (NamedHqlQueryDefinition) queryDefinition );
	}

	/**
	 * Adds metadata for a named HQL query to this repository.
	 *
	 * @param queryDefinition The metadata
	 *
	 * @throws DuplicateMappingException If a query already exists with that name.
	 */
	void addNamedHqlQuery(NamedHqlQueryDefinition queryDefinition) throws DuplicateMappingException;

	/**
	 * Adds metadata for a named SQL query to this repository.
	 *
	 * @param query The metadata
	 *
	 * @throws DuplicateMappingException If a query already exists with that name.
	 */
	void addNamedNativeQuery(NamedNativeQueryDefinition query) throws DuplicateMappingException;

	/**
	 * Adds the metadata for a named SQL result set mapping to this repository.
	 *
	 * @param sqlResultSetMapping The metadata
	 *
	 * @throws DuplicateMappingException If metadata for another SQL result mapping was
	 * already found under the given name.
	 */
	void addResultSetMapping(ResultSetMappingDefinition sqlResultSetMapping) throws DuplicateMappingException;

	/**
	 * Adds metadata for a named stored procedure call to this repository.
	 *
	 * @param definition The procedure call information
	 *
	 * @throws DuplicateMappingException If a query already exists with that name.
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
	 * Adds a filter definition to this repository.
	 *
	 * @param definition The filter definition to add.
	 *
	 * @throws DuplicateMappingException If a FilterDefinition already exists with that name.
	 */
	void addFilterDefinition(FilterDefinition definition);

	/**
	 * Add metadata pertaining to an auxiliary database object to this repository.
	 *
	 * @param auxiliaryDatabaseObject The metadata.
	 */
	void addAuxiliaryDatabaseObject(MappedAuxiliaryDatabaseObject auxiliaryDatabaseObject);

	void addFetchProfile(FetchProfile profile);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// make sure these are account for better in metamodel

	void addIdentifierGenerator(IdentifierGeneratorDefinition generatorDefinition);

	void addAttributeConverter(ConverterDescriptor descriptor);

	default <O, R> void addAttributeConverter(Class<? extends AttributeConverter<O, R>> converterClass) {
		addAttributeConverter( new ClassBasedConverterDescriptor( converterClass, getBootstrapContext().getClassmateContext() ) );
	}

	ConverterAutoApplyHandler getAttributeConverterAutoApplyHandler();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// second passes

	void addSecondPass(SecondPass secondPass);

	void addSecondPass(SecondPass sp, boolean onTopOfTheQueue);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// stuff needed for annotation binding :(

	void addDefaultIdentifierGenerator(IdentifierGeneratorDefinition generatorDefinition);

	/**
	 * @deprecated Use {@link #addDefaultNamedHqlQuery} instead
	 */
	@Deprecated
	default void addDefaultQuery(NamedQueryDefinition queryDefinition) {
		addDefaultNamedHqlQuery( (NamedHqlQueryDefinition) queryDefinition );
	}

	void addDefaultNamedHqlQuery(NamedHqlQueryDefinition queryDefinition);

	void addDefaultNamedNativeQuery(NamedNativeQueryDefinition query);

	void addDefaultResultSetMapping(ResultSetMappingDefinition resultSetMapping);

	void addDefaultNamedProcedureCallDefinition(NamedProcedureCallDefinition procedureCallDefinition);

	void addAnyMetaDef(AnyMetaDef defAnn);
	AnyMetaDef getAnyMetaDef(String anyMetaDefName);

	AnnotatedClassType addClassType(XClass clazz);
	AnnotatedClassType getClassType(XClass clazz);

	void addMappedSuperclass(Class type, MappedSuperclass mappedSuperclass);
	MappedSuperclass getMappedSuperclass(Class type);

	PropertyData getPropertyAnnotatedWithMapsId(XClass persistentXClass, String propertyName);
	void addPropertyAnnotatedWithMapsId(XClass entity, PropertyData propertyAnnotatedElement);
	void addPropertyAnnotatedWithMapsIdSpecj(XClass entity, PropertyData specJPropertyData, String s);

	void addToOneAndIdProperty(XClass entity, PropertyData propertyAnnotatedElement);
	PropertyData getPropertyAnnotatedWithIdAndToOne(XClass persistentXClass, String propertyName);

	boolean isInSecondPass();

	NaturalIdUniqueKeyBinder locateNaturalIdUniqueKeyBinder(String entityName);
	void registerNaturalIdUniqueKeyBinder(String entityName, NaturalIdUniqueKeyBinder ukBinder);

	/**
	 * Performs the same function as the legacy TypeResolver#basic, essentially performing
	 * a resolution for BasicType using "registry keys".
	 */
	<T> BasicType<T> basicType(String registrationKey);


	void registerValueMappingResolver(Function<ResolutionContext,Boolean> resolver);
	List<Function<ResolutionContext,Boolean>> getValueMappingResolvers();

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

	void addUniqueConstraints(MappedTable table, List uniqueConstraints);
	void addUniqueConstraintHolders(MappedTable table, List<UniqueConstraintHolder> uniqueConstraints);
	void addJpaIndexHolders(MappedTable table, List<JPAIndexHolder> jpaIndexHolders);


	interface EntityTableXref {
		void addSecondaryTable(LocalMetadataBuildingContext buildingContext, Identifier logicalName, Join secondaryTableJoin);
		void addSecondaryTable(Identifier logicalName, Join secondaryTableJoin);
		MappedTable resolveTable(Identifier tableName);
		MappedTable getPrimaryTable();
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
			MappedTable primaryTable,
			EntityTableXref superEntityTableXref);
	Map<String,Join> getJoins(String entityName);
}
