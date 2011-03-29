/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.metamodel.source.hbm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.dom4j.Attribute;
import org.dom4j.Element;

import org.hibernate.EntityMode;
import org.hibernate.MappingException;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.engine.Versioning;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.BagBinding;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.PluralAttributeBinding;
import org.hibernate.metamodel.binding.SimpleAttributeBinding;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.domain.Hierarchical;
import org.hibernate.metamodel.domain.PluralAttribute;
import org.hibernate.metamodel.domain.PluralAttributeNature;
import org.hibernate.metamodel.domain.SingularAttribute;
import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.relational.Index;
import org.hibernate.metamodel.relational.Schema;
import org.hibernate.metamodel.relational.SimpleValue;
import org.hibernate.metamodel.relational.Table;
import org.hibernate.metamodel.relational.TableSpecification;
import org.hibernate.metamodel.relational.Tuple;
import org.hibernate.metamodel.relational.UniqueKey;
import org.hibernate.metamodel.relational.Value;
import org.hibernate.metamodel.source.Metadata;
import org.hibernate.metamodel.source.util.DomHelper;

/**
* TODO : javadoc
*
* @author Steve Ebersole
*/
abstract class AbstractEntityBinder {

	protected final HibernateMappingBinder hibernateMappingBinder;
	protected final Map<String, MetaAttribute> entityMetas;
	protected final Schema.Name schemaName;

	AbstractEntityBinder(HibernateMappingBinder hibernateMappingBinder, Element entityElement) {
		this.hibernateMappingBinder = hibernateMappingBinder;

		entityMetas = HbmHelper.extractMetas( entityElement, true, hibernateMappingBinder.getMappingMetas() );

		final Attribute schemaAttribute = entityElement.attribute( "schema" );
		String schemaName = ( schemaAttribute == null )
				? hibernateMappingBinder.getDefaultSchemaName()
				: schemaAttribute.getValue();

		final Attribute catalogAttribute = entityElement.attribute( "catalog" );
		String catalogName = ( catalogAttribute == null )
				? hibernateMappingBinder.getDefaultCatalogName()
				: catalogAttribute.getValue();

		this.schemaName = new Schema.Name( schemaName, catalogName );
	}

	protected HibernateXmlBinder getHibernateXmlBinder() {
		return hibernateMappingBinder.getHibernateXmlBinder();
	}

	protected Metadata getMetadata() {
		return hibernateMappingBinder.getHibernateXmlBinder().getMetadata();
	}

	protected NamingStrategy getNamingStrategy() {
		return getMetadata().getNamingStrategy();
	}

