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
