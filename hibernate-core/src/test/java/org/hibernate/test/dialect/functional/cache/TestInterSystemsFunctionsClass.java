/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dialect.functional.cache;
import java.util.Date;

/**
 * Entity for testing function support of InterSystems' CacheSQL...
 *
 * @author Jonathan Levinson
 */
public class TestInterSystemsFunctionsClass {
	private Long id;
    private java.util.Date date3;
    private java.util.Date date1;
    private java.util.Date date;
    private String dateText;

	public TestInterSystemsFunctionsClass() {
	}

	public TestInterSystemsFunctionsClass(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }


    public String getDateText() {
        return dateText;
    }

    public void setDateText(String dateText) {
        this.dateText = dateText;
    }


    public Date getDate1() {
        return date1;
    }

    public void setDate1(Date date1) {
        this.date1 = date1;
    }


    public Date getDate3() {
        return date3;
    }

    public void setDate3(Date date3) {
        this.date3 = date3;
    }

}
