/**
 * 
 */
package org.hibernate.test.annotations.persister;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;

/**
 * @author Laabidi RAISSI
 *
 */
@Entity
@org.hibernate.annotations.Entity( persister = "org.hibernate.persister.entity.SingleTableEntityPersister" )
@SQLInsert(sql = "INSERT INTO {h-schema}FOO", callable = true)
@SQLDelete(sql = "DELETE FROM {h-schema}FOO", check = ResultCheckStyle.COUNT)
@SQLUpdate(sql = "UPDATE {h-schema}FOO", check = ResultCheckStyle.PARAM)
public class CardWithCustomSQL implements Serializable {
	@Id
	public Integer id;

	@ManyToOne()
	@JoinColumn()
	public Deck deck;
}
