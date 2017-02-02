/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
import groovy.sql.Sql
import groovy.xml.MarkupBuilder
import org.geolatte.geom.ByteBuffer

@GrabConfig(systemClassLoader=true)
@Grab(group='com.microsoft', module='sqljdbc', version='2.0')
@Grab(group='org.geolatte', module='geolatte-geom',version='0.12-SNAPSHOT')

//path to the generated TestData XML file
def OUT_FILE = "/tmp/out.xml"

//input test data set
String testdata = new File('test-data-set.xml').text

def records = new XmlSlurper().parseText(testdata)
def wkts = records.Element.wkt

def wkt_srids= wkts.collect{ wkt -> wkt.text() =~ /SRID=(.*);(.*)/ }
    .findAll { it.matches() }
    .collect {m -> [ m[0][1], m[0][2] ] } //select only matching srid and wkt regions

//add the empty elements

wkt_srids.add( [0, 'POINT EMPTY'])
wkt_srids.add( [0, 'LINESTRING EMPTY'])
wkt_srids.add( [0, 'GEOMETRYCOLLECTION EMPTY'])
wkt_srids.add( [0, 'POLYGON EMPTY'])
wkt_srids.add( [0, 'MULTIPOINT EMPTY'])

//wkt_srids.each{ println( it ) }

sql = Sql.newInstance('jdbc:sqlserver://sqlserver.geovise.com:1433;databaseName=HBS',
        'hbs', 'hbs', 'com.microsoft.sqlserver.jdbc.SQLServerDriver')

def writer = new FileWriter( new File(OUT_FILE) )
def xmlOut = new MarkupBuilder(writer)
 xmlOut.TestCases() {
    def i = 1
    wkt_srids.each { el ->
        TestCase {
            id( i++ )
            ewkt( el[1] )
            srid( el[0])
            db_representation(to_native(el[1], el[0]))
//            db_wkt(to_wkt(el[1], el[0]))
//            db_wkb(to_wkb(el[1], el[0]))
        }
    }

}

def to_native(wkt, srid) {
    row = sql.firstRow("select Geometry::STGeomFromText('" + wkt + "', ${srid}) ")
    buffer = ByteBuffer.from(row[0])
    buffer.toString()
}

def to_wkt(wkt, srid) {
    row = sql.firstRow("select Geometry::STGeomFromText('" + wkt + "', ${srid}).STAsText() ")
    row[0]
}


def to_wkb(wkt, srid) {
    row = sql.firstRow("select Geometry::STGeomFromText('" + wkt + "', ${srid}).STAsBinary() ")
    buffer = ByteBuffer.from(row[0])
    buffer.toString()
}













