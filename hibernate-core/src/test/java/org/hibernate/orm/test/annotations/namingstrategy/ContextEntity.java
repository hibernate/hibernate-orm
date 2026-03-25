package org.hibernate.orm.test.annotations.namingstrategy;

import java.util.List;

import org.hibernate.annotations.Audited;
import org.hibernate.annotations.SoftDelete;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity(name = "ContextEntity")
@Table
@SoftDelete(columnName = "gone")
public class ContextEntity {
	@Id
	private Integer id;

	private String basicValue;

	@Embedded
	private ContextAddress address;

	@ManyToOne
	@JoinColumn
	private ContextParent parent;

	@ElementCollection
	@Audited
	@jakarta.persistence.CollectionTable(name = "context_tags", joinColumns = @JoinColumn(name = "owner_fk"))
	@jakarta.persistence.Column(name = "tag_value")
	private List<String> tags;
}
