/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package hudson.util

/**
 *
 * @author mvecera
 */
class DBAllocation {
    private final static String DB_ALLOCATOR_URL = "http://dballocator.mw.lab.eng.bos.redhat.com:8080/Allocator/AllocatorServlet";
    private dbinstallPath = "dbinstall"
    private retries = 30
    private UUID = ""
    private ant
    private File dbConfigFile;
    private String requestee;

    def DBAllocation(dbinstallPath) {
        this(new AntBuilder(), dbinstallPath)
    }

    def DBAllocation(ant, dbinstallPath, outPropsFileName = "allocated.db.properties") {
        this.ant = ant;
        this.dbinstallPath = dbinstallPath;
        this.dbConfigFile = new File(dbinstallPath, outPropsFileName);
        if(System.properties.containsKey("hibernate-matrix-dballocation-requestee"))
        requestee = System.properties["hibernate-matrix-dballocation-requestee"]
        else
        requestee = "hibernate"
    }

    def getProperties() {
        def props = new Properties();
        props.load(new FileInputStream(dbConfigFile));
        return props;
    }

    def allocate(label, expiry) {
        if ( dbConfigFile.exists() ) {
            dbConfigFile.delete()
        }
        def i = 0
        while ( !(dbConfigFile.exists() && dbConfigFile.length() > 0) ) {
            if ( i >= retries ) {
                throw new Exception('Database unavailable')
            }
            if ( i > 0 ) {
                println "Waiting before trying to allocate DB again."
                Thread.sleep(60000)
            }
            println "Allocating DB..."
            def allocatorUrl = DB_ALLOCATOR_URL + "?operation=alloc&label=$label&requestee=${requestee}&expiry=$expiry"
            ant.get(src: allocatorUrl, dest: dbConfigFile.absolutePath, ignoreerrors: 'true')
            i++
        }
        def dbProps = getProperties();
        this.UUID = dbProps['uuid']
        return this.UUID
    }

    def release() {
        release(this.UUID)
    }

    def release(UUID) {
        println 'De-allocating DB...'
        def allocatorUrl = DB_ALLOCATOR_URL + "?operation=dealloc&uuid=$UUID"
        ant.get(src: allocatorUrl, dest: "/tmp/.tmpfile")
    }

    def clean() {
        clean(this.UUID);
    }

    def clean(UUID) {
        println 'Cleaning DB...'
        def allocatorUrl = DB_ALLOCATOR_URL + "?operation=erase&uuid=$UUID"
        ant.get(src: allocatorUrl, dest: "/tmp/.tmpfile")
    }

    def reallocate(newExpiry) {
        reallocate(this.UUID, newExpiry)
    }

    def reallocate(UUID, newExpiry) {
        println 'Re-allocating DB...'
        def allocatorUrl = DB_ALLOCATOR_URL + "?operation=realloc&uuid=$UUID&expiry=$newExpiry"
        ant.get(src: allocatorUrl, dest: "/tmp/.tmpfile")
    }
}

