/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.query.results;

/**
 * @author Steve Ebersole
 */
public class Queries {
	public static final String ENTITY = "select e from SimpleEntity e";
	public static final String ENTITY_NO_SELECT = "from SimpleEntity e";

	public static final String COMPOSITE = "select e.composite from SimpleEntity e";
	public static final String NAME = "select e.name from SimpleEntity e";
	public static final String COMP_VAL = "select e.composite.value1 from SimpleEntity e";

	public static final String ID_NAME = "select e.id, e.name from SimpleEntity e";
	public static final String ID_COMP_VAL = "select e.id, e.composite.value1 from SimpleEntity e";

	public static final String ID_NAME_DTO = "select new Dto(e.id, e.name) from SimpleEntity e";
	public static final String ID_COMP_VAL_DTO = "select new Dto(e.id, e.composite.value1) from SimpleEntity e";

	public static final String NAME_DTO = "select new Dto2(e.name) from SimpleEntity e";
	public static final String COMP_VAL_DTO = "select new Dto2(e.composite.value1) from SimpleEntity e";

	public static final String NAMED_ENTITY = "entity";
	public static final String NAMED_ENTITY_NO_SELECT = "entity-no-select";
	public static final String NAMED_COMPOSITE = "composite";
	public static final String NAMED_NAME = "name";
	public static final String NAMED_COMP_VAL = "comp-val";
	public static final String NAMED_ID_NAME = "id-name";
	public static final String NAMED_ID_COMP_VAL = "id-comp-val";
	public static final String NAMED_ID_NAME_DTO = "id-name-dto";
	public static final String NAMED_ID_COMP_VAL_DTO = "id-comp-val-dto";
	public static final String NAMED_NAME_DTO = "name-dto";
	public static final String NAMED_COMP_VAL_DTO = "comp-val-dto";
}
