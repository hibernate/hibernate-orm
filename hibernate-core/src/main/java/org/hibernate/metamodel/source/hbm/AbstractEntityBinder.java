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

import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.BagBinding;
import org.hibernate.metamodel.binding.CollectionElementType;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.binding.ManyToOneAttributeBinding;
import org.hibernate.metamodel.binding.SimpleAttributeBinding;
import org.hibernate.metamodel.binding.state.ManyToOneAttributeBindingState;
import org.hibernate.metamodel.binding.state.PluralAttributeBindingState;
import org.hibernate.metamodel.binding.state.SimpleAttributeBindingState;
import org.hibernate.metamodel.domain.Hierarchical;
import org.hibernate.metamodel.relational.Schema;
import org.hibernate.metamodel.relational.Table;
import org.hibernate.metamodel.relational.TableSpecification;
import org.hibernate.metamodel.relational.UniqueKey;
import org.hibernate.metamodel.relational.state.ManyToOneRelationalState;
import org.hibernate.metamodel.relational.state.TupleRelationalState;
import org.hibernate.metamodel.relational.state.ValueRelationalState;
import org.hibernate.metamodel.source.hbm.state.binding.HbmEntityBindingState;
import org.hibernate.metamodel.source.hbm.state.binding.HbmManyToOneAttributeBindingState;
import org.hibernate.metamodel.source.hbm.state.binding.HbmPluralAttributeBindingState;
import org.hibernate.metamodel.source.hbm.state.binding.HbmSimpleAttributeBindingState;
import org.hibernate.metamodel.source.hbm.state.relational.HbmManyToOneRelationalStateContainer;
import org.hibernate.metamodel.source.hbm.state.relational.HbmSimpleValueRelationalStateContainer;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLAnyElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLBagElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLComponentElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLDynamicComponentElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLFilterElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLIdbagElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLJoinElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLJoinedSubclassElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLListElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLManyToOneElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLMapElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLOneToOneElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLPropertiesElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLPropertyElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLQueryElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLResultsetElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSetElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSqlQueryElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSubclassElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLTuplizerElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLUnionSubclassElement;
import org.hibernate.metamodel.source.spi.MetadataImplementor;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
abstract class AbstractEntityBinder {
	private final HbmBindingContext bindingContext;
	private final Schema.Name schemaName;

	AbstractEntityBinder(HbmBindingContext bindingContext, XMLHibernateMapping.XMLClass entityClazz) {
		this.bindingContext = bindingContext;
		this.schemaName = new Schema.Name(
				entityClazz.getSchema() == null
						? bindingContext.getMappingDefaults().getSchemaName()
						: entityClazz.getSchema(),
				entityClazz.getCatalog() == null
						? bindingContext.getMappingDefaults().getCatalogName() :
						entityClazz.getCatalog()
		);
	}

	public boolean isRoot() {
		return false;
	}

	public abstract InheritanceType getInheritanceType();

	public HbmBindingContext getBindingContext() {
		return bindingContext;
	}

	protected MetadataImplementor getMetadata() {
		return bindingContext.getMetadataImplementor();
	}

	protected Schema.Name getSchemaName() {
		return schemaName;
	}

	protected NamingStrategy getNamingStrategy() {
		return getMetadata().getOptions().getNamingStrategy();
	}

	protected void basicEntityBinding(
			XMLHibernateMapping.XMLClass entityClazz,
			EntityBinding entityBinding,
			Hierarchical superType) {
		entityBinding.initialize(
				bindingContext,
				new HbmEntityBindingState(
						superType,
						entityClazz,
						isRoot(),
						getInheritanceType(),
						bindingContext
				)
		);

		final String entityName = entityBinding.getEntity().getName();

		if ( entityClazz.getFetchProfile() != null ) {
			bindingContext.bindFetchProfiles( entityClazz.getFetchProfile(), entityName );
		}

		getMetadata().addImport( entityName, entityName );
		if ( bindingContext.isAutoImport() ) {
			if ( entityName.indexOf( '.' ) > 0 ) {
				getMetadata().addImport( StringHelper.unqualify( entityName ), entityName );
			}
		}
	}

