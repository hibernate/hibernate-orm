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

import java.util.Iterator;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.hibernate.EntityMode;
import org.hibernate.MappingException;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.engine.Versioning;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.BagBinding;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.PluralAttributeBinding;
import org.hibernate.metamodel.binding.SimpleAttributeBinding;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.domain.Hierarchical;
import org.hibernate.metamodel.domain.PluralAttributeNature;
import org.hibernate.metamodel.relational.Schema;
import org.hibernate.metamodel.relational.Table;
import org.hibernate.metamodel.relational.TableSpecification;
import org.hibernate.metamodel.relational.UniqueKey;
import org.hibernate.metamodel.source.Metadata;
import org.hibernate.metamodel.source.hbm.state.domain.HbmPluralAttributeDomainState;
import org.hibernate.metamodel.source.hbm.state.domain.HbmSimpleAttributeDomainState;
import org.hibernate.metamodel.source.hbm.state.relational.HbmSimpleValueRelationalStateContainer;

/**
* TODO : javadoc
*
* @author Steve Ebersole
*/
abstract class AbstractEntityBinder {
	private final HibernateMappingBinder hibernateMappingBinder;
	private final Schema.Name schemaName;

	AbstractEntityBinder(HibernateMappingBinder hibernateMappingBinder, Element entityElement) {
		this.hibernateMappingBinder = hibernateMappingBinder;

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

	protected HibernateMappingBinder getHibernateMappingBinder() {
		return hibernateMappingBinder;
	}

	protected HibernateXmlBinder getHibernateXmlBinder() {
		return hibernateMappingBinder.getHibernateXmlBinder();
	}

	protected Metadata getMetadata() {
		return hibernateMappingBinder.getHibernateXmlBinder().getMetadata();
	}

	protected Schema.Name getSchemaName() {
		return schemaName;
	}
	protected NamingStrategy getNamingStrategy() {
		return getMetadata().getNamingStrategy();
	}

	protected void basicEntityBinding(Element node, EntityBinding entityBinding, Hierarchical superType) {
		entityBinding.fromHbmXml(
				hibernateMappingBinder,
				node,
				new Entity( hibernateMappingBinder.extractEntityName( node ), superType )
		);
		// TODO: move this stuff out
		// transfer an explicitly defined lazy attribute
		bindPojoRepresentation( node, entityBinding );
		bindDom4jRepresentation( node, entityBinding );
		bindMapRepresentation( node, entityBinding );

		final String entityName = entityBinding.getEntity().getName();
		Iterator itr = node.elementIterator( "fetch-profile" );
		while ( itr.hasNext() ) {
			final Element profileElement = ( Element ) itr.next();
			hibernateMappingBinder.parseFetchProfile( profileElement, entityName );
		}

		getMetadata().addImport( entityName, entityName );
		if ( hibernateMappingBinder.isAutoImport() ) {
			if ( entityName.indexOf( '.' ) > 0 ) {
				getMetadata().addImport( StringHelper.unqualify( entityName ), entityName );
			}
		}
	}

	protected String getDefaultAccess() {
		return hibernateMappingBinder.getDefaultAccess();
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

	protected void bindSimpleAttribute(Element propertyElement,
									   SimpleAttributeBinding attributeBinding,
									   EntityBinding entityBinding,
									   String attributeName) {
		if ( attributeBinding.getAttribute() == null ) {
			attributeBinding.initialize(
					new HbmSimpleAttributeDomainState(
							hibernateMappingBinder,
							propertyElement,
							entityBinding.getEntity().getOrCreateSingularAttribute( attributeName )
					)
			);
		}

		if ( attributeBinding.getValue() == null ) {
			// relational model has not been bound yet
			// boolean (true here) indicates that by default column names should be guessed
			attributeBinding.initializeTupleValue(
					new HbmSimpleValueRelationalStateContainer(
							getHibernateMappingBinder(),
							propertyElement,
							true
					)
			);
		}
	}

	protected void bindCollection(
			Element collectionNode,
			PluralAttributeBinding collectionBinding,
			EntityBinding entityBinding,
			PluralAttributeNature attributeNature,
			String attributeName) {
		if ( collectionBinding.getAttribute() == null ) {
			// domain model has not been bound yet
			collectionBinding.initialize(
					new HbmPluralAttributeDomainState(
							hibernateMappingBinder,
							collectionNode,
							entityBinding.getEntity().getOrCreatePluralAttribute( attributeName, attributeNature )
					)
			);
		}

		if ( collectionBinding.getValue() == null ) {
			// todo : relational model binding
		}
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


//	protected HbmRelationalState processValues(Element identifierElement, TableSpecification baseTable, String propertyPath, boolean isSimplePrimaryKey) {
		// first boolean (false here) indicates that by default columns are nullable
		// second boolean (true here) indicates that by default column names should be guessed
// todo : logical 1-1 handling
//			final Attribute uniqueAttribute = node.attribute( "unique" );
//			if ( uniqueAttribute != null
//					&& "true".equals( uniqueAttribute.getValue() )
//					&& ManyToOne.class.isInstance( simpleValue ) ) {
//				( (ManyToOne) simpleValue ).markAsLogicalOneToOne();
//			}
		//return processValues( identifierElement, baseTable, false, true, propertyPath, isSimplePrimaryKey );



}