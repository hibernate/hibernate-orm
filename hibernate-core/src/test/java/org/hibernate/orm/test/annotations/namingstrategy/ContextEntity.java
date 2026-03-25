/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.namingstrategy;

import java.util.List;

import org.hibernate.annotations.Audited;
import org.hibernate.annotations.SoftDelete;

import jakarta.persistence.ElementCollection;
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

	@ManyToOne
	@JoinColumn
	private ContextParent parent;

	@ElementCollection
	@Audited(transactionId = "tags_txn", modificationType = "tags_mod")
	@jakarta.persistence.CollectionTable(name = "context_tags", joinColumns = @JoinColumn(name = "owner_fk"))
	@jakarta.persistence.Column(name = "tag_value")
	private List<String> tags;
}
