package org.hibernate.test.annotations.xml.ejb3;

import java.util.HashMap;
import java.util.Map;

public class Company {
	int id;
	Map organization = new HashMap();
	Map conferenceRoomExtensions = new HashMap();
}