	protected String getDefaultAccess() {
		return bindingContext.getMappingDefaults().getPropertyAccessorName();
	}

	/**
	 * Locate any explicit tuplizer definition in the metadata, for the given entity-mode.
	 *
	 * @param container The containing element (representing the entity/component)
	 * @param entityMode The entity-mode for which to locate the tuplizer element
	 *
	 * @return The tuplizer element, or null.
	 */
	private static XMLTuplizerElement locateTuplizerDefinition(XMLHibernateMapping.XMLClass container,
															   EntityMode entityMode) {
		for ( XMLTuplizerElement tuplizer : container.getTuplizer() ) {
			if ( entityMode.toString().equals( tuplizer.getEntityMode() ) ) {
				return tuplizer;
			}
		}
		return null;
	}

	protected String getClassTableName(
			XMLHibernateMapping.XMLClass entityClazz,
			EntityBinding entityBinding,
			Table denormalizedSuperTable) {
		final String entityName = entityBinding.getEntity().getName();
		String logicalTableName;
		String physicalTableName;
		if ( entityClazz.getTable() == null ) {
			logicalTableName = StringHelper.unqualify( entityName );
			physicalTableName = getMetadata()
					.getOptions()
					.getNamingStrategy()
					.classToTableName( entityName );
		}
		else {
			logicalTableName = entityClazz.getTable();
			physicalTableName = getMetadata()
					.getOptions()
					.getNamingStrategy()
					.tableName( logicalTableName );
		}
// todo : find out the purpose of these logical bindings
//			mappings.addTableBinding( schema, catalog, logicalTableName, physicalTableName, denormalizedSuperTable );
		return physicalTableName;
	}

	protected void buildAttributeBindings(XMLHibernateMapping.XMLClass entityClazz,
										  EntityBinding entityBinding) {
		// null = UniqueKey (we are not binding a natural-id mapping)
		// true = mutable, by default properties are mutable
		// true = nullable, by default properties are nullable.
		buildAttributeBindings( entityClazz, entityBinding, null, true, true );
	}

