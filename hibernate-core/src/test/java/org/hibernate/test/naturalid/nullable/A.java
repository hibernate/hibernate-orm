/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.naturalid.nullable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.NaturalId;

/**
 * @author Guenther Demetz
 */
@Entity
public class A  {
	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	public long oid;

	@ManyToOne
	@NaturalId(mutable=true)
	public C assC;

	@Column
	@NaturalId(mutable=true)
	public String myname;

	@OneToMany(mappedBy="assA")
	public Set<B> assB = new HashSet<B>();
}
