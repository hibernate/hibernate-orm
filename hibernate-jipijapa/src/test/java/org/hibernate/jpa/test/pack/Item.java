/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.pack;

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
						@QueryHint(name = QueryHints.TIMEOUT_JPA, value = "3000"),
						@QueryHint(name = QueryHints.CACHE_MODE, value = "ignore"),
						@QueryHint(name = QueryHints.CACHEABLE, value = "true"),
						@QueryHint(name = QueryHints.READ_ONLY, value = "true"),
						@QueryHint(name = QueryHints.COMMENT, value = "custom static comment"),
						@QueryHint(name = QueryHints.FETCH_SIZE, value = "512"),
						@QueryHint(name = QueryHints.FLUSH_MODE, value = "manual")
				}
		),
		@NamedQuery(name = "query-construct", query = "select new Item(i.name,i.descr) from Item i")
})
public class Item implements Serializable {

	private String name;
	private String descr;
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

	@OneToMany
	public Set<Distributor> getDistributors() {
		return distributors;
	}

	public void setDistributors(Set<Distributor> distributors) {
		this.distributors = distributors;
	}

	public void addDistributor(Distributor d) {
		if ( distributors == null ) {
			distributors = new HashSet();
		}
		distributors.add( d );
	}
}
