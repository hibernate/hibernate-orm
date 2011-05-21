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

import org.hibernate.InvalidMappingException;
import org.hibernate.MappingException;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.metamodel.binding.Caching;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.state.SimpleAttributeBindingState;
import org.hibernate.metamodel.relational.Identifier;
import org.hibernate.metamodel.relational.InLineView;
import org.hibernate.metamodel.relational.Schema;
import org.hibernate.metamodel.relational.state.ValueRelationalState;
import org.hibernate.metamodel.source.hbm.state.binding.HbmDiscriminatorBindingState;
import org.hibernate.metamodel.source.hbm.state.binding.HbmSimpleAttributeBindingState;
import org.hibernate.metamodel.source.hbm.state.relational.HbmSimpleValueRelationalStateContainer;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLCacheElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping.XMLClass;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping.XMLClass.XMLCompositeId;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping.XMLClass.XMLId;
import org.hibernate.metamodel.binding.state.DiscriminatorBindingState;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
class RootEntityBinder extends AbstractEntityBinder {

	RootEntityBinder(HibernateMappingBinder hibernateMappingBinder, XMLClass xmlClazz) {
		super( hibernateMappingBinder, xmlClazz );
	}

	public void process(XMLClass xmlClazz) {
		String entityName = getHibernateMappingBinder().extractEntityName( xmlClazz );
		if ( entityName == null ) {
			throw new MappingException( "Unable to determine entity name" );
		}

		EntityBinding entityBinding = new EntityBinding();
		basicEntityBinding( xmlClazz, entityBinding, null );
		basicTableBinding( xmlClazz, entityBinding );

		entityBinding.setMutable( xmlClazz.isMutable() );

		if ( xmlClazz.getWhere() != null ) {
			entityBinding.setWhereFilter( xmlClazz.getWhere() );
		}

		if ( xmlClazz.getPolymorphism() != null ) {
			entityBinding.setExplicitPolymorphism( "explicit".equals( xmlClazz.getPolymorphism() ) );
		}

		if ( xmlClazz.getRowid() != null ) {
			entityBinding.setRowId( xmlClazz.getRowid() );
		}

		bindIdentifier( xmlClazz, entityBinding );
		bindDiscriminator( xmlClazz, entityBinding );
		bindVersionOrTimestamp( xmlClazz, entityBinding );
		bindCaching( xmlClazz, entityBinding );

		// called createClassProperties in HBMBinder...
		buildAttributeBindings( xmlClazz, entityBinding );

		getHibernateXmlBinder().getMetadata().addEntity( entityBinding );
	}

	private void basicTableBinding(XMLClass xmlClazz,
								   EntityBinding entityBinding) {
		final Schema schema = getHibernateXmlBinder().getMetadata().getDatabase().getSchema( getSchemaName() );

		final String subSelect =
				xmlClazz.getSubselectAttribute() == null ? xmlClazz.getSubselect() : xmlClazz.getSubselectAttribute();
		if ( subSelect != null ) {
			final String logicalName = entityBinding.getEntity().getName();
			InLineView inLineView = schema.getInLineView( logicalName );
			if ( inLineView == null ) {
				inLineView = schema.createInLineView( logicalName, subSelect );
			}
			entityBinding.setBaseTable( inLineView );
		}
		else {
			final Identifier tableName = Identifier.toIdentifier( getClassTableName( xmlClazz, entityBinding, null ) );
			org.hibernate.metamodel.relational.Table table = schema.getTable( tableName );
			if ( table == null ) {
				table = schema.createTable( tableName );
			}
			entityBinding.setBaseTable( table );
			String comment = xmlClazz.getComment();
			if ( comment != null ) {
				table.addComment( comment.trim() );
			}
			String check = xmlClazz.getCheck();
			if ( check != null ) {
				table.addCheckConstraint( check );
			}
		}
	}

	private void bindIdentifier(XMLClass xmlClazz,
								EntityBinding entityBinding) {
		if ( xmlClazz.getId() != null ) {
			bindSimpleId( xmlClazz.getId(), entityBinding );
			return;
		}

		if ( xmlClazz.getCompositeId() != null ) {
			bindCompositeId( xmlClazz.getCompositeId(), entityBinding );
		}

		throw new InvalidMappingException(
				"Entity [" + entityBinding.getEntity().getName() + "] did not contain identifier mapping",
				getHibernateMappingBinder().getOrigin()
		);
	}

