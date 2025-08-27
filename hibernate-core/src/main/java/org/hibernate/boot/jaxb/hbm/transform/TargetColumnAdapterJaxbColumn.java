/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.transform;

import org.hibernate.boot.jaxb.mapping.spi.JaxbCheckConstraintImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbColumnImpl;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public class TargetColumnAdapterJaxbColumn implements TargetColumnAdapter {
	private final JaxbColumnImpl jaxbColumn;

	public TargetColumnAdapterJaxbColumn(ColumnDefaults columnDefaults) {
		this( new JaxbColumnImpl(), columnDefaults );
	}

	public TargetColumnAdapterJaxbColumn(JaxbColumnImpl jaxbColumn, ColumnDefaults columnDefaults) {
		this.jaxbColumn = jaxbColumn;
		this.jaxbColumn.setLength( columnDefaults.getLength() );
		this.jaxbColumn.setScale( columnDefaults.getScale() );
		this.jaxbColumn.setPrecision( columnDefaults.getPrecision() );
		this.jaxbColumn.setNullable( columnDefaults.isNullable() );
		this.jaxbColumn.setUnique( columnDefaults.isUnique() );
		this.jaxbColumn.setInsertable( columnDefaults.isInsertable() );
		this.jaxbColumn.setUpdatable( columnDefaults.isUpdatable() );
	}

	public JaxbColumnImpl getTargetColumn() {
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
		if ( value != null ) {
			jaxbColumn.setLength( value );
		}
	}

	@Override
	public void setPrecision(Integer value) {
		if ( value != null ) {
			jaxbColumn.setPrecision( value );
		}
	}

	@Override
	public void setScale(Integer value) {
		if ( value != null ) {
			jaxbColumn.setScale( value );
		}
	}

	@Override
	public void setColumnDefinition(String value) {
		jaxbColumn.setColumnDefinition( value );
	}

	@Override
	public void setDefault(String value) {
		jaxbColumn.setDefault( value );
	}

	@Override
	public void setCheck(String value) {
		if ( StringHelper.isEmpty( value ) ) {
			return;
		}
		final JaxbCheckConstraintImpl checkConstraint = new JaxbCheckConstraintImpl();
		checkConstraint.setConstraint( value );
		jaxbColumn.getCheckConstraints().add( checkConstraint );
	}

	@Override
	public void setComment(String value) {
		jaxbColumn.setComment( value );
	}

	@Override
	public void setRead(String value) {
		jaxbColumn.setRead( value );
	}

	@Override
	public void setWrite(String value) {
		jaxbColumn.setWrite( value );
	}
}
