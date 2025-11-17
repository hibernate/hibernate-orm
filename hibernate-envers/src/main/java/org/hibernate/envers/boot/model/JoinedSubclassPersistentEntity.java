/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.model;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmJoinedSubclassEntityType;
import org.hibernate.envers.configuration.internal.metadata.AuditTableData;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.mapping.PersistentClass;

/**
 * A persistent entity mapping that uses joined table(s) to store the hierarchy of entity types.
 *
 * @author Chris Cranford
 */
public class JoinedSubclassPersistentEntity extends SubclassPersistentEntity {

	private final List<Attribute> attributes;
	private String discriminatorValue;
	private Key key;

	public JoinedSubclassPersistentEntity(AuditTableData auditTableData, PersistentClass persistentClass) {
		super( auditTableData, persistentClass );
		this.attributes = new ArrayList<>();
	}

	@Override
	public void addAttribute(Attribute attribute) {
		attributes.add( attribute );
	}

	public void setKey(Key key) {
		this.key = key;
	}

	public String getDiscriminatorValue() {
		return discriminatorValue;
	}

	public void setDiscriminatorValue(String discriminatorValue) {
		this.discriminatorValue = discriminatorValue;
	}

//	@Override
//	public List<Join> getJoins() {
//		throw new UnsupportedOperationException();
//	}
//
//	@Override
//	public void addJoin(Join join) {
//		throw new UnsupportedOperationException();
//	}

	@Override
	public void build(JaxbHbmHibernateMapping mapping) {
		mapping.getJoinedSubclass().add( buildJaxbMapping() );
	}

	public JaxbHbmJoinedSubclassEntityType buildJaxbMapping() {
		final JaxbHbmJoinedSubclassEntityType entity = new JaxbHbmJoinedSubclassEntityType();
		entity.setExtends( getExtends() );

		// Set common stuff
		if ( getPersistentClass() != null ) {
			entity.setAbstract( getPersistentClass().isAbstract() );
		}

		if ( !StringTools.isEmpty(getAuditTableData().getAuditEntityName() ) ) {
			entity.setEntityName( getAuditTableData().getAuditEntityName() );
		}

		if ( !StringTools.isEmpty( getAuditTableData().getAuditTableName() ) ) {
			entity.setTable( getAuditTableData().getAuditTableName() );
		}

		if ( !StringTools.isEmpty( getAuditTableData().getSchema() ) ) {
			entity.setSchema( getAuditTableData().getSchema() );
		}

		if ( !StringTools.isEmpty( getAuditTableData().getCatalog() ) ) {
			entity.setCatalog( getAuditTableData().getCatalog() );
		}

		for ( Attribute attribute : attributes ) {
			entity.getAttributes().add( attribute.build() );
		}

		entity.setKey( key.build() );

		if ( !StringTools.isEmpty( discriminatorValue ) ) {
			entity.setDiscriminatorValue( discriminatorValue );
		}

		return entity;
	}
}