	private void bindSimpleId(XMLId id, EntityBinding entityBinding) {
		SimpleAttributeBindingState bindingState = new HbmSimpleAttributeBindingState(
				entityBinding.getEntity().getPojoEntitySpecifics().getClassName(),
				getHibernateMappingBinder(),
				entityBinding.getMetaAttributes(),
				id
		);
		// boolean (true here) indicates that by default column names should be guessed
		HbmSimpleValueRelationalStateContainer relationalStateContainer = new HbmSimpleValueRelationalStateContainer(
				getHibernateMappingBinder(), true, id
		);
		if ( relationalStateContainer.getRelationalStates().size() > 1 ) {
			throw new MappingException( "ID is expected to be a single column, but has more than 1 value" );
		}

		entityBinding.getEntity().getOrCreateSingularAttribute( bindingState.getAttributeName() );
		entityBinding.makeSimpleIdAttributeBinding( bindingState.getAttributeName() )
				.initialize( bindingState )
				.initialize( relationalStateContainer.getRelationalStates().get( 0 ) );

		// if ( propertyName == null || entity.getPojoRepresentation() == null ) {
		// bindSimpleValue( idNode, id, false, RootClass.DEFAULT_IDENTIFIER_COLUMN_NAME, mappings );
		// if ( !id.isTypeSpecified() ) {
		// throw new MappingException( "must specify an identifier type: " + entity.getEntityName()
		// );
		// }
		// }
		// else {
		// bindSimpleValue( idNode, id, false, propertyName, mappings );
		// PojoRepresentation pojo = entity.getPojoRepresentation();
		// id.setTypeUsingReflection( pojo.getClassName(), propertyName );
		//
		// Property prop = new Property();
		// prop.setValue( id );
		// bindProperty( idNode, prop, mappings, inheritedMetas );
		// entity.setIdentifierProperty( prop );
		// }

//		if ( propertyName == null ) {
//			bindSimpleValue( idNode, id, false, RootClass.DEFAULT_IDENTIFIER_COLUMN_NAME, mappings );
//		}
//		else {
//			bindSimpleValue( idNode, id, false, propertyName, mappings );
//		}
//
//		if ( propertyName == null || !entity.hasPojoRepresentation() ) {
//			if ( !id.isTypeSpecified() ) {
//				throw new MappingException( "must specify an identifier type: "
//					+ entity.getEntityName() );
//			}
//		}
//		else {
//			id.setTypeUsingReflection( entity.getClassName(), propertyName );
//		}
//
//		if ( propertyName != null ) {
//			Property prop = new Property();
//			prop.setValue( id );
//			bindProperty( idNode, prop, mappings, inheritedMetas );
//			entity.setIdentifierProperty( prop );
//		}

		// TODO:
		/*
		 * if ( id.getHibernateType().getReturnedClass().isArray() ) throw new MappingException(
		 * "illegal use of an array as an identifier (arrays don't reimplement equals)" );
		 */
//		makeIdentifier( idNode, id, mappings );
	}

	private static void bindCompositeId(XMLCompositeId compositeId, EntityBinding entityBinding) {
		final String explicitName = compositeId.getName();

//		String propertyName = idNode.attributeValue( "name" );
//		Component id = new Component( mappings, entity );
//		entity.setIdentifier( id );
//		bindCompositeId( idNode, id, entity, propertyName, mappings, inheritedMetas );
//		if ( propertyName == null ) {
//			entity.setEmbeddedIdentifier( id.isEmbedded() );
//			if ( id.isEmbedded() ) {
//				// todo : what is the implication of this?
//				id.setDynamic( !entity.hasPojoRepresentation() );
//				/*
//				 * Property prop = new Property(); prop.setName("id");
//				 * prop.setPropertyAccessorName("embedded"); prop.setValue(id);
//				 * entity.setIdentifierProperty(prop);
//				 */
//			}
//		}
//		else {
//			Property prop = new Property();
//			prop.setValue( id );
//			bindProperty( idNode, prop, mappings, inheritedMetas );
//			entity.setIdentifierProperty( prop );
//		}
//
//		makeIdentifier( idNode, id, mappings );

	}

