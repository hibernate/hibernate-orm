package org.hibernate.jpa.test.procedure;

import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.FieldResult;
import javax.persistence.Id;
import javax.persistence.NamedStoredProcedureQueries;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.ParameterMode;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.StoredProcedureParameter;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
@Entity
@NamedStoredProcedureQueries(
		{
				@NamedStoredProcedureQuery(
						name = "s1",
						procedureName = "p1",
						parameters = {
								@StoredProcedureParameter(name = "p11",
										mode = ParameterMode.IN,
										type = Integer.class),
								@StoredProcedureParameter(name = "p12",
										mode = ParameterMode.IN,
										type = Integer.class
								)
						},
						resultClasses = { User.class }
				),
				@NamedStoredProcedureQuery(
						name = "s2",
						procedureName = "p2",
						parameters = {
								@StoredProcedureParameter(
										mode = ParameterMode.INOUT,
										type = String.class),
								@StoredProcedureParameter(
										mode = ParameterMode.INOUT,
										type = Long.class)
						},
						resultSetMappings = { "srms" }

				)
		}
)
@SqlResultSetMapping(name = "srms",
		entities = {
				@EntityResult(entityClass = User.class, fields = {
						@FieldResult(name = "id", column = "order_id"),
						@FieldResult(name = "name", column = "order_item")
				})
		}
)
public class User {
	@Id
	private int id;
	private String name;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
