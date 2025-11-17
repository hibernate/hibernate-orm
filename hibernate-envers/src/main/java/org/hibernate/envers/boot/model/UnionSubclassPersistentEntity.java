/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.model;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmUnionSubclassEntityType;
import org.hibernate.envers.configuration.internal.metadata.AuditTableData;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.mapping.PersistentClass;

/**
 * A persistent entity mapping that uses the table-per-class inheritance strategy.
 *
 * @author Chris Cranford
 */
public class UnionSubclassPersistentEntity extends SubclassPersistentEntity {

	private final List<Attribute> attributes;

	public UnionSubclassPersistentEntity(AuditTableData auditTableData, PersistentClass persistentClass) {
		super( auditTableData, persistentClass );
		this.attributes = new ArrayList<>();
	}

	@Override
	public void addAttribute(Attribute attribute) {
		attributes.add( attribute );
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
		mapping.getUnionSubclass().add( buildJaxbMapping() );
	}

	public JaxbHbmUnionSubclassEntityType buildJaxbMapping() {
		final JaxbHbmUnionSubclassEntityType entity = new JaxbHbmUnionSubclassEntityType();
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

		// Initialize attributes
		for ( Attribute attribute : attributes ) {
			entity.getAttributes().add( attribute.build() );
		}

		return entity;
	}
}
