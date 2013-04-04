package org.hibernate.ejb.criteria.jpaMapMode;

import java.sql.Types;
import java.text.MessageFormat;
import java.util.*;

public class OracleSqlGenerator extends GenericSqlGenerator implements SqlGenerator
{
    private static final Map<Integer, String> sqlTypeToDeclaredType;
    private static final Map<ColumnType, Integer> columnTypeToSqlType;

    static
    {
        Map<Integer, String> map = new HashMap<Integer, String>();
        map.put(Types.NUMERIC, "number(19)");
        map.put(Types.VARCHAR, "varchar(4000)");
        map.put(Types.BINARY, "raw(16)");

        sqlTypeToDeclaredType = Collections.unmodifiableMap(map);
    }

    static
    {
        Map<ColumnType, Integer> map = new HashMap<ColumnType, Integer>();
        map.put(ColumnType.NUMERIC, Types.NUMERIC);
        map.put(ColumnType.TEXT, Types.VARCHAR);
        map.put(ColumnType.ID, Types.BINARY);

        columnTypeToSqlType = Collections.unmodifiableMap(map);
    }

    @Override
    protected Map<Integer, String> getSqlTypeToDeclaredType()
    {
        return sqlTypeToDeclaredType;
    }

    @Override
    public Map<ColumnType, Integer> getColumnTypeToSqlType()
    {
        return columnTypeToSqlType;
    }

    @Override
    public List<String> createBlobTable()
    {
        List<String> commands = new ArrayList<String>(2);
        commands.add(MessageFormat.format(getDropTableTemplate("table"), "blobs"));
        commands.add(MessageFormat.format(super.createBlobTable().get(0), "blobs"));

        return commands;
    }

    @Override
    protected void instantiateView(MetaModelMapping metaModelMapping,
        DocumentTableMapping documentTableMapping, List<CharSequence> commands)
    {
        commands.add(MessageFormat
            .format(getDropTableTemplate("view"), documentTableMapping.getDocument().getName()));
        super.instantiateView(metaModelMapping, documentTableMapping, commands);
    }

    @Override
    protected String getCreateTableTemplate()
    {
        return " create table {0} ( {1} raw(16) primary key";
    }

    @Override
    protected void instantiateTable(Table table, List<CharSequence> commands)
    {

        String createTableTemplate = getCreateTableTemplate();
        String tableName = table.getTableName();

        commands.add(MessageFormat.format(getDropTableTemplate("table"), tableName));

        StringBuilder builder = new StringBuilder();
        String createTableStatement = MessageFormat
            .format(createTableTemplate, tableName, table.getIdColumnName());
        builder.append(createTableStatement);

        generateColumns(builder, table);
        builder.append(")");

        commands.add(builder.toString());
    }

    @Override
    public Column createNumericColumn(int i)
    {
        return new Column(createColumnName(ColumnType.NUMERIC, i), Types.NUMERIC);
    }

    protected String getDropTableTemplate(String type)
    {
        // note that MessageFormat ignores markers inside single quotes, so
        // they're doubled (e.g., ''DROP TABLE FOO'')
        // also note that Oracle cannot handle CRLF, so we send only LF
        return "BEGIN\n EXECUTE IMMEDIATE ''DROP " + type + " {0}'';\n" +
            "EXCEPTION\n WHEN OTHERS THEN\n " + "IF SQLCODE != -942 THEN\n RAISE;\n END IF;\n END;";
    }

}
