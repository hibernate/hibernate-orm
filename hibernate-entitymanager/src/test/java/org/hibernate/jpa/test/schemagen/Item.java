//$Id$
package org.hibernate.jpa.test.schemagen;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.FieldResult;
import javax.persistence.Id;
import javax.persistence.LockModeType;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.QueryHint;
import javax.persistence.SqlResultSetMapping;

import org.hibernate.annotations.QueryHints;

/**
 * @author Gavin King
 */
@Entity(name = "Item")
public class Item implements Serializable {

	private String name;
	private String descr;

	public Item() {
	}

	public Item(String name, String desc) {
		this.name = name;
		this.descr = desc;
	}

	@Column(length = 200)
	public String getDescr() {
		return descr;
	}

	public void setDescr(String desc) {
		this.descr = desc;
	}

	@Id
	@Column(length = 30)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