	private void bindDiscriminator(XMLClass xmlEntityClazz,
								   EntityBinding entityBinding) {
		if ( xmlEntityClazz.getDiscriminator() == null ) {
			return;
		}

		DiscriminatorBindingState bindingState = new HbmDiscriminatorBindingState(
						entityBinding.getEntity().getPojoEntitySpecifics().getClassName(),
						getHibernateMappingBinder(),
						xmlEntityClazz.getDiscriminator()
		);

		// boolean (true here) indicates that by default column names should be guessed
		ValueRelationalState relationalState = convertToSimpleValueRelationalStateIfPossible(
				new HbmSimpleValueRelationalStateContainer(
						getHibernateMappingBinder(),
						true,
						xmlEntityClazz.getDiscriminator()
				)
		);


		entityBinding.getEntity().getOrCreateSingularAttribute( bindingState.getAttributeName() );
		entityBinding.makeEntityDiscriminator( bindingState.getAttributeName() )
				.initialize( bindingState )
				.initialize( relationalState );
	}

	private void bindVersionOrTimestamp(XMLClass xmlEntityClazz,
										EntityBinding entityBinding) {
		if ( xmlEntityClazz.getVersion() != null ) {
			bindVersion(
					xmlEntityClazz.getVersion(),
					entityBinding
			);
		}
		else if ( xmlEntityClazz.getTimestamp() != null ) {
			bindTimestamp(
					xmlEntityClazz.getTimestamp(),
					entityBinding
			);
		}
	}

	protected void bindVersion(XMLHibernateMapping.XMLClass.XMLVersion version,
									   EntityBinding entityBinding) {
		SimpleAttributeBindingState bindingState =
				new HbmSimpleAttributeBindingState(
							entityBinding.getEntity().getPojoEntitySpecifics().getClassName(),
							getHibernateMappingBinder(),
							entityBinding.getMetaAttributes(),
							version
				);

		// boolean (true here) indicates that by default column names should be guessed
		ValueRelationalState relationalState =
				convertToSimpleValueRelationalStateIfPossible(
						new HbmSimpleValueRelationalStateContainer(
								getHibernateMappingBinder(),
								true,
								version
						)
				);

		entityBinding.getEntity().getOrCreateSingularAttribute( bindingState.getAttributeName() );
		entityBinding.makeVersionBinding( bindingState.getAttributeName() )
				.initialize( bindingState )
				.initialize( relationalState );
	}

	protected void bindTimestamp(XMLHibernateMapping.XMLClass.XMLTimestamp timestamp,
								 EntityBinding entityBinding) {

		SimpleAttributeBindingState bindingState =
				new HbmSimpleAttributeBindingState(
					entityBinding.getEntity().getPojoEntitySpecifics().getClassName(),
					getHibernateMappingBinder(),
					entityBinding.getMetaAttributes(),
					timestamp
				);

		// relational model has not been bound yet
		// boolean (true here) indicates that by default column names should be guessed
		ValueRelationalState relationalState =
				convertToSimpleValueRelationalStateIfPossible(
						new HbmSimpleValueRelationalStateContainer(
								getHibernateMappingBinder(),
								true,
								timestamp
						)
				);

		entityBinding.makeVersionBinding( bindingState.getAttributeName() )
				.initialize( bindingState )
		 		.initialize( relationalState );
	}

	private void bindCaching(XMLClass xmlClazz,
							 EntityBinding entityBinding) {
		XMLCacheElement cache = xmlClazz.getCache();
		if ( cache == null ) {
			return;
		}
		final String region = cache.getRegion() != null ? cache.getRegion() : entityBinding.getEntity().getName();
		final AccessType accessType = Enum.valueOf( AccessType.class, cache.getUsage() );
		final boolean cacheLazyProps = !"non-lazy".equals( cache.getInclude() );
		entityBinding.setCaching( new Caching( region, accessType, cacheLazyProps ) );
	}
}
