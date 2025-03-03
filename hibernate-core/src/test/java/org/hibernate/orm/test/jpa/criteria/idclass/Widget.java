/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.idclass;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * @author Erich Heard
 */
@Entity
@Table( name = "WIDGET" )
@IdClass( WidgetId.class )
public class Widget {
	@Id
	private String code;
	public String getCode( ) { return this.code; }
	public void setCode( String value ) { this.code = value; }

	@Id
	private String division;
	public String getDivision( ) { return this.division; }
	public void setDivision( String value ) { this.division = value; }

	@Column( name = "COST" )
	private Double cost;
	public Double getCost( ) { return this.cost; }
	public void setCost( Double value ) { this.cost = value; }

	@Override
	public String toString( ) {
		return "[Code:" + this.getCode( ) + "; Division: " + this.getDivision( ) + "; Cost: " + this.getCost( ) + "]";
	}
}