	protected void basicEntityBinding(Element node, EntityBinding entityBinding, Hierarchical superType) {
		entityBinding.setMetaAttributes( entityMetas );

		// transfer an explicitly defined lazy attribute
		Attribute lazyNode = node.attribute( "lazy" );
		boolean lazy = ( lazyNode == null )
				? hibernateMappingBinder.isDefaultLazy()
				: Boolean.valueOf( lazyNode.getValue() );
		// go ahead and set the lazy here, since pojo.proxy can override it.
		entityBinding.setLazy( lazy );

		String entityName = hibernateMappingBinder.extractEntityName( node );
		if ( entityName == null ) {
			throw new MappingException( "Unable to determine entity name" );
		}
		entityBinding.setEntity( new Entity( entityName, superType ) );

		bindPojoRepresentation( node, entityBinding );
		bindDom4jRepresentation( node, entityBinding );
		bindMapRepresentation( node, entityBinding );

		Iterator itr = node.elementIterator( "fetch-profile" );
		while ( itr.hasNext() ) {
			final Element profileElement = ( Element ) itr.next();
			hibernateMappingBinder.parseFetchProfile( profileElement, entityName );
		}

		entityBinding.setDiscriminatorValue( DomHelper.extractAttributeValue( node, "discriminator-value", entityName ) );
		entityBinding.setDynamicUpdate( DomHelper.extractBooleanAttributeValue( node, "dynamic-update", false ) );
		entityBinding.setDynamicInsert( DomHelper.extractBooleanAttributeValue( node, "dynamic-insert", false ) );

		getMetadata().addImport( entityName, entityName );
		if ( hibernateMappingBinder.isAutoImport() ) {
			if ( entityName.indexOf( '.' ) > 0 ) {
				getMetadata().addImport( StringHelper.unqualify( entityName ), entityName );
			}
		}

		final Attribute batchNode = node.attribute( "batch-size" );
		if ( batchNode != null ) {
			entityBinding.setBatchSize( Integer.parseInt( batchNode.getValue() ) );
		}

		final Attribute sbuNode = node.attribute( "select-before-update" );
		if ( sbuNode != null ) {
			entityBinding.setSelectBeforeUpdate( Boolean.valueOf( sbuNode.getValue() ) );
		}

		// OPTIMISTIC LOCK MODE
		Attribute olNode = node.attribute( "optimistic-lock" );
		entityBinding.setOptimisticLockMode( getOptimisticLockMode( olNode ) );


		// PERSISTER
		Attribute persisterNode = node.attribute( "persister" );
		if ( persisterNode != null ) {
			try {
				entityBinding.setEntityPersisterClass(
						ReflectHelper.classForName( persisterNode.getValue() )
				);
			}
			catch (ClassNotFoundException cnfe) {
				throw new MappingException( "Could not find persister class: "
					+ persisterNode.getValue() );
			}
		}

		// CUSTOM SQL
		handleCustomSQL( node, entityBinding );

		Iterator tables = node.elementIterator( "synchronize" );
		while ( tables.hasNext() ) {
			entityBinding.addSynchronizedTable( ( (Element) tables.next() ).attributeValue( "table" ) );
		}

		Attribute abstractNode = node.attribute( "abstract" );
		Boolean isAbstract = abstractNode == null
				? null
		        : "true".equals( abstractNode.getValue() )
						? Boolean.TRUE
	                    : "false".equals( abstractNode.getValue() )
								? Boolean.FALSE
	                            : null;
		entityBinding.setAbstract( isAbstract );
	}

	private void bindPojoRepresentation(Element node, EntityBinding entityBinding) {
		String className = hibernateMappingBinder.getClassName( node.attribute( "name" ) );
		String proxyName = hibernateMappingBinder.getClassName( node.attribute( "proxy" ) );

		entityBinding.getEntity().getPojoEntitySpecifics().setClassName( className );

		if ( proxyName != null ) {
			entityBinding.getEntity().getPojoEntitySpecifics().setProxyInterfaceName( proxyName );
			entityBinding.setLazy( true );
		}
		else if ( entityBinding.isLazy() ) {
			entityBinding.getEntity().getPojoEntitySpecifics().setProxyInterfaceName( className );
		}

		Element tuplizer = locateTuplizerDefinition( node, EntityMode.POJO );
		if ( tuplizer != null ) {
			entityBinding.getEntity().getPojoEntitySpecifics().setTuplizerClassName( tuplizer.attributeValue( "class" ) );
		}
	}

	private void bindDom4jRepresentation(Element node, EntityBinding entityBinding) {
		String nodeName = node.attributeValue( "node" );
		if ( nodeName == null ) {
			nodeName = StringHelper.unqualify( entityBinding.getEntity().getName() );
		}
		entityBinding.getEntity().getDom4jEntitySpecifics().setNodeName(nodeName);

		Element tuplizer = locateTuplizerDefinition( node, EntityMode.DOM4J );
		if ( tuplizer != null ) {
			entityBinding.getEntity().getDom4jEntitySpecifics().setTuplizerClassName( tuplizer.attributeValue( "class" ) );
		}
	}

	private void bindMapRepresentation(Element node, EntityBinding entityBinding) {
		Element tuplizer = locateTuplizerDefinition( node, EntityMode.MAP );
		if ( tuplizer != null ) {
			entityBinding.getEntity().getMapEntitySpecifics().setTuplizerClassName( tuplizer.attributeValue( "class" ) );
		}
	}

