/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.cid;
import java.util.Date;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Column;

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
