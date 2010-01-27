/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cfg;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.engine.NamedQueryDefinition;
import org.hibernate.engine.NamedSQLQueryDefinition;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.mapping.IdGenerator;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;

/**
 * Allow annotation related mappings
 * <p/>
 * at least for named generators
 *
 * @author Emmanuel Bernard
 */
public interface ExtendedMappings extends Mappings {

	/**
	 * Adds a default id generator.
	 *
	 * @param generator The id generator
	 */
	public void addDefaultGenerator(IdGenerator generator);

	/**
	 * Retrieve the id-generator by name.
	 *
	 * @param name The generator name.
	 * @return The generator, or null.
	 */
	public IdGenerator getGenerator(String name);

	/**
	 * Try to find the generator from the localGenerators
	 * and then from the global generator list
	 *
	 * @param name generator name
	 * @param localGenerators local generators
	 * @return the appropriate idgenerator or null if not found
	 */
	public IdGenerator getGenerator(String name, Map<String, IdGenerator> localGenerators);

	/**
	 * Add a generator.
	 *
	 * @param generator The generator to add.
	 */
	public void addGenerator(IdGenerator generator);

	/**
	 * Add a generator table properties.
	 *
	 * @param name The generator name
	 * @param params The generator table properties.
	 */
	public void addGeneratorTable(String name, Properties params);

	/**
	 * Retrieve the properties related to a generator table.
	 *
	 * @param name generator name
	 * @param localGeneratorTables local generator tables
	 * @return The properties, or null.
	 */
	public Properties getGeneratorTableProperties(String name, Map<String, Properties> localGeneratorTables);

	/**
	 * Retrieve join metadata for a particular persistent entity.
	 *
	 * @param entityName The entity name
	 * @return The join metadata
	 */
	public Map<String, Join> getJoins(String entityName);

	/**
	 * Add join metadata for a persistent entity.
	 *
	 * @param persistentClass The persistent entity metadata.
	 * @param joins The join metadata to add.
	 * @throws MappingException
	 */
	public void addJoins(PersistentClass persistentClass, Map<String, Join> joins);

	/**
	 * Get and maintain a cache of class type.
	 *
	 * @param clazz The XClass mapping
	 * @return The class type.
	 */
	public AnnotatedClassType getClassType(XClass clazz);

	/**
	 * FIXME should be private but will this break things?
	 * Add a class type.
	 *
	 * @param clazz The XClass mapping.
	 * @return The class type.
	 */
	public AnnotatedClassType addClassType(XClass clazz);

	/**
	 * @deprecated Use {@link #getUniqueConstraintHoldersByTable} instead
	 */
	@SuppressWarnings({ "JavaDoc" })
	public Map<Table, List<String[]>> getTableUniqueConstraints();

	public Map<Table, List<UniqueConstraintHolder>> getUniqueConstraintHoldersByTable();

	/**
	 * @deprecated Use {@link #addUniqueConstraintHolders} instead
	 */
	@SuppressWarnings({ "JavaDoc" })
	public void addUniqueConstraints(Table table, List uniqueConstraints);

	public void addUniqueConstraintHolders(Table table, List<UniqueConstraintHolder> uniqueConstraintHolders);

	public void addMappedBy(String entityName, String propertyName, String inversePropertyName);

	public String getFromMappedBy(String entityName, String propertyName);

	public void addPropertyReferencedAssociation(String entityName, String propertyName, String propertyRef);

	public String getPropertyReferencedAssociation(String entityName, String propertyName);

	public ReflectionManager getReflectionManager();

	public void addDefaultQuery(String name, NamedQueryDefinition query);

	public void addDefaultSQLQuery(String name, NamedSQLQueryDefinition query);

	public void addDefaultResultSetMapping(ResultSetMappingDefinition definition);

	public Map getClasses();

	public void addAnyMetaDef(AnyMetaDef defAnn) throws AnnotationException;

	public AnyMetaDef getAnyMetaDef(String name);
	
	public boolean isInSecondPass();

	/**
	 * Return the property annotated with @MapsId("propertyName") if any.
	 * Null otherwise
	 */
	public PropertyData getPropertyAnnotatedWithMapsId(XClass entityType, String propertyName);

	public void addPropertyAnnotatedWithMapsId(XClass entityType, PropertyData property);

	/**
	 * Should we use the new generator strategy mappings.  This is controlled by the
	 * {@link AnnotationConfiguration#USE_NEW_ID_GENERATOR_MAPPINGS} setting.
	 *
	 * @return True if the new generators should be used, false otherwise.
	 */
	public boolean useNewGeneratorMappings();
}