	/**
	 * Locate any explicit tuplizer definition in the metadata, for the given entity-mode.
	 *
	 * @param container The containing element (representing the entity/component)
	 * @param entityMode The entity-mode for which to locate the tuplizer element
	 *
	 * @return The tuplizer element, or null.
	 */
	private static Element locateTuplizerDefinition(Element container, EntityMode entityMode) {
		Iterator itr = container.elementIterator( "tuplizer" );
		while( itr.hasNext() ) {
			final Element tuplizerElem = ( Element ) itr.next();
			if ( entityMode.toString().equals( tuplizerElem.attributeValue( "entity-mode") ) ) {
				return tuplizerElem;
			}
		}
		return null;
	}

	int getOptimisticLockMode(Attribute olAtt) throws MappingException {
		if ( olAtt == null ) {
			return Versioning.OPTIMISTIC_LOCK_VERSION;
		}
		String olMode = olAtt.getValue();
		if ( olMode == null || "version".equals( olMode ) ) {
			return Versioning.OPTIMISTIC_LOCK_VERSION;
		}
		else if ( "dirty".equals( olMode ) ) {
			return Versioning.OPTIMISTIC_LOCK_DIRTY;
		}
		else if ( "all".equals( olMode ) ) {
			return Versioning.OPTIMISTIC_LOCK_ALL;
		}
		else if ( "none".equals( olMode ) ) {
			return Versioning.OPTIMISTIC_LOCK_NONE;
		}
		else {
			throw new MappingException( "Unsupported optimistic-lock style: " + olMode );
		}
	}

	private static void handleCustomSQL(Element entityElement, EntityBinding entityBinding)
			throws MappingException {
		Element element = entityElement.element( "sql-insert" );
		if ( element != null ) {
			boolean callable = HbmHelper.isCallable( element );
			entityBinding.setCustomSqlInsert( element.getTextTrim(), callable, HbmHelper.getResultCheckStyle( element, callable ) );
		}

		element = entityElement.element( "sql-delete" );
		if ( element != null ) {
			boolean callable = HbmHelper.isCallable( element );
			entityBinding.setCustomSqlDelete( element.getTextTrim(), callable, HbmHelper.getResultCheckStyle( element, callable ) );
		}

		element = entityElement.element( "sql-update" );
		if ( element != null ) {
			boolean callable = HbmHelper.isCallable( element );
			entityBinding.setCustomSqlUpdate( element.getTextTrim(), callable, HbmHelper.getResultCheckStyle( element, callable ) );
		}

		element = entityElement.element( "loader" );
		if ( element != null ) {
			entityBinding.setLoaderName( element.attributeValue( "query-ref" ) );
		}
	}

	protected String getClassTableName(
			Element entityElement,
			EntityBinding entityBinding,
			Table denormalizedSuperTable) {
		final String entityName = entityBinding.getEntity().getName();
		final Attribute tableNameNode = entityElement.attribute( "table" );
		String logicalTableName;
		String physicalTableName;
		if ( tableNameNode == null ) {
			logicalTableName = StringHelper.unqualify( entityName );
			physicalTableName = getHibernateXmlBinder().getMetadata().getNamingStrategy().classToTableName( entityName );
		}
		else {
			logicalTableName = tableNameNode.getValue();
			physicalTableName = getHibernateXmlBinder().getMetadata().getNamingStrategy().tableName( logicalTableName );
		}
// todo : find out the purpose of these logical bindings
//			mappings.addTableBinding( schema, catalog, logicalTableName, physicalTableName, denormalizedSuperTable );
		return physicalTableName;
	}

	protected void buildAttributeBindings(Element entityElement, EntityBinding entityBinding) {
		// null = UniqueKey (we are not binding a natural-id mapping)
		// true = mutable, by default properties are mutable
		// true = nullable, by default properties are nullable.
		buildAttributeBindings( entityElement, entityBinding, null, true, true );
	}

