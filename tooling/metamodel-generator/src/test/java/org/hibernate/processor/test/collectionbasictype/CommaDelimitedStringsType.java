/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.collectionbasictype;

import java.sql.Types;
import java.util.List;

import org.hibernate.usertype.UserTypeSupport;

/**
 * @author Vlad Mihalcea
 */
public class CommaDelimitedStringsType extends UserTypeSupport<List<String>> {
    public CommaDelimitedStringsType() {
        super( List.class, Types.VARCHAR );
    }
}