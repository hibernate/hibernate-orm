package org.hibernate.test.annotations.persister;
import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Persister;

/**
 * @author Shawn Clowater
 */
@Entity
@Persister(impl = org.hibernate.persister.entity.SingleTableEntityPersister.class)
public class Card implements Serializable {
	@Id
	public Integer id;

	@ManyToOne()
	@JoinColumn()
	public Deck deck;
}
