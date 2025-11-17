/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmDiscriminatorSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.envers.configuration.internal.metadata.AuditTableData;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.mapping.PersistentClass;

/**
 * A persistent entity mapping that uses a single table to store entities based on a discriminator.
 *
 * @author Chris Cranford
 */
public class DiscriminatorPersistentEntity extends SubclassPersistentEntity implements JoinAwarePersistentEntity {

	private final List<Join> joins;
	private final List<Attribute> attributes;
	private String discriminatorValue;

	public DiscriminatorPersistentEntity(AuditTableData auditTableData, PersistentClass persistentClass) {
		super( auditTableData, persistentClass );
		this.attributes = new ArrayList<>();
		this.joins = new ArrayList<>();
	}

	@Override
	public void addAttribute(Attribute attribute) {
		attributes.add( attribute );
	}

	@Override
	public boolean isJoinAware() {
		return true;
	}

	public String getDiscriminatorValue() {
		return discriminatorValue;
	}

	public void setDiscriminatorValue(String discriminatorValue) {
		this.discriminatorValue = discriminatorValue;
	}

	@Override
	public List<Join> getJoins() {
		return Collections.unmodifiableList( joins );
	}

	@Override
	public void addJoin(Join join) {
		this.joins.add( join );
	}

	@Override
	public void build(JaxbHbmHibernateMapping mapping) {
		mapping.getSubclass().add( buildJaxbMapping() );
	}

	public JaxbHbmDiscriminatorSubclassEntityType buildJaxbMapping() {
		final JaxbHbmDiscriminatorSubclassEntityType entity = new JaxbHbmDiscriminatorSubclassEntityType();
		entity.setExtends( getExtends() );

		// Set common stuff
		if ( getPersistentClass() != null ) {
			entity.setAbstract( getPersistentClass().isAbstract() );
		}

		if ( !StringTools.isEmpty( getAuditTableData().getAuditEntityName() ) ) {
			entity.setEntityName( getAuditTableData().getAuditEntityName() );
		}

		// Initialize attributes
		for ( Attribute attribute : attributes ) {
			entity.getAttributes().add( attribute.build() );
		}

		if ( !StringTools.isEmpty( discriminatorValue ) ) {
			entity.setDiscriminatorValue( discriminatorValue );
		}

		for ( Join join : joins ) {
			entity.getJoin().add( join.build() );
		}

		return entity;
	}
}