	/**
	 * This form is essentially used to create natural-id mappings.  But the processing is the same, aside from these
	 * extra parameterized values, so we encapsulate it here.
	 *
	 * @param entityClazz
	 * @param entityBinding
	 * @param uniqueKey
	 * @param mutable
	 * @param nullable
	 */
	protected void buildAttributeBindings(
			XMLHibernateMapping.XMLClass entityClazz,
			EntityBinding entityBinding,
			UniqueKey uniqueKey,
			boolean mutable,
			boolean nullable) {
		final boolean naturalId = uniqueKey != null;

		final String entiytName = entityBinding.getEntity().getName();
		final TableSpecification tabe = entityBinding.getBaseTable();

		AttributeBinding attributeBinding = null;
		for ( Object attribute : entityClazz.getPropertyOrManyToOneOrOneToOne() ) {
			if ( XMLBagElement.class.isInstance( attribute ) ) {
				XMLBagElement collection = XMLBagElement.class.cast( attribute );
				BagBinding collectionBinding = makeBagAttributeBinding( collection, entityBinding );
				bindingContext.getMetadataImplementor().addCollection( collectionBinding );
				attributeBinding = collectionBinding;
			}
			else if ( XMLIdbagElement.class.isInstance( attribute ) ) {
				XMLIdbagElement collection = XMLIdbagElement.class.cast( attribute );
				//BagBinding collectionBinding = entityBinding.makeBagAttributeBinding( collection.getName() );
				//bindIdbag( collection, bagBinding, entityBinding, PluralAttributeNature.BAG, collection.getName() );
				// todo: handle identifier
				//attributeBinding = collectionBinding;
				//hibernateMappingBinder.getHibernateXmlBinder().getMetadata().addCollection( attributeBinding );
			}
			else if ( XMLSetElement.class.isInstance( attribute ) ) {
				XMLSetElement collection = XMLSetElement.class.cast( attribute );
				//BagBinding collectionBinding = entityBinding.makeBagAttributeBinding( collection.getName() );
				//bindSet( collection, collectionBinding, entityBinding, PluralAttributeNature.SET, collection.getName() );
				//attributeBinding = collectionBinding;
				//hibernateMappingBinder.getHibernateXmlBinder().getMetadata().addCollection( attributeBinding );
			}
			else if ( XMLListElement.class.isInstance( attribute ) ) {
				XMLListElement collection = XMLListElement.class.cast( attribute );
				//ListBinding collectionBinding = entityBinding.makeBagAttributeBinding( collection.getName() );
				//bindList( collection, bagBinding, entityBinding, PluralAttributeNature.LIST, collection.getName() );
				// todo : handle list index
				//attributeBinding = collectionBinding;
				//hibernateMappingBinder.getHibernateXmlBinder().getMetadata().addCollection( attributeBinding );
			}
			else if ( XMLMapElement.class.isInstance( attribute ) ) {
				XMLMapElement collection = XMLMapElement.class.cast( attribute );
				//BagBinding bagBinding = entityBinding.makeBagAttributeBinding( collection.getName() );
				//bindMap( collection, bagBinding, entityBinding, PluralAttributeNature.MAP, collection.getName() );
				// todo : handle map key
				//hibernateMappingBinder.getHibernateXmlBinder().getMetadata().addCollection( attributeBinding );
			}
			else if ( XMLManyToOneElement.class.isInstance( attribute ) ) {
				XMLManyToOneElement manyToOne = XMLManyToOneElement.class.cast( attribute );
				attributeBinding =  makeManyToOneAttributeBinding( manyToOne, entityBinding );
			}
			else if ( XMLAnyElement.class.isInstance( attribute ) ) {
// todo : implement
//				value = new Any( mappings, table );
//				bindAny( subElement, (Any) value, nullable, mappings );
			}
			else if ( XMLOneToOneElement.class.isInstance( attribute ) ) {
// todo : implement
//				value = new OneToOne( mappings, table, persistentClass );
//				bindOneToOne( subElement, (OneToOne) value, propertyName, true, mappings );
			}
			else if ( XMLPropertyElement.class.isInstance( attribute ) ) {
				XMLPropertyElement property = XMLPropertyElement.class.cast( attribute );
				attributeBinding = bindProperty( property, entityBinding );
			}
			else if ( XMLComponentElement.class.isInstance( attribute )
					|| XMLDynamicComponentElement.class.isInstance( attribute )
					|| XMLPropertiesElement.class.isInstance( attribute ) ) {
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
		}

		/*
Array
PrimitiveArray
*/
		for ( XMLJoinElement join : entityClazz.getJoin() ) {
// todo : implement
//			Join join = new Join();
//			join.setPersistentClass( persistentClass );
//			bindJoin( subElement, join, mappings, inheritedMetas );
//			persistentClass.addJoin( join );
		}
		for ( XMLSubclassElement subclass : entityClazz.getSubclass() ) {
// todo : implement
//			handleSubclass( persistentClass, mappings, subElement, inheritedMetas );
		}
		for ( XMLJoinedSubclassElement subclass : entityClazz.getJoinedSubclass() ) {
// todo : implement
//			handleJoinedSubclass( persistentClass, mappings, subElement, inheritedMetas );
		}
		for ( XMLUnionSubclassElement subclass : entityClazz.getUnionSubclass() ) {
// todo : implement
//			handleUnionSubclass( persistentClass, mappings, subElement, inheritedMetas );
		}
		for ( XMLFilterElement filter : entityClazz.getFilter() ) {
// todo : implement
//				parseFilter( subElement, entityBinding );
		}
		if ( entityClazz.getNaturalId() != null ) {
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
		if ( entityClazz.getQueryOrSqlQuery() != null ) {
			for ( Object queryOrSqlQuery : entityClazz.getQueryOrSqlQuery() ) {
				if ( XMLQueryElement.class.isInstance( queryOrSqlQuery ) ) {
// todo : implement
//				bindNamedQuery(subElement, persistentClass.getEntityName(), mappings);
				}
				else if ( XMLSqlQueryElement.class.isInstance( queryOrSqlQuery ) ) {
// todo : implement
//			bindNamedSQLQuery(subElement, persistentClass.getEntityName(), mappings);
				}
			}
		}
		if ( entityClazz.getResultset() != null ) {
			for ( XMLResultsetElement resultSet : entityClazz.getResultset() ) {
// todo : implement
//				bindResultSetMappingDefinition( subElement, persistentClass.getEntityName(), mappings );
			}
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

	protected SimpleAttributeBinding bindProperty(
			XMLPropertyElement property,
			EntityBinding entityBinding) {
		SimpleAttributeBindingState bindingState = new HbmSimpleAttributeBindingState(
				entityBinding.getEntity().getJavaType().getName(),
				bindingContext,
				entityBinding.getMetaAttributeContext(),
				property
		);

		// boolean (true here) indicates that by default column names should be guessed
		ValueRelationalState relationalState =
				convertToSimpleValueRelationalStateIfPossible(
						new HbmSimpleValueRelationalStateContainer(
								bindingContext,
								true,
								property
						)
				);

		entityBinding.getEntity().getOrCreateSingularAttribute( bindingState.getAttributeName() );
		return entityBinding.makeSimpleAttributeBinding( bindingState.getAttributeName() )
				.initialize( bindingState )
				.initialize( relationalState );
	}

	protected static ValueRelationalState convertToSimpleValueRelationalStateIfPossible(ValueRelationalState state) {
	// TODO: should a single-valued tuple always be converted???
		if ( !TupleRelationalState.class.isInstance( state ) ) {
			return state;
		}
		TupleRelationalState tupleRelationalState = TupleRelationalState.class.cast( state );
		return tupleRelationalState.getRelationalStates().size() == 1 ?
				tupleRelationalState.getRelationalStates().get( 0 ) :
				state;
	}

	protected BagBinding makeBagAttributeBinding(
			XMLBagElement collection,
			EntityBinding entityBinding) {

		PluralAttributeBindingState bindingState =
				new HbmPluralAttributeBindingState(
						entityBinding.getEntity().getJavaType().getName(),
						bindingContext,
						entityBinding.getMetaAttributeContext(),
						collection
				);

		BagBinding collectionBinding = entityBinding.makeBagAttributeBinding(
				bindingState.getAttributeName(),
				getCollectionElementType( collection ) )
				.initialize( bindingState );

			// todo : relational model binding
		return collectionBinding;
	}

	private CollectionElementType getCollectionElementType(XMLBagElement collection) {
		if ( collection.getElement() != null ) {
			return CollectionElementType.BASIC;
		}
		else if ( collection.getCompositeElement() != null ) {
			return CollectionElementType.COMPOSITE;
		}
		else if ( collection.getManyToMany() != null ) {
			return CollectionElementType.MANY_TO_MANY;
		}
		else if ( collection.getOneToMany() != null ) {
			return CollectionElementType.ONE_TO_MANY;
		}
		else if ( collection.getManyToAny() != null ) {
			return CollectionElementType.MANY_TO_ANY;
		}
		else {
			throw new AssertionFailure( "Unknown collection element type: " + collection );
		}
	}

	private ManyToOneAttributeBinding makeManyToOneAttributeBinding(XMLManyToOneElement manyToOne,
							   EntityBinding entityBinding) {
		ManyToOneAttributeBindingState bindingState =
				new HbmManyToOneAttributeBindingState(
						entityBinding.getEntity().getJavaType().getName(),
						bindingContext,
						entityBinding.getMetaAttributeContext(),
						manyToOne
				);

		// boolean (true here) indicates that by default column names should be guessed
		ManyToOneRelationalState relationalState =
						new HbmManyToOneRelationalStateContainer(
								bindingContext,
								true,
								manyToOne
						);

	    entityBinding.getEntity().getOrCreateSingularAttribute( bindingState.getAttributeName() );
		ManyToOneAttributeBinding manyToOneAttributeBinding =
				entityBinding.makeManyToOneAttributeBinding( bindingState.getAttributeName() )
						.initialize( bindingState )
						.initialize( relationalState );

		return manyToOneAttributeBinding;
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