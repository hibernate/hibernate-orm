//$Id$
package org.hibernate.cfg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.engine.NamedQueryDefinition;
import org.hibernate.engine.NamedSQLQueryDefinition;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.mapping.IdGenerator;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Allow annotation related mappings
 * <p/>
 * at least for named generators
 *
 * @author Emmanuel Bernard
 */
public class ExtendedMappings extends Mappings {

	private final Logger log = LoggerFactory.getLogger( ExtendedMappings.class );

	private final Map<String, IdGenerator> namedGenerators;
	private final Map<String, Map<String, Join>> joins;
	private final Map<String, AnnotatedClassType> classTypes;
	private final Map<String, Properties> generatorTables;
	private final Map<Table, List<String[]>> tableUniqueConstraints;
	private final Map<String, String> mappedByResolver;
	private final Map<String, String> propertyRefResolver;
	private final ReflectionManager reflectionManager;
	private final Set<String> defaultNamedQueryNames;
	private final Set<String> defaultNamedNativeQueryNames;
	private final Set<String> defaultSqlResulSetMappingNames;
	private final Set<String> defaultNamedGenerators;
	private final Map<String, AnyMetaDef> anyMetaDefs;

	ExtendedMappings(
			Map classes, Map collections, Map tables, Map queries, Map sqlqueries, Map sqlResultSetMappings,
			Set<String> defaultNamedQueryNames, Set<String> defaultNamedNativeQueryNames,
			Set<String> defaultSqlResulSetMappingNames, Set<String> defaultNamedGenerators, Map imports,
			List secondPasses, List propertyReferences, NamingStrategy namingStrategy, Map typeDefs,
			Map filterDefinitions, Map namedGenerators, Map<String, Map<String, Join>> joins, Map<String,
			AnnotatedClassType> classTypes, Map extendsQueue, Map<String, TableDescription> tableNameBinding,
											Map<Table, ColumnNames> columnNameBindingPerTable,
											final List auxiliaryDatabaseObjects,
											Map<String, Properties> generatorTables,
											Map<Table, List<String[]>> tableUniqueConstraints,
											Map<String, String> mappedByResolver,
											Map<String, String> propertyRefResolver,
											Map<String, AnyMetaDef> anyMetaDefs,
											ReflectionManager reflectionManager
	) {
		super(
				classes,
				collections,
				tables,
				queries,
				sqlqueries,
				sqlResultSetMappings,
				imports,
				secondPasses,
				propertyReferences,
				namingStrategy,
				typeDefs,
				filterDefinitions,
				extendsQueue,
				auxiliaryDatabaseObjects,
				tableNameBinding,
				columnNameBindingPerTable
		);
		this.namedGenerators = namedGenerators;
		this.joins = joins;
		this.classTypes = classTypes;
		this.generatorTables = generatorTables;
		this.tableUniqueConstraints = tableUniqueConstraints;
		this.mappedByResolver = mappedByResolver;
		this.propertyRefResolver = propertyRefResolver;
		this.reflectionManager = reflectionManager;
		this.defaultNamedQueryNames = defaultNamedQueryNames;
		this.defaultNamedNativeQueryNames = defaultNamedNativeQueryNames;
		this.defaultSqlResulSetMappingNames = defaultSqlResulSetMappingNames;
		this.defaultNamedGenerators = defaultNamedGenerators;
		this.anyMetaDefs = anyMetaDefs;
	}

	public void addGenerator(IdGenerator generator) throws MappingException {
		if ( !defaultNamedGenerators.contains( generator.getName() ) ) {
			Object old = namedGenerators.put( generator.getName(), generator );
			if ( old != null ) log.warn( "duplicate generator name: {}", generator.getName() );
		}
	}

	public void addJoins(PersistentClass persistentClass, Map<String, Join> joins) throws MappingException {
		Object old = this.joins.put( persistentClass.getEntityName(), joins );
		if ( old != null ) log.warn( "duplicate joins for class: {}", persistentClass.getEntityName() );
	}

	public AnnotatedClassType addClassType(XClass clazz) {
		AnnotatedClassType type;
		if ( clazz.isAnnotationPresent( Entity.class ) ) {
			type = AnnotatedClassType.ENTITY;
		}
		else if ( clazz.isAnnotationPresent( Embeddable.class ) ) {
			type = AnnotatedClassType.EMBEDDABLE;
		}
		else if ( clazz.isAnnotationPresent( MappedSuperclass.class ) ) {
			type = AnnotatedClassType.EMBEDDABLE_SUPERCLASS;
		}
		else {
			type = AnnotatedClassType.NONE;
		}
		classTypes.put( clazz.getName(), type );
		return type;
	}