	/**
	 * This form is essentially used to create natural-id mappings.  But the processing is the same, aside from these
	 * extra parameterized values, so we encapsulate it here.
	 *
	 * @param entityElement
	 * @param entityBinding
	 * @param uniqueKey
	 * @param mutable
	 * @param nullable
	 */
	protected void buildAttributeBindings(
			Element entityElement,
			EntityBinding entityBinding,
			UniqueKey uniqueKey,
			boolean mutable,
			boolean nullable) {
		final boolean naturalId = uniqueKey != null;

		final String entiytName = entityBinding.getEntity().getName();
		final TableSpecification tabe = entityBinding.getBaseTable();

		AttributeBinding attributeBinding = null;

		Iterator iter = entityElement.elementIterator();
		while ( iter.hasNext() ) {
			final Element subElement = (Element) iter.next();
			final String subElementName = subElement.getName();
			final String propertyName = subElement.attributeValue( "name" );

			if ( "bag".equals( subElementName ) ) {
				BagBinding bagBinding = entityBinding.makeBagAttributeBinding( propertyName );
				bindCollection( subElement, bagBinding, entityBinding, PluralAttributeNature.BAG, propertyName );
				hibernateMappingBinder.getHibernateXmlBinder().getMetadata().addCollection( bagBinding );
				attributeBinding = bagBinding;
			}
			else if ( "idbag".equals( subElementName ) ) {
				BagBinding bagBinding = entityBinding.makeBagAttributeBinding( propertyName );
				bindCollection( subElement, bagBinding, entityBinding, PluralAttributeNature.BAG, propertyName );
				hibernateMappingBinder.getHibernateXmlBinder().getMetadata().addCollection( bagBinding );
				attributeBinding = bagBinding;
				// todo: handle identifier
			}
			else if ( "set".equals( subElementName ) ) {
				BagBinding bagBinding = entityBinding.makeBagAttributeBinding( propertyName );
				bindCollection( subElement, bagBinding, entityBinding, PluralAttributeNature.SET, propertyName );
				hibernateMappingBinder.getHibernateXmlBinder().getMetadata().addCollection( bagBinding );
				attributeBinding = bagBinding;
			}
			else if ( "list".equals( subElementName ) ) {
				BagBinding bagBinding = entityBinding.makeBagAttributeBinding( propertyName );
				bindCollection( subElement, bagBinding, entityBinding, PluralAttributeNature.LIST, propertyName );
				hibernateMappingBinder.getHibernateXmlBinder().getMetadata().addCollection( bagBinding );
				attributeBinding = bagBinding;
				// todo : handle list index
			}
			else if ( "map".equals( subElementName ) ) {
				BagBinding bagBinding = entityBinding.makeBagAttributeBinding( propertyName );
				bindCollection( subElement, bagBinding, entityBinding, PluralAttributeNature.MAP, propertyName );
				hibernateMappingBinder.getHibernateXmlBinder().getMetadata().addCollection( bagBinding );
				attributeBinding = bagBinding;
				// todo : handle map key
			}
			else if ( "many-to-one".equals( subElementName ) ) {
// todo : implement
//				value = new ManyToOne( mappings, table );
//				bindManyToOne( subElement, (ManyToOne) value, propertyName, nullable, mappings );
			}
			else if ( "any".equals( subElementName ) ) {
// todo : implement
//				value = new Any( mappings, table );
//				bindAny( subElement, (Any) value, nullable, mappings );
			}
			else if ( "one-to-one".equals( subElementName ) ) {
// todo : implement
//				value = new OneToOne( mappings, table, persistentClass );
//				bindOneToOne( subElement, (OneToOne) value, propertyName, true, mappings );
			}
			else if ( "property".equals( subElementName ) ) {
				SimpleAttributeBinding binding = entityBinding.makeSimpleAttributeBinding( propertyName );
				bindSimpleAttribute( subElement, binding, entityBinding, propertyName );
				attributeBinding = binding;
			}
			else if ( "component".equals( subElementName )
					|| "dynamic-component".equals( subElementName )
					|| "properties".equals( subElementName ) ) {
// todo : implement
//				String subpath = StringHelper.qualify( entityName, propertyName );
//				value = new Component( mappings, persistentClass );
//
//				bindComponent(
//						subElement,
//						(Component) value,
//						persistentClass.getClassName(),
//						propertyName,
//						subpath,
//						true,
//						"properties".equals( subElementName ),
//						mappings,
//						inheritedMetas,
//						false
//					);
			}
			else if ( "join".equals( subElementName ) ) {
// todo : implement
//				Join join = new Join();
//				join.setPersistentClass( persistentClass );
//				bindJoin( subElement, join, mappings, inheritedMetas );
//				persistentClass.addJoin( join );
			}
			else if ( "subclass".equals( subElementName ) ) {
// todo : implement
//				handleSubclass( persistentClass, mappings, subElement, inheritedMetas );
			}
			else if ( "joined-subclass".equals( subElementName ) ) {
// todo : implement
//				handleJoinedSubclass( persistentClass, mappings, subElement, inheritedMetas );
			}
			else if ( "union-subclass".equals( subElementName ) ) {
// todo : implement
//				handleUnionSubclass( persistentClass, mappings, subElement, inheritedMetas );
			}
			else if ( "filter".equals( subElementName ) ) {
// todo : implement
//				parseFilter( subElement, entityBinding );
			}
			else if ( "natural-id".equals( subElementName ) ) {
// todo : implement
//				UniqueKey uk = new UniqueKey();
//				uk.setName("_UniqueKey");
//				uk.setTable(table);
//				//by default, natural-ids are "immutable" (constant)
//				boolean mutableId = "true".equals( subElement.attributeValue("mutable") );
//				createClassProperties(
//						subElement,
//						persistentClass,
//						mappings,
//						inheritedMetas,
//						uk,
//						mutableId,
//						false,
//						true
//					);
//				table.addUniqueKey(uk);
			}
			else if ( "query".equals(subElementName) ) {
// todo : implement
//				bindNamedQuery(subElement, persistentClass.getEntityName(), mappings);
			}
			else if ( "sql-query".equals(subElementName) ) {
// todo : implement
//				bindNamedSQLQuery(subElement, persistentClass.getEntityName(), mappings);
			}
			else if ( "resultset".equals(subElementName) ) {
// todo : implement
//				bindResultSetMappingDefinition( subElement, persistentClass.getEntityName(), mappings );
			}

//			if ( value != null ) {
//				Property property = createProperty( value, propertyName, persistentClass
//					.getClassName(), subElement, mappings, inheritedMetas );
//				if ( !mutable ) property.setUpdateable(false);
//				if ( naturalId ) property.setNaturalIdentifier(true);
//				persistentClass.addProperty( property );
//				if ( uniqueKey!=null ) uniqueKey.addColumns( property.getColumnIterator() );
//			}

		}
	}

