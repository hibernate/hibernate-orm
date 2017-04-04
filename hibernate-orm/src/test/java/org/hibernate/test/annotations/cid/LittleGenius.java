/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.cid;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.PrimaryKeyJoinColumns;

/**
 * Hierarchy with cid + many to one
 * @author Anthony
 *
 */
@Entity
@PrimaryKeyJoinColumns({
@PrimaryKeyJoinColumn(name = "nthChild"),
@PrimaryKeyJoinColumn(name = "parentLastName"),
@PrimaryKeyJoinColumn(name = "parentFirstName")})
public class LittleGenius extends Child {
	public String particularSkill;
}
