package org.hibernate.test.annotations.query;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.FieldResult;
import javax.persistence.Id;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;

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
	@Column(name = "TABLE_NAME", nullable = false)
	private String tableName;

	@Formula(value = "(SYSDATE())")
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