	protected void bindSimpleAttribute(Element propertyElement, SimpleAttributeBinding attributeBinding, EntityBinding entityBinding, String attributeName) {
		if ( attributeBinding.getAttribute() == null ) {
			// attribute has not been bound yet
			SingularAttribute attribute = entityBinding.getEntity().getOrCreateSingularAttribute( attributeName );
			attributeBinding.setAttribute( attribute );
			basicAttributeBinding( propertyElement, attributeBinding );
		}

		if ( attributeBinding.getValue() == null ) {
			// relational model has not been bound yet
			Value idValue = processValues( propertyElement, entityBinding.getBaseTable(), attributeName );
			attributeBinding.setValue( idValue );
		}
	}

	protected void bindCollection(
			Element collectionElement,
			PluralAttributeBinding collectionBinding,
			EntityBinding entityBinding,
			PluralAttributeNature attributeNature,
			String attributeName) {
		if ( collectionBinding.getAttribute() == null ) {
			// domain model has not been bound yet
			PluralAttribute attribute = entityBinding.getEntity().getOrCreatePluralAttribute( attributeName, attributeNature );
			collectionBinding.setAttribute( attribute );
			basicCollectionBinding( collectionElement, collectionBinding );
		}

		// todo : relational model binding
	}

	protected void basicCollectionBinding(Element collectionElement, PluralAttributeBinding collectionBinding) {
		// todo : implement
	}

//	private static Property createProperty(
//			final Value value,
//	        final String propertyName,
//			final String className,
//	        final Element subnode,
//	        final Mappings mappings,
//			java.util.Map inheritedMetas) throws MappingException {
//
//		if ( StringHelper.isEmpty( propertyName ) ) {
//			throw new MappingException( subnode.getName() + " mapping must defined a name attribute [" + className + "]" );
//		}
//
//		value.setTypeUsingReflection( className, propertyName );
//
//		// this is done here 'cos we might only know the type here (ugly!)
//		// TODO: improve this a lot:
//		if ( value instanceof ToOne ) {
//			ToOne toOne = (ToOne) value;
//			String propertyRef = toOne.getReferencedPropertyName();
//			if ( propertyRef != null ) {
//				mappings.addUniquePropertyReference( toOne.getReferencedEntityName(), propertyRef );
//			}
//		}
//		else if ( value instanceof Collection ) {
//			Collection coll = (Collection) value;
//			String propertyRef = coll.getReferencedPropertyName();
//			// not necessarily a *unique* property reference
//			if ( propertyRef != null ) {
//				mappings.addPropertyReference( coll.getOwnerEntityName(), propertyRef );
//			}
//		}
//
//		value.createForeignKey();
//		Property prop = new Property();
//		prop.setValue( value );
//		bindProperty( subnode, prop, mappings, inheritedMetas );
//		return prop;
//	}

