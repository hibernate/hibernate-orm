//$Id$
package org.hibernate.test.annotations.various;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Antenna {
	@Id public Integer id;
	@Generated(GenerationTime.ALWAYS) @Column()
	public String longitude;

	@Generated(GenerationTime.INSERT) @Column(insertable = false)
	public String latitude;

	@Generated(GenerationTime.NEVER)
	public Double power;
}
