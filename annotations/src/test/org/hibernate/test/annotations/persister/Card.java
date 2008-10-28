package org.hibernate.test.annotations.persister;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * @author Shawn Clowater
 */
@Entity
@org.hibernate.annotations.Entity( persister = "org.hibernate.persister.entity.SingleTableEntityPersister" )
public class Card implements Serializable {
	@Id
	public Integer id;

	@ManyToOne()
	@JoinColumn()
	public Deck deck;
}