	protected Value processValues(Element identifierElement, TableSpecification baseTable, String propertyPath) {
		// first boolean (false here) indicates that by default columns are nullable
		// second boolean (true here) indicates that by default column names should be guessed
		return processValues( identifierElement, baseTable, false, true, propertyPath );
	}

	protected Value processValues(
			Element propertyElement,
			TableSpecification table,
			boolean isNullableByDefault,
			boolean autoColumnCreation,
			String propertyPath) {
		final UniqueKeyBinder propertyUniqueKeyBinder = new UniqueKeyBinder( propertyElement.attribute( "unique-key" ), table );
		final IndexBinder propertyIndexBinder = new IndexBinder( propertyElement.attribute( "index" ), table );

		final Attribute columnAttribute = propertyElement.attribute( "column" );

		if ( columnAttribute == null ) {
			SimpleValue value = null;
			Tuple tuple = null;
			final Iterator valueElements = propertyElement.elementIterator();
			while ( valueElements.hasNext() ) {
				if ( value != null ) {
					if ( tuple == null ) {
						tuple = table.createTuple( "[" + propertyPath + "]" );
					}
					tuple.addValue( value );
				}

				final Element valueElement = (Element) valueElements.next();
				if ( "column".equals( valueElement.getName() ) ) {
					final Element columnElement = valueElement;
					final String explicitName = columnElement.attributeValue( "name" );
					final String logicalColumnName = getNamingStrategy().logicalColumnName( explicitName, propertyPath );
					final String columnName = getNamingStrategy().columnName( explicitName );
// todo : find out the purpose of these logical bindings
//				mappings.addColumnBinding( logicalColumnName, column, table );
					Column column = table.createColumn( columnName );
					value = column;
					basicColumnBinding( columnElement, column, isNullableByDefault );

					propertyUniqueKeyBinder.bindColumn( column );
					propertyIndexBinder.bindColumn( column );
					new UniqueKeyBinder( columnElement.attribute( "unique-key" ), table ).bindColumn( column );
					new IndexBinder( columnElement.attribute( "index" ), table ).bindColumn( column );
				}
				else if ( "formula".equals( valueElement.getName() ) ) {
					value = table.createDerivedValue( valueElement.getTextTrim() );
				}
			}

// todo : logical 1-1 handling
//			final Attribute uniqueAttribute = node.attribute( "unique" );
//			if ( uniqueAttribute != null
//					&& "true".equals( uniqueAttribute.getValue() )
//					&& ManyToOne.class.isInstance( simpleValue ) ) {
//				( (ManyToOne) simpleValue ).markAsLogicalOneToOne();
//			}

			if ( tuple != null ) {
				return tuple;
			}
			else if ( value != null ) {
				return value;
			}
			else if ( autoColumnCreation  ) {
				final String columnName = getNamingStrategy().propertyToColumnName( propertyPath );
				final String logicalColumnName = getNamingStrategy().logicalColumnName( null, propertyPath );
// todo : find out the purpose of these logical bindings
//				mappings.addColumnBinding( logicalColumnName, column, table );
				Column column = table.createColumn( columnName );
				basicColumnBinding( propertyElement, column, isNullableByDefault );
				propertyUniqueKeyBinder.bindColumn( column );
				propertyIndexBinder.bindColumn( column );
				return column;
			}
		}

		if ( propertyElement.elementIterator( "column" ).hasNext() ) {
			throw new MappingException( "column attribute may not be used together with <column> subelement" );
		}

		if ( propertyElement.elementIterator( "formula" ).hasNext() ) {
			throw new MappingException( "column attribute may not be used together with <formula> subelement" );
		}

		final String explicitName = columnAttribute.getValue();
		final String logicalColumnName = getNamingStrategy().logicalColumnName( explicitName, propertyPath );
		final String columnName = getNamingStrategy().columnName( explicitName );
// todo : find out the purpose of these logical bindings
//		mappings.addColumnBinding( logicalColumnName, column, table );
		Column column = table.createColumn( columnName );
		basicColumnBinding( propertyElement, column, isNullableByDefault );
		propertyUniqueKeyBinder.bindColumn( column );
		propertyIndexBinder.bindColumn( column );
		return column;
	}

