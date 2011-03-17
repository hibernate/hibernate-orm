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
import org.hibernate.metamodel.relational.Tuple;
import org.hibernate.metamodel.relational.Value;

/**
* TODO : javadoc
*
* @author Steve Ebersole
*/
class RootEntityBinder extends AbstractEntityBinder {

	RootEntityBinder(HibernateMappingBinder hibernateMappingBinder, Element entityElement) {
		super( hibernateMappingBinder, entityElement );
	}

	public void process(Element entityElement) {
		EntityBinding entityBinding = new EntityBinding();
		basicEntityBinding( entityElement, entityBinding, null );
		basicTableBinding( entityElement, entityBinding );

		Attribute mutableAttribute = entityElement.attribute( "mutable" );
		if ( mutableAttribute != null ) {
			entityBinding.setMutable( Boolean.valueOf( mutableAttribute.getValue() ) );
		}

		Attribute whereAttribute = entityElement.attribute( "where" );
		if ( whereAttribute != null ) {
			entityBinding.setWhereFilter( whereAttribute.getValue() );
		}

		Attribute polymorphismAttribute = entityElement.attribute( "polymorphism" );
		if ( polymorphismAttribute != null ) {
			entityBinding.setExplicitPolymorphism( "explicit".equals( polymorphismAttribute.getValue() ) );
		}

		Attribute rowidAttribute = entityElement.attribute( "rowid" );
		if ( rowidAttribute != null ) {
			entityBinding.setRowId( rowidAttribute.getValue() );
		}

		bindIdentifier( entityElement, entityBinding );
		bindDiscriminator( entityElement, entityBinding );
		bindVersion( entityElement, entityBinding );
		bindCaching( entityElement, entityBinding );

		// called createClassProperties in HBMBinder...
		buildAttributeBindings( entityElement, entityBinding );

		getHibernateXmlBinder().getMetadata().addEntity( entityBinding );
	}

	private void basicTableBinding(Element entityElement, EntityBinding entityBinding) {
		final Schema schema = getHibernateXmlBinder().getMetadata().getDatabase().getSchema( schemaName );

		final String subSelect = HbmHelper.getSubselect( entityElement );
		if ( subSelect != null ) {
			final String logicalName = entityBinding.getEntity().getName();
			InLineView inLineView = schema.getInLineView( logicalName );
			if ( inLineView == null ) {
				inLineView = schema.createInLineView( logicalName, subSelect );
			}
			entityBinding.setBaseTable( inLineView );
		}
		else {
			final Identifier tableName = Identifier.toIdentifier( getClassTableName( entityElement, entityBinding, null ) );
			org.hibernate.metamodel.relational.Table table = schema.getTable( tableName );
			if ( table == null ) {
				table = schema.createTable( tableName );
			}
			entityBinding.setBaseTable( table );
			Element comment = entityElement.element( "comment" );
			if ( comment != null ) {
				table.addComment( comment.getTextTrim() );
			}
			Attribute checkAttribute = entityElement.attribute( "check" );
			if ( checkAttribute != null ) {
				table.addCheckConstraint( checkAttribute.getValue() );
			}
		}
	}

	private void bindIdentifier(Element entityElement, EntityBinding entityBinding) {
		final Element idElement = entityElement.element( "id" );
		if ( idElement != null ) {
			bindSimpleId( idElement, entityBinding );
			return;
		}

		final Element compositeIdElement = entityElement.element( "composite-id" );
		if ( compositeIdElement != null ) {
			bindCompositeId( compositeIdElement, entityBinding );
		}

		throw new InvalidMappingException(
				"Entity [" + entityBinding.getEntity().getName() + "] did not contain identifier mapping",
				hibernateMappingBinder.getXmlDocument()
		);
	}

