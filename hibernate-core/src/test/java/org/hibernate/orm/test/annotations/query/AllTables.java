/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.query;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.Id;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;

import org.hibernate.annotations.Formula;

@Entity
@Table(name = "ALL_TABLES")
@SqlResultSetMapping(name = "all",
		entities = @EntityResult(entityClass = AllTables.class,
				fields = {
						@FieldResult(name = "tableName", column = "t_name"),
						@FieldResult(name = "daysOld", column = "t_time")
				}))
public class AllTables {

	@Id
	@Column(name = "table_name", nullable = false)
	private String tableName;

	@Formula(value = "(SYSDATE())")
	@Column(name = "days_old")
	private String daysOld;

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getDaysOld() {
		return daysOld;
	}

	public void setDaysOld(String daysOld) {
		this.daysOld = daysOld;
	}
}