	protected static class UniqueKeyBinder {
		private final List<UniqueKey> uniqueKeys;

		UniqueKeyBinder(Attribute uniqueKeyAttribute, TableSpecification table) {
			if ( uniqueKeyAttribute == null ) {
				uniqueKeys = Collections.emptyList();
			}
			else {
				uniqueKeys = new ArrayList<UniqueKey>();
				StringTokenizer uniqueKeyNames = new StringTokenizer( uniqueKeyAttribute.getValue(), ", " );
				while ( uniqueKeyNames.hasMoreTokens() ) {
					uniqueKeys.add( table.getOrCreateUniqueKey( uniqueKeyNames.nextToken() ) );
				}
			}
		}

		void bindColumn(Column column) {
			for ( UniqueKey uniqueKey : uniqueKeys ) {
				uniqueKey.addColumn( column );
			}
		}
	}

	protected static class IndexBinder {
		private final List<Index> indexes;

		IndexBinder(Attribute indexAttribute, TableSpecification table) {
			if ( indexAttribute == null ) {
				indexes = Collections.emptyList();
			}
			else {
				indexes = new ArrayList<Index>();
				StringTokenizer indexNames = new StringTokenizer( indexAttribute.getValue(), ", " );
				while ( indexNames.hasMoreTokens() ) {
					indexes.add( table.getOrCreateIndex( indexNames.nextToken() ) );
				}
			}
		}

		void bindColumn(Column column) {
			for ( Index index : indexes ) {
				index.addColumn( column );
			}
		}
	}

	public static void basicColumnBinding(Element node, Column column, boolean isNullable) throws MappingException {
		Attribute lengthNode = node.attribute( "length" );
		if ( lengthNode != null ) {
			column.getSize().setLength( Integer.parseInt( lengthNode.getValue() ) );
		}
		Attribute scalNode = node.attribute( "scale" );
		if ( scalNode != null ) {
			column.getSize().setScale( Integer.parseInt( scalNode.getValue() ) );
		}
		Attribute precNode = node.attribute( "precision" );
		if ( precNode != null ) {
			column.getSize().setPrecision( Integer.parseInt( precNode.getValue() ) );
		}

		Attribute nullNode = node.attribute( "not-null" );
		column.setNullable( nullNode == null ? isNullable : nullNode.getValue().equals( "false" ) );

		Attribute unqNode = node.attribute( "unique" );
		if ( unqNode != null ) {
			column.setUnique( unqNode.getValue().equals( "true" ) );
		}

		column.setCheckCondition( node.attributeValue( "check" ) );
		column.setDefaultValue( node.attributeValue( "default" ) );

		Attribute typeNode = node.attribute( "sql-type" );
		if ( typeNode != null ) column.setSqlType( typeNode.getValue() );

		String customWrite = node.attributeValue( "write" );
		if(customWrite != null && !customWrite.matches("[^?]*\\?[^?]*")) {
			throw new MappingException("write expression must contain exactly one value placeholder ('?') character");
		}
		column.setWriteFragment( customWrite );
		column.setReadFragment( node.attributeValue( "read" ) );

		Element comment = node.element("comment");
		if ( comment != null ) {
			column.setComment( comment.getTextTrim() );
		}
	}

