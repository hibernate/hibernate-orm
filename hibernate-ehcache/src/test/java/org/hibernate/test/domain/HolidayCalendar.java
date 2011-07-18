/**
 *  Copyright 2003-2010 Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hibernate.test.domain;



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

