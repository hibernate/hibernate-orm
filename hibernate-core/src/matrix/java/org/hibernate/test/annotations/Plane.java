//$Id$
package org.hibernate.test.annotations;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;

/**
 * Plane class
 *
 * @author Emmanuel Bernard
 */
@Entity()
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "planetype", length = 100, discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("Plane")
@AttributeOverride(name = "altitude", column = @Column(name = "fld_altitude"))
@NamedQuery(name = "plane.byId", query = "from Plane where id = :id",
		hints = {@QueryHint(name = "org.hibernate.cacheable", value = "true"),
		@QueryHint(name = "org.hibernate.cacheRegion", value = "testedCacheRegion"),
		@QueryHint(name = "org.hibernate.timeout", value = "100"),
		@QueryHint(name = "org.hibernate.fetchSize", value = "1"),
		@QueryHint(name = "org.hibernate.flushMode", value = "Commit"),
		@QueryHint(name = "org.hibernate.cacheMode", value = "NORMAL"),
		@QueryHint(name = "org.hibernate.comment", value = "Plane by id")})
public class Plane extends FlyingObject {

	private Long id;
	private int nbrofSeats;

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public int getNbrOfSeats() {
		return nbrofSeats;
	}

	public void setId(Long long1) {
		id = long1;
	}

	public void setNbrOfSeats(int i) {
		nbrofSeats = i;
	}

}
