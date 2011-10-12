package org.hibernate.test.cfg.persister;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
@Entity
public class Tree {
    @Id
	public Long getId() { return id; }
	public void setId(Long id) {  this.id = id; }
	private Long id;
}