	private void bindSimpleId(Element identifierElement, EntityBinding entityBinding) {
		// Handle the domain portion of the binding...
		final String explicitName = identifierElement.attributeValue( "name" );
		final String attributeName = explicitName == null ? RootClass.DEFAULT_IDENTIFIER_COLUMN_NAME : explicitName;
		entityBinding.getEntity().getOrCreateSingularAttribute( attributeName );

		SimpleAttributeBinding idBinding = entityBinding.makeSimpleAttributeBinding( attributeName );

		basicAttributeBinding( identifierElement, idBinding );

		// Handle the relational portion of the binding...
		Value idValue = processValues( identifierElement, entityBinding.getBaseTable(), attributeName );
		idBinding.setValue( idValue );

		// ear-mark this value binding as the identifier...
		entityBinding.getEntityIdentifier().setValueBinding( idBinding );

		if ( idValue instanceof Tuple ) {
			// this should never ever happen..
			throw new MappingException( "Unanticipated situation" );
		}

		entityBinding.getBaseTable().getPrimaryKey().addColumn( (Column) idValue );

//		SimpleValue id = new SimpleValue( mappings, entity.getTable() );
//		entity.setIdentifier( id );

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

	private static void bindCompositeId(Element identifierElement, EntityBinding entityBinding) {
		final String explicitName = identifierElement.attributeValue( "name" );

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

	private void bindDiscriminator(Element entityElement, EntityBinding entityBinding) {
		Element discriminatorElement = entityElement.element( "discriminator" );
		if ( discriminatorElement == null ) {
			return;
		}

		final String explicitName = discriminatorElement.attributeValue( "name" );
		final String attributeName = explicitName == null ? RootClass.DEFAULT_DISCRIMINATOR_COLUMN_NAME : explicitName;
		entityBinding.getEntity().getOrCreateSingularAttribute( attributeName );

		SimpleAttributeBinding discriminatorBinding = entityBinding.makeSimpleAttributeBinding( attributeName );
		basicAttributeBinding( discriminatorElement, discriminatorBinding );
		if ( discriminatorBinding.getHibernateTypeDescriptor().getTypeName() == null ) {
			discriminatorBinding.getHibernateTypeDescriptor().setTypeName( "string" );
		}

		// Handle the relational portion of the binding...
		Value discriminatorValue = processValues( discriminatorElement, entityBinding.getBaseTable(), attributeName );
		discriminatorBinding.setValue( discriminatorValue );

		// ear-mark this value binding as the discriminator...
		entityBinding.makeEntityDiscriminator();
		entityBinding.getEntityDiscriminator().setValueBinding( discriminatorBinding );

		if ( "true".equals( discriminatorElement.attributeValue( "force" ) ) ) {
			entityBinding.getEntityDiscriminator().setForced( true );
		}
		if ( "false".equals( discriminatorElement.attributeValue( "insert" ) ) ) {
			entityBinding.getEntityDiscriminator().setInserted( false );
		}
	}

	private void bindVersion(Element entityElement, EntityBinding entityBinding) {
		Element versioningElement = entityElement.element( "version" );
		if ( versioningElement == null ) {
			versioningElement = entityElement.element( "timestamp" );
		}
		if ( versioningElement == null ) {
			return;
		}

		boolean isVersion = "version".equals( versioningElement.getName() );

		final String explicitName = versioningElement.attributeValue( "name" );
		if ( explicitName == null ) {
			throw new MappingException( "Mising property name for version/timestamp mapping [" + entityBinding.getEntity().getName() + "]" );
		}
		entityBinding.getEntity().getOrCreateSingularAttribute( explicitName );
		SimpleAttributeBinding versionBinding = entityBinding.makeSimpleAttributeBinding( explicitName );
		basicAttributeBinding( versioningElement, versionBinding );

		if ( versionBinding.getHibernateTypeDescriptor().getTypeName() == null ) {
			if ( isVersion ) {
				versionBinding.getHibernateTypeDescriptor().setTypeName( "integer" );
			}
			else {
				final String tsSource = versioningElement.attributeValue( "source" );
				if ( "db".equals( tsSource ) ) {
					versionBinding.getHibernateTypeDescriptor().setTypeName( "dbtimestamp" );
				}
				else {
					versionBinding.getHibernateTypeDescriptor().setTypeName( "timestamp" );
				}
			}
		}

		// Handle the relational portion of the binding...
		Value discriminatorValue = processValues( versioningElement, entityBinding.getBaseTable(), explicitName );
		versionBinding.setValue( discriminatorValue );

		// for version properties marked as being generated, make sure they are "always"
		// generated; aka, "insert" is invalid; this is dis-allowed by the DTD,
		// but just to make sure...
		if ( versionBinding.getGeneration() == PropertyGeneration.INSERT ) {
			throw new MappingException( "'generated' attribute cannot be 'insert' for versioning property" );
		}

		entityBinding.setVersioningValueBinding( versionBinding );
	}

	private void bindCaching(Element entityElement, EntityBinding entityBinding) {
		final Element cacheElement = entityElement.element( "cache" );
		if ( cacheElement == null ) {
			return;
		}
		final String explicitRegion = cacheElement.attributeValue( "region" );
		final String region = explicitRegion != null ? explicitRegion : entityBinding.getEntity().getName();
		final String strategy = cacheElement.attributeValue( "usage" );
		final boolean cacheLazyProps = !"non-lazy".equals( cacheElement.attributeValue( "include" ) );
		entityBinding.setCaching( new Caching( region, strategy, cacheLazyProps ) );
	}

}
