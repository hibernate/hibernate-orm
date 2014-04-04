//$Id$
package org.hibernate.test.annotations.cid;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@SecondaryTable( name = "TV_PROGRAM_IDCLASS", pkJoinColumns =
		{
		@PrimaryKeyJoinColumn( name = "CHANNEL_ID" ),
		@PrimaryKeyJoinColumn( name = "PRESENTER_NAME" )
				} )
@IdClass( TvMagazinPk.class )
public class TvProgramIdClass {
	@Id
	@ManyToOne
	@JoinColumn(nullable=false)
	public Channel channel;
	@Id
	@ManyToOne
	@JoinColumn(nullable=false)
	public Presenter presenter;

	@Temporal( TemporalType.TIME )
    @Column(name="`time`")
	Date time;

	@Column( name = "TXT", table = "TV_PROGRAM_IDCLASS" )
	public String text;
}


