package org.hibernate.orm.test.annotations.namingstrategy;

import org.hibernate.annotations.Temporal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "TemporalEntity")
@Table
@Temporal(rowStart = "valid_from", rowEnd = "valid_to")
public class TemporalEntity {
	@Id
	private Integer id;
}
