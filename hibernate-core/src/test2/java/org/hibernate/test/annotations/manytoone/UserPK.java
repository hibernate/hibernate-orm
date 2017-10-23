/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.manytoone;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.persistence.Column;

public class UserPK implements Serializable {
	private static final long serialVersionUID = -7720874756224520523L;
	@Column(name = "CTVUSERS_KEY")
	public Long userKey;

	@Column(name = "CTVUSERS_START_DATE")
	public Date startDate;


	@Column(name = "CTVUSERS_END_DATE")
	public Date endDate;

	public UserPK() {
	}

	@Override
	public boolean equals(Object obj) {
		if ( !( obj instanceof UserPK ) ) {
			return false;
		}
		UserPK userPK = (UserPK) obj;
		SimpleDateFormat formatter = new SimpleDateFormat( "MM/dd/yyyy" );
		return userKey.equals( userPK.userKey ) && formatter.format( startDate )
				.equals( formatter.format( userPK.startDate ) )
				&& formatter.format( endDate ).equals( formatter.format( userPK.endDate ) );
	}

	@Override
	public int hashCode() {
		return userKey.hashCode() * startDate.hashCode() * endDate.hashCode();
	}
}
