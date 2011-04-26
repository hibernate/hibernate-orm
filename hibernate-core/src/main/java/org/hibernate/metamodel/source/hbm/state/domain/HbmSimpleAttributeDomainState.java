/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.hbm.state.domain;

import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.binding.MappingDefaults;
import org.hibernate.metamodel.binding.SimpleAttributeBinding;
import org.hibernate.metamodel.domain.MetaAttribute;
import org.hibernate.metamodel.source.hbm.HbmHelper;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping.XMLClass.XMLDiscriminator;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping.XMLClass.XMLId;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping.XMLClass.XMLTimestamp;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping.XMLClass.XMLVersion;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLPropertyElement;
import org.hibernate.metamodel.source.util.MappingHelper;

/**
 * @author Gail Badner
 */
public class HbmSimpleAttributeDomainState extends AbstractHbmAttributeDomainState implements SimpleAttributeBinding.DomainState {
	private final HibernateTypeDescriptor hibernateTypeDescriptor = new HibernateTypeDescriptor();
	private final boolean isLazy;
	private final PropertyGeneration propertyGeneration;
	private final boolean isInsertable;
	private final boolean isUpdateable;

	public HbmSimpleAttributeDomainState(MappingDefaults defaults,
										 org.hibernate.metamodel.domain.Attribute attribute,
										 Map<String, MetaAttribute> entityMetaAttributes,
										 XMLId id) {
		super(
				defaults,
				attribute,
				id.getNode(),
				HbmHelper.extractMetas( id.getMeta(), entityMetaAttributes ),
				HbmHelper.getPropertyAccessorName( id.getAccess(), false, defaults.getDefaultAccess() ) ,
				true
		);

		this.isLazy = false;
		if ( id.getType() != null ) {
			this.hibernateTypeDescriptor.setTypeName( id.getType().getName() );
		}

		// TODO: how should these be set???
		this.propertyGeneration = PropertyGeneration.parse( null );
		this.isInsertable = true;

		this.isUpdateable = false;
	}

	public HbmSimpleAttributeDomainState(MappingDefaults defaults,
										 org.hibernate.metamodel.domain.Attribute attribute,
										 Map<String, MetaAttribute> entityMetaAttributes,
										 XMLDiscriminator discriminator) {
		super(
				defaults, attribute, null, null, null, true
		);
		this.hibernateTypeDescriptor.setTypeName( discriminator.getType() == null ? "string" : discriminator.getType() );
		this.isLazy = false;

		this.propertyGeneration = PropertyGeneration.NEVER;
		this.isInsertable = discriminator.isInsert();
		this.isUpdateable = false;
	}

	public HbmSimpleAttributeDomainState(MappingDefaults defaults,
										 org.hibernate.metamodel.domain.Attribute attribute,
										 Map<String, MetaAttribute> entityMetaAttributes,
										 XMLVersion version) {

		super(
				defaults,
				attribute,
				version.getNode(),
				HbmHelper.extractMetas( version.getMeta(), entityMetaAttributes ),
				HbmHelper.getPropertyAccessorName( version.getAccess(), false, defaults.getDefaultAccess() ) ,
				true
		);
		this.hibernateTypeDescriptor.setTypeName( version.getType() == null ? "integer" : version.getType() );
		this.isLazy = false;

		// for version properties marked as being generated, make sure they are "always"
		// generated; aka, "insert" is invalid; this is dis-allowed by the DTD,
		// but just to make sure.
		this.propertyGeneration = PropertyGeneration.parse(  version.getGenerated().value()  );
		if ( propertyGeneration == PropertyGeneration.INSERT ) {
			throw new MappingException( "'generated' attribute cannot be 'insert' for versioning property" );
		}
		this.isInsertable = MappingHelper.getBooleanValue( version.isInsert(), true );
		this.isUpdateable = true;
	}

	public HbmSimpleAttributeDomainState(MappingDefaults defaults,
										 org.hibernate.metamodel.domain.Attribute attribute,
										 Map<String, MetaAttribute> entityMetaAttributes,
										 XMLTimestamp timestamp) {

		super(
				defaults,
				attribute,
				timestamp.getNode(),
				HbmHelper.extractMetas( timestamp.getMeta(), entityMetaAttributes ),
				HbmHelper.getPropertyAccessorName( timestamp.getAccess(), false, defaults.getDefaultAccess() ),
				true
		);
		// Timestamp.getType() is not defined
		this.hibernateTypeDescriptor.setTypeName( "db".equals( timestamp.getSource() ) ? "dbtimestamp" : "timestamp" );
		this.isLazy = false;

		// for version properties marked as being generated, make sure they are "always"
		// generated; aka, "insert" is invalid; this is dis-allowed by the DTD,
		// but just to make sure.
		this.propertyGeneration = PropertyGeneration.parse(  timestamp.getGenerated().value()  );
		if ( propertyGeneration == PropertyGeneration.INSERT ) {
			throw new MappingException( "'generated' attribute cannot be 'insert' for versioning property" );
		}
		this.isInsertable = true; //TODO: is this right????
		this.isUpdateable = true;
	}

	public HbmSimpleAttributeDomainState(MappingDefaults defaults,
										 org.hibernate.metamodel.domain.Attribute attribute,
										 Map<String, MetaAttribute> entityMetaAttributes,
										 XMLPropertyElement property) {
		super(
				defaults,
				attribute,
				property.getNode(),
				HbmHelper.extractMetas( property.getMeta(), entityMetaAttributes ),
				HbmHelper.getPropertyAccessorName( property.getAccess(), false, defaults.getDefaultAccess() ),
				property.isOptimisticLock()
		);
		this.isLazy = property.isLazy();
		this.propertyGeneration = PropertyGeneration.parse( property.getGenerated() );

		if ( propertyGeneration == PropertyGeneration.ALWAYS || propertyGeneration == PropertyGeneration.INSERT ) {
			// generated properties can *never* be insertable.
			if (property.isInsert() != null && property.isInsert()) {
				// the user specifically supplied insert="true", which constitutes an illegal combo
				throw new MappingException(
						"cannot specify both insert=\"true\" and generated=\"" + propertyGeneration.getName() +
						"\" for property: " +
						getAttribute().getName()
				);
			}
			isInsertable = false;
		}
		else {
			isInsertable = MappingHelper.getBooleanValue( property.isInsert(), true );
		}
		if ( propertyGeneration == PropertyGeneration.ALWAYS ) {
			if (property.isUpdate() != null && property.isUpdate()) {
				// the user specifically supplied update="true",
				// which constitutes an illegal combo
				throw new MappingException(
						"cannot specify both update=\"true\" and generated=\"" + propertyGeneration.getName() +
						"\" for property: " +
						getAttribute().getName()
				);
			}
			isUpdateable = false;
		}
		else {
			isUpdateable = MappingHelper.getBooleanValue( property.isUpdate(), true );
		}
	}

	protected boolean isEmbedded() {
		return false;
	}

	public HibernateTypeDescriptor getHibernateTypeDescriptor() {
		return hibernateTypeDescriptor;
	}

	public boolean isLazy() {
		return isLazy;
	}

	public PropertyGeneration getPropertyGeneration() {
		return propertyGeneration;
	}
	public boolean isInsertable() {
		return isInsertable;
	}
	public boolean isUpdateable() {
		return isUpdateable;
	}

	public String getCascade() {
		return null;
	}

	public boolean isKeyCasadeDeleteEnabled() {
		//TODO: implement
		return false;
	}
	public String getUnsavedValue() {
		//TODO: implement
		return null;
	}
}