	protected void basicAttributeBinding(Element propertyElement, SimpleAttributeBinding valueBinding) {
		Attribute typeAttribute = propertyElement.attribute( "type" );
		if ( typeAttribute != null ) {
			valueBinding.getHibernateTypeDescriptor().setTypeName( typeAttribute.getValue() );
		}

		valueBinding.setMetaAttributes( HbmHelper.extractMetas( propertyElement, entityMetas ) );

		final String propertyName = valueBinding.getAttribute().getName();
		final String explicitNodename = propertyElement.attributeValue( "node" );
		final String nodeName = explicitNodename != null ? explicitNodename : propertyName;
		valueBinding.setNodeName( nodeName );

		final Attribute accessNode = propertyElement.attribute( "access" );
		if ( accessNode != null ) {
			valueBinding.setPropertyAccessorName( accessNode.getValue() );
		}
		else if ( propertyElement.getName().equals( "properties" ) ) {
			valueBinding.setPropertyAccessorName( "embedded" );
		}
		else {
			valueBinding.setPropertyAccessorName( hibernateMappingBinder.getDefaultAccess() );
		}

		final String explicitCascade = propertyElement.attributeValue( "cascade" );
		final String cascade = StringHelper.isNotEmpty( explicitCascade ) ? explicitCascade : hibernateMappingBinder.getDefaultCascade();
		valueBinding.setCascade( cascade );

		final Attribute updateAttribute = propertyElement.attribute( "update" );
		valueBinding.setUpdateable( updateAttribute == null || "true".equals( updateAttribute.getValue() ) );

		final Attribute insertAttribute = propertyElement.attribute( "insert" );
		valueBinding.setInsertable( insertAttribute == null || "true".equals( insertAttribute.getValue() ) );

		final Attribute optimisticLockAttribute = propertyElement.attribute( "optimistic-lock" );
		valueBinding.setOptimisticLockable( optimisticLockAttribute == null || "true".equals( optimisticLockAttribute.getValue() ) );

		final Attribute generatedAttribute= propertyElement.attribute( "generated" );
        final String generationName = generatedAttribute == null ? null : generatedAttribute.getValue();
        final PropertyGeneration generation = PropertyGeneration.parse( generationName );
		valueBinding.setGeneration( generation );

        if ( generation == PropertyGeneration.ALWAYS || generation == PropertyGeneration.INSERT ) {
	        // generated properties can *never* be insertable...
	        if ( valueBinding.isInsertable() ) {
		        if ( insertAttribute == null ) {
			        // insertable simply because the user did not specify anything; just override it
					valueBinding.setInsertable( false );
		        }
		        else {
			        // the user specifically supplied insert="true", which constitutes an illegal combo
					throw new MappingException(
							"cannot specify both insert=\"true\" and generated=\"" + generation.getName() +
							"\" for property: " +
							propertyName
					);
		        }
	        }

	        // properties generated on update can never be updateable...
	        if ( valueBinding.isUpdateable() && generation == PropertyGeneration.ALWAYS ) {
		        if ( updateAttribute == null ) {
			        // updateable only because the user did not specify
			        // anything; just override it
			        valueBinding.setUpdateable( false );
		        }
		        else {
			        // the user specifically supplied update="true",
			        // which constitutes an illegal combo
					throw new MappingException(
							"cannot specify both update=\"true\" and generated=\"" + generation.getName() +
							"\" for property: " +
							propertyName
					);
		        }
	        }
        }

		boolean isLazyable = "property".equals( propertyElement.getName() )
				|| "component".equals( propertyElement.getName() )
				|| "many-to-one".equals( propertyElement.getName() )
				|| "one-to-one".equals( propertyElement.getName() )
				|| "any".equals( propertyElement.getName() );
		if ( isLazyable ) {
			Attribute lazyNode = propertyElement.attribute( "lazy" );
			valueBinding.setLazy( lazyNode != null && "true".equals( lazyNode.getValue() ) );
		}

	}
}
