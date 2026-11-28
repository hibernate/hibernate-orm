package org.hibernate.orm.test.any.keydefault.modulefixtures.string;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import org.hibernate.annotations.Any;

/**
 * @author Vincent Bouthinon
 */
@Entity
public class StringOwner {
	@Id
	public Integer id;
	@Any
	@JoinColumn(name = "content_id")
	@Column(name = "content_type")
	public Object content;
}
