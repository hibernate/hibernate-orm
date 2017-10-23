/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.cid;
import java.util.Date;
import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Column;

/**
 * @author Emmanuel Bernard
 */
@Entity
@AssociationOverrides({
@AssociationOverride(name = "id.channel", joinColumns = @JoinColumn(name = "chan_id", nullable = false)),
@AssociationOverride(name = "id.presenter", joinColumns = @JoinColumn(name = "presenter_name", nullable = false))})
public class TvMagazin {
	@EmbeddedId
	public TvMagazinPk id;
	@Temporal(TemporalType.TIME)
    @Column(name="`time`")
	Date time;
}
