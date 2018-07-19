/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.ehcache.test.domain;



import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class HolidayCalendar {


	private Long id;
	// Date -> String
	private Map holidays = new HashMap();

	public HolidayCalendar init() {
		DateFormat df = new SimpleDateFormat("yyyy.MM.dd");
		try {
			holidays.clear();
			holidays.put(df.parse("2009.01.01"), "New Year's Day");
			holidays.put(df.parse("2009.02.14"), "Valentine's Day");
			holidays.put(df.parse("2009.11.11"), "Armistice Day");
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	public Map getHolidays() {
		return holidays;
	}

	protected void setHolidays(Map holidays) {
		this.holidays = holidays;
	}

	public void addHoliday(Date d, String name) {
		holidays.put(d, name);
	}

	public String getHoliday(Date d) {
		return (String)holidays.get(d);
	}

	public boolean isHoliday(Date d) {
		return holidays.containsKey(d);
	}

	protected Long getId() {
		return id;
	}

	protected void setId(Long id) {
		this.id = id;
	}
}