	/**
	 * get and maintain a cache of class type.
	 * A class can be an entity, a embedded objet or nothing.
	 */
	public AnnotatedClassType getClassType(XClass clazz) {
		AnnotatedClassType type = classTypes.get( clazz.getName() );
		if ( type == null ) {
			return addClassType( clazz );
		}
		else {
			return type;
		}
	}

	public IdGenerator getGenerator(String name) {
		return getGenerator( name, null );
	}

	public Map<String, Join> getJoins(String persistentClass) {
		return joins.get( persistentClass );
	}

	/**
	 * Try to find the generator from the localGenerators
	 * and then from the global generator list
	 *
	 * @param name			generator name
	 * @param localGenerators local generators to find to
	 * @return the appropriate idgenerator or null if not found
	 */
	public IdGenerator getGenerator(String name, Map<String, IdGenerator> localGenerators) {
		if ( localGenerators != null ) {
			IdGenerator result = localGenerators.get( name );
			if ( result != null ) return result;
		}
		return namedGenerators.get( name );
	}

	public void addGeneratorTable(String name, Properties params) {
		Object old = generatorTables.put( name, params );
		if ( old != null ) log.warn( "duplicate generator table: {}", name );
	}

	public Properties getGeneratorTableProperties(String name, Map<String, Properties> localGeneratorTables) {
		if ( localGeneratorTables != null ) {
			Properties result = localGeneratorTables.get( name );
			if ( result != null ) return result;
		}
		return generatorTables.get( name );
	}

	public void addUniqueConstraints(Table table, List uniqueConstraints) {
		List oldConstraints = tableUniqueConstraints.get( table );
		if ( oldConstraints == null ) {
			oldConstraints = new ArrayList();
			tableUniqueConstraints.put( table, oldConstraints );
		}
		oldConstraints.addAll( uniqueConstraints );
	}

	public Map<Table, List<String[]>> getTableUniqueConstraints() {
		return tableUniqueConstraints;
	}

	public void addMappedBy(String entityName, String propertyName, String inversePropertyName) {
		mappedByResolver.put( entityName + "." + propertyName, inversePropertyName );
	}

	public String getFromMappedBy(String entityName, String propertyName) {
		return mappedByResolver.get( entityName + "." + propertyName );
	}

	public void addPropertyReferencedAssociation(String entityName, String propertyName, String propertyRef) {
		propertyRefResolver.put( entityName + "." + propertyName, propertyRef );
	}

	public String getPropertyReferencedAssociation(String entityName, String propertyName) {
		return propertyRefResolver.get( entityName + "." + propertyName );
	}

	@Override
	public void addUniquePropertyReference(String referencedClass, String propertyName) {
		super.addUniquePropertyReference( referencedClass, propertyName );
	}

	@Override
	public void addPropertyReference(String referencedClass, String propertyName) {
		super.addPropertyReference( referencedClass, propertyName );
	}

	public ReflectionManager getReflectionManager() {
		return reflectionManager;
	}

	public void addDefaultQuery(String name, NamedQueryDefinition query) {
		super.addQuery( name, query );
		defaultNamedQueryNames.add( name );
	}

	public void addDefaultSQLQuery(String name, NamedSQLQueryDefinition query) {
		super.addSQLQuery( name, query );
		defaultNamedNativeQueryNames.add( name );
	}

	public void addDefaultGenerator(IdGenerator idGen) {
		this.addGenerator( idGen );
		defaultNamedGenerators.add( idGen.getName() );

	}

	public void addDefaultResultSetMapping(ResultSetMappingDefinition definition) {
		final String name = definition.getName();
		if ( !defaultSqlResulSetMappingNames.contains( name )
				&& super.getResultSetMapping( name ) != null ) {
			resultSetMappings.remove( name );
		}
		super.addResultSetMapping( definition );
		defaultSqlResulSetMappingNames.add( name );
	}

	@Override
	public void addQuery(String name, NamedQueryDefinition query) throws MappingException {
		if ( !defaultNamedQueryNames.contains( name ) ) super.addQuery( name, query );
	}

	@Override
	public void addResultSetMapping(ResultSetMappingDefinition definition) {
		if ( !defaultSqlResulSetMappingNames.contains( definition.getName() ) )
			super.addResultSetMapping( definition );
	}

	@Override
	public void addSQLQuery(String name, NamedSQLQueryDefinition query) throws MappingException {
		if ( !defaultNamedNativeQueryNames.contains( name ) ) super.addSQLQuery( name, query );
	}

	public Map getClasses() {
		return classes;
	}

	public void addAnyMetaDef(AnyMetaDef defAnn) {
		if ( anyMetaDefs.containsKey( defAnn.name() ) ) {
			throw new AnnotationException( "Two @AnyMetaDef with the same name defined: " + defAnn.name() );
		}
		anyMetaDefs.put( defAnn.name(), defAnn );
	}

	public AnyMetaDef getAnyMetaDef(String name) {
		return anyMetaDefs.get( name );
	}
}