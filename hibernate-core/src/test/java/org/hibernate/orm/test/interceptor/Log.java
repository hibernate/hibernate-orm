package org.hibernate.orm.test.interceptor;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.id.IncrementGenerator;
import java.util.Calendar;

@Entity
@Table(name = "log_entries")
public class Log {
	private Long id;
	private String entityName;
	private String entityId;
	private String action;
	private Calendar time;

	public Log(String action, String id, String name) {
		super();
		this.action = action;
		entityId = id;
		entityName = name;
		time = Calendar.getInstance();
	}
	public Log() {
		super();
	}
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	@Column(name = "entity_id")
	public String getEntityId() {
		return entityId;
	}
	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}
	@Column(name = "entity_name")
	public String getEntityName() {
		return entityName;
	}
	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}
	@Id
	@GeneratedValue
	@GenericGenerator(type = IncrementGenerator.class)
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	@Column(name = "action_time")
	public Calendar getTime() {
		return time;
	}
	public void setTime(Calendar time) {
		this.time = time;
	}
}
