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
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * @author Chandra Patni
 */
@Entity
@SecondaryTable( name = "TV_PROGRAM_EXT", pkJoinColumns = {
@PrimaryKeyJoinColumn( name = "CHANNEL_ID" ),
@PrimaryKeyJoinColumn( name = "PRESENTER_NAME" )
		} )
@AssociationOverrides({
@AssociationOverride(name = "id.channel", joinColumns = @JoinColumn(name = "chan_id", nullable = false)),
@AssociationOverride(name = "id.presenter", joinColumns = @JoinColumn(name = "presenter_name", nullable = false))})
public class TvProgram {
	@EmbeddedId
	public TvMagazinPk id;

	@Temporal( TemporalType.TIME )
    @Column(name="`time`")
	Date time;

	@Column( name = "TXT", table = "TV_PROGRAM_EXT" )
	public String text;

}
