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
import javax.persistence.AttributeConverter;

import org.hibernate.DuplicateMappingException;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.boot.internal.ClassmateContext;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.source.spi.LocalMetadataBuildingContext;
import org.hibernate.cfg.AnnotatedClassType;
import org.hibernate.cfg.AttributeConverterDefinition;
import org.hibernate.cfg.JPAIndexHolder;
import org.hibernate.cfg.PropertyData;
import org.hibernate.cfg.SecondPass;
import org.hibernate.cfg.UniqueConstraintHolder;
import org.hibernate.cfg.annotations.NamedEntityGraphDefinition;
import org.hibernate.cfg.annotations.NamedProcedureCallDefinition;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.type.TypeResolver;

/**
 * An in-flight representation of Metadata while Metadata is being built.
 *
 * @author Steve Ebersole
 *
 * @since 5.0
 */
public interface InFlightMetadataCollector extends Mapping, MetadataImplementor {

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
	Table addTable(String schema, String catalog, String name, String subselect, boolean isAbstract);

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
	Table addDenormalizedTable(
			String schema,
			String catalog,
			String name,
			boolean isAbstract,
			String subselect,
			Table includedTable) throws DuplicateMappingException;

	/**
	 * Adds metadata for a named query to this repository.
	 *
	 * @param query The metadata
	 *
	 * @throws DuplicateMappingException If a query already exists with that name.
	 */
	void addNamedQuery(NamedQueryDefinition query) throws DuplicateMappingException;

	/**
	 * Adds metadata for a named SQL query to this repository.
	 *
	 * @param query The metadata
	 *
	 * @throws DuplicateMappingException If a query already exists with that name.
	 */
	void addNamedNativeQuery(NamedSQLQueryDefinition query) throws DuplicateMappingException;

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
	 * Adds a type definition to this metadata repository.
	 *
	 * @param typeDefinition The named type definition to add.
	 *
	 * @throws DuplicateMappingException If a TypeDefinition already exists with that name.
	 */
	void addTypeDefinition(TypeDefinition typeDefinition);

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
	void addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject);

	void addFetchProfile(FetchProfile profile);

	TypeResolver getTypeResolver();

	Database getDatabase();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// make sure these are account for better in metamodel

	void addIdentifierGenerator(IdentifierGeneratorDefinition generatorDefinition);


	void addAttributeConverter(AttributeConverterDefinition converter);
	void addAttributeConverter(Class<? extends AttributeConverter> converterClass);

	AttributeConverterAutoApplyHandler getAttributeConverterAutoApplyHandler();


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

	void addDefaultQuery(NamedQueryDefinition queryDefinition);

	void addDefaultNamedNativeQuery(NamedSQLQueryDefinition query);

	void addDefaultResultSetMapping(ResultSetMappingDefinition definition);

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

	ClassmateContext getClassmateContext();

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

	void addUniqueConstraints(Table table, List uniqueConstraints);
	void addUniqueConstraintHolders(Table table, List<UniqueConstraintHolder> uniqueConstraints);
	void addJpaIndexHolders(Table table, List<JPAIndexHolder> jpaIndexHolders);


	interface EntityTableXref {
		void addSecondaryTable(LocalMetadataBuildingContext buildingContext, Identifier logicalName, Join secondaryTableJoin);
		void addSecondaryTable(Identifier logicalName, Join secondaryTableJoin);
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
}
