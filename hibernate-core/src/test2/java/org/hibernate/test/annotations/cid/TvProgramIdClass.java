/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.cid;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
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
	@JoinColumn(nullable=false)
	public Channel channel;
	@Id
	@JoinColumn(nullable=false)
	public Presenter presenter;

	@Temporal( TemporalType.TIME )
    @Column(name="`time`")
	Date time;

	@Column( name = "TXT", table = "TV_PROGRAM_IDCLASS" )
	public String text;
}


