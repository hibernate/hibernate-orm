//$Id$
package org.hibernate.test.annotations.cid;
import java.util.Date;
import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

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
