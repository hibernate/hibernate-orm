/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.uniqueconstraint;

import org.hibernate.JDBCException;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.SybaseDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;

import org.junit.jupiter.api.Test;

import jakarta.persistence.PersistenceException;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.fail;

/**
 * @author Manuel Bernhardt
 * @author Brett Meyer
 */
@DomainModel( annotatedClasses = { Room.class, Building.class, House.class } )
@SessionFactory
@SkipForDialect( dialectClass = InformixDialect.class,
		matchSubTypes = true,
		reason = "Informix does not properly support unique constraints on nullable columns" )
@SkipForDialect( dialectClass = SybaseDialect.class,
        matchSubTypes = true,
        reason = "Sybase does not properly support unique constraints on nullable columns" )
public class UniqueConstraintTest {

	@Test
	public void testUniquenessConstraintWithSuperclassProperty(SessionFactoryScope scope) {
        scope.inTransaction( (s) -> {
            Room livingRoom = new Room();
            livingRoom.setId(1l);
            livingRoom.setName("livingRoom");
            s.persist(livingRoom);
            s.flush();
            House house = new House();
            house.setId(1l);
            house.setCost(100);
            house.setHeight(1000l);
            house.setRoom(livingRoom);
            s.persist(house);
            s.flush();
            House house2 = new House();
            house2.setId(2l);
            house2.setCost(100);
            house2.setHeight(1001l);
            house2.setRoom(livingRoom);
            s.persist(house2);
            try {
                s.flush();
                fail( "Database constraint non-existent" );
            }
            catch (PersistenceException e) {
                assertTyping( JDBCException.class, e );
                //success
            }
            finally {
                s.getTransaction().markRollbackOnly();
            }
        } );
    }
    
}
