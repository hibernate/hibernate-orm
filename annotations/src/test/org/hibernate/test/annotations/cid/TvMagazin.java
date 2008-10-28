//$Id$
package org.hibernate.test.annotations.cid;

import java.util.Date;
import javax.persistence.AssociationOverride;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * @author Emmanuel Bernard
 */
@Entity
@AssociationOverride(name = "id.channel", joinColumns = @JoinColumn(name = "chan_id"))
public class TvMagazin {
	@EmbeddedId
	public TvMagazinPk id;
	@Temporal(TemporalType.TIME)
	Date time;
}
