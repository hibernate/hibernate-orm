/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.orm.test.jpa;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.QueryHint;
import jakarta.persistence.SqlResultSetMapping;

import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHE_MODE;
import static org.hibernate.jpa.HibernateHints.HINT_COMMENT;
import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;
import static org.hibernate.jpa.HibernateHints.HINT_FLUSH_MODE;
import static org.hibernate.jpa.HibernateHints.HINT_READ_ONLY;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_QUERY_TIMEOUT;

/**
 * @author Gavin King
 */
@Entity(name = "Item")
@SqlResultSetMapping(name = "getItem", entities =
	@EntityResult(entityClass = Item.class, fields = {
		@FieldResult(name = "name", column = "itemname"),
		@FieldResult(name = "descr", column = "itemdescription")
	})
)
@NamedNativeQueries({
	@NamedNativeQuery(
		name = "nativeItem1",
		query = "select name as itemname, descr as itemdescription from Item",
		resultSetMapping = "getItem"
	),
	@NamedNativeQuery(
		name = "nativeItem2",
		query = "select * from Item",
		resultClass = Item.class
	)
})
@NamedQueries({
		@NamedQuery(
				name = "itemJpaQueryWithLockModeAndHints",
				query = "select i from Item i",
				lockMode = LockModeType.PESSIMISTIC_WRITE,
				hints = {
						@QueryHint( name = HINT_SPEC_QUERY_TIMEOUT, value = "3000" ),
						@QueryHint( name = HINT_CACHE_MODE, value = "ignore" ),
						@QueryHint( name = HINT_CACHEABLE, value = "true" ),
						@QueryHint( name = HINT_READ_ONLY, value = "true" ),
						@QueryHint( name = HINT_COMMENT, value = "custom static comment" ),
						@QueryHint( name = HINT_FETCH_SIZE, value = "512" ),
						@QueryHint( name = HINT_FLUSH_MODE, value = "manual" )
				}
		),
		@NamedQuery(name = "query-construct", query = "select new Item(i.name,i.descr) from Item i")
})
public class Item implements Serializable {

	private String name;
	private String descr;
	private Integer intVal;
	private Set<Distributor> distributors = new HashSet<Distributor>();

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

	@Column(name = "int_val")
	public Integer getIntVal() {
		return intVal;
	}
	public void setIntVal(Integer intVal) {
		this.intVal = intVal;
	}

	@OneToMany
	public Set<Distributor> getDistributors() {
		return distributors;
	}

	public void setDistributors(Set<Distributor> distributors) {
		this.distributors = distributors;
	}

	public void addDistributor(Distributor d) {
		if ( distributors == null ) distributors = new HashSet();
		distributors.add( d );
	}
}
