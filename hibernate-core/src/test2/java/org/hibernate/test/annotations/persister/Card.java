/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
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
