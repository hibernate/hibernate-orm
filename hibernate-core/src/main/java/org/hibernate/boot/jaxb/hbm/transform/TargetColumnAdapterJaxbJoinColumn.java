/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.transform;

import org.hibernate.boot.jaxb.mapping.spi.JaxbJoinColumnImpl;

/**
 * @author Steve Ebersole
 */
public class TargetColumnAdapterJaxbJoinColumn implements TargetColumnAdapter {
	private final JaxbJoinColumnImpl jaxbColumn;

	public TargetColumnAdapterJaxbJoinColumn(ColumnDefaults columnDefaults) {
		this( new JaxbJoinColumnImpl(), columnDefaults );
	}

	public TargetColumnAdapterJaxbJoinColumn(JaxbJoinColumnImpl jaxbColumn, ColumnDefaults columnDefaults) {
		this.jaxbColumn = jaxbColumn;
		this.jaxbColumn.setNullable( columnDefaults.isNullable() );
		this.jaxbColumn.setUnique( columnDefaults.isUnique() );
		this.jaxbColumn.setInsertable( columnDefaults.isInsertable() );
		this.jaxbColumn.setUpdatable( columnDefaults.isUpdatable() );
	}

	public JaxbJoinColumnImpl getTargetColumn() {
		return jaxbColumn;
	}

	@Override
	public void setName(String value) {
		jaxbColumn.setName( value );
	}

	@Override
	public void setTable(String value) {
		jaxbColumn.setTable( value );
	}

	@Override
	public void setNullable(Boolean value) {
		if ( value != null ) {
			jaxbColumn.setNullable( value );
		}
	}

	@Override
	public void setUnique(Boolean value) {
		if ( value != null ) {
			jaxbColumn.setUnique( value );
		}
	}

	@Override
	public void setInsertable(Boolean value) {
		if ( value != null ) {
			jaxbColumn.setInsertable( value );
		}
	}

	@Override
	public void setUpdatable(Boolean value) {
		if ( value != null ) {
			jaxbColumn.setUpdatable( value );
		}
	}

	@Override
	public void setLength(Integer value) {
	}

	@Override
	public void setPrecision(Integer value) {
	}

	@Override
	public void setScale(Integer value) {
	}

	@Override
	public void setColumnDefinition(String value) {
		jaxbColumn.setColumnDefinition( value );
	}

	@Override
	public void setDefault(String value) {
	}

	@Override
	public void setCheck(String value) {
	}

	@Override
	public void setComment(String value) {
	}

	@Override
	public void setRead(String value) {
	}

	@Override
	public void setWrite(String value) {
	}
}
