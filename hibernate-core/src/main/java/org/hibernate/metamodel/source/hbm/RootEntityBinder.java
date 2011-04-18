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

import org.dom4j.Attribute;
import org.dom4j.Element;

import org.hibernate.InvalidMappingException;
import org.hibernate.MappingException;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.binding.Caching;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.SimpleAttributeBinding;
import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.relational.Identifier;
import org.hibernate.metamodel.relational.InLineView;
import org.hibernate.metamodel.relational.Schema;
import org.hibernate.metamodel.source.util.MappingHelper;

/**
* TODO : javadoc
*
* @author Steve Ebersole
*/
class RootEntityBinder extends AbstractEntityBinder {

	RootEntityBinder(HibernateMappingBinder hibernateMappingBinder, org.hibernate.metamodel.source.hbm.xml.mapping.Class xmlClazz) {
		super( hibernateMappingBinder, xmlClazz );
	}

	public void process(org.hibernate.metamodel.source.hbm.xml.mapping.Class xmlClazz) {
		String entityName = getHibernateMappingBinder().extractEntityName( xmlClazz );
		if ( entityName == null ) {
			throw new MappingException( "Unable to determine entity name" );
		}

		EntityBinding entityBinding = new EntityBinding();
		basicEntityBinding( xmlClazz, entityBinding, null );
		basicTableBinding( xmlClazz, entityBinding );

		if ( xmlClazz.getMutable() != null ) {
			entityBinding.setMutable( Boolean.valueOf( xmlClazz.getMutable() ) );
		}

		if ( xmlClazz.getWhere() != null ) {
			entityBinding.setWhereFilter( xmlClazz.getWhere() );
		}

		if ( xmlClazz.getPolymorphism() != null ) {
			entityBinding.setExplicitPolymorphism( "explicit".equals(  xmlClazz.getPolymorphism() ) );
		}

		if ( xmlClazz.getRowid() != null ) {
			entityBinding.setRowId( xmlClazz.getRowid() );
		}

		bindIdentifier( xmlClazz, entityBinding );
		bindDiscriminator( xmlClazz, entityBinding );
		bindVersion( xmlClazz, entityBinding );
		bindCaching( xmlClazz, entityBinding );

		// called createClassProperties in HBMBinder...
		buildAttributeBindings( xmlClazz, entityBinding );

		getHibernateXmlBinder().getMetadata().addEntity( entityBinding );
	}

	private void basicTableBinding(org.hibernate.metamodel.source.hbm.xml.mapping.Class xmlClazz,
								   EntityBinding entityBinding) {
		final Schema schema = getHibernateXmlBinder().getMetadata().getDatabase().getSchema( getSchemaName() );

		final String subSelect =
				xmlClazz.getSubselect() == null ? xmlClazz.getSubselectElement() : xmlClazz.getSubselect();
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

	private void bindIdentifier(org.hibernate.metamodel.source.hbm.xml.mapping.Class xmlClazz,
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

	private void bindSimpleId(org.hibernate.metamodel.source.hbm.xml.mapping.Id id, EntityBinding entityBinding) {
		// Handle the domain portion of the binding...
		final String explicitName = id.getName();
		final String attributeName = explicitName == null ? RootClass.DEFAULT_IDENTIFIER_COLUMN_NAME : explicitName;
		SimpleAttributeBinding idBinding = entityBinding.makeSimplePrimaryKeyAttributeBinding( attributeName );
		bindSimpleAttribute( id, idBinding, entityBinding, attributeName );

		if ( ! Column.class.isInstance( idBinding.getValue() ) ) {
			// this should never ever happen..
			throw new MappingException( "Unanticipated situation" );
		}

		entityBinding.getBaseTable().getPrimaryKey().addColumn( Column.class.cast( idBinding.getValue() ) );
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

	private static void bindCompositeId(org.hibernate.metamodel.source.hbm.xml.mapping.CompositeId compositeId, EntityBinding entityBinding) {
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

	private void bindDiscriminator(org.hibernate.metamodel.source.hbm.xml.mapping.Class xmlEntityClazz,
								   EntityBinding entityBinding) {
		if ( xmlEntityClazz.getDiscriminator() == null ) {
			return;
		}

		// Discriminator.getName() is not defined, so the attribute will always be RootClass.DEFAULT_DISCRIMINATOR_COLUMN_NAME
		SimpleAttributeBinding discriminatorBinding = entityBinding.makeEntityDiscriminatorBinding( RootClass.DEFAULT_DISCRIMINATOR_COLUMN_NAME );

		// Handle the relational portion of the binding...
		bindSimpleAttribute( xmlEntityClazz.getDiscriminator(), discriminatorBinding, entityBinding, RootClass.DEFAULT_DISCRIMINATOR_COLUMN_NAME );

		entityBinding.getEntityDiscriminator().setForced( MappingHelper.getBooleanValue( xmlEntityClazz.getDiscriminator().getForce(), false ) );
	}

	private void bindVersion(org.hibernate.metamodel.source.hbm.xml.mapping.Class xmlEntityClazz,
							 EntityBinding entityBinding) {
		if ( xmlEntityClazz.getVersion() == null && xmlEntityClazz.getTimestamp() == null ) {
			return;
		}

		boolean isVersion = xmlEntityClazz.getVersion() != null;
		String explicitName = isVersion ? xmlEntityClazz.getVersion().getName() : xmlEntityClazz.getTimestamp().getName();
		if ( explicitName == null ) {
			throw new MappingException( "Mising property name for version/timestamp mapping [" + entityBinding.getEntity().getName() + "]" );
		}
		SimpleAttributeBinding versionBinding = entityBinding.makeVersionBinding( explicitName );
		if ( isVersion ) {
			bindSimpleAttribute(
					xmlEntityClazz.getVersion(),
					versionBinding,
					entityBinding,
					explicitName
			);
		}
		else {
			bindSimpleAttribute(
					xmlEntityClazz.getTimestamp(),
					versionBinding,
					entityBinding,
					explicitName
			);
		}
	}

	private void bindCaching(org.hibernate.metamodel.source.hbm.xml.mapping.Class xmlClazz,
							 EntityBinding entityBinding) {
		org.hibernate.metamodel.source.hbm.xml.mapping.Cache cache = xmlClazz.getCache();
		if ( cache == null ) {
			return;
		}
		final String region = cache.getRegion() != null ? cache.getRegion() : entityBinding.getEntity().getName();
		final String strategy = cache.getUsage();
		final boolean cacheLazyProps = !"non-lazy".equals( cache.getInclude() );
		entityBinding.setCaching( new Caching( region, strategy, cacheLazyProps ) );
	}

}
