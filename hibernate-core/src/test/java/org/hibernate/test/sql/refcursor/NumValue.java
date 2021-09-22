/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.sql.refcursor;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.QueryHint;
import jakarta.persistence.Table;

@Entity
@Table(name = "BOT_NUMVALUE")
@NamedNativeQueries({
		@NamedNativeQuery(name = "NumValue.getSomeValues",
				query = "{ ? = call f_test_return_cursor() }",
				resultClass = NumValue.class, hints = { @QueryHint(name = "org.hibernate.callable", value = "true") })
})
public class NumValue implements Serializable {
	@Id
	@Column(name = "BOT_NUM", nullable = false)
	private long num;

	@Column(name = "BOT_VALUE")
	private String value;

	public NumValue() {
	}

	public NumValue(long num, String value) {
		this.num = num;
		this.value = value;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof NumValue ) ) return false;

		NumValue numValue = (NumValue) o;

		if ( num != numValue.num ) return false;
		if ( value != null ? !value.equals( numValue.value ) : numValue.value != null ) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = (int) ( num ^ ( num >>> 32 ) );
		result = 31 * result + ( value != null ? value.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		return "NumValue(num = " + num + ", value = " + value + ")";
	}

	public long getNum() {
		return num;
	}

	public void setNum(long num) {
		this.num = num;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
