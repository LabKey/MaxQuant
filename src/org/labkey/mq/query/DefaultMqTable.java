package org.labkey.mq.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ContainerForeignKey;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.UserIdQueryForeignKey;
import org.labkey.mq.MqSchema;

public class DefaultMqTable extends FilteredTable<MqSchema>
{
    public DefaultMqTable(@NotNull TableInfo table, @NotNull MqSchema schema)
    {
        super(table, schema);
        wrapAllColumns(true);

        ContainerForeignKey.initColumn(getColumn("Container"), schema);
        UserIdQueryForeignKey.initColumn(schema.getUser(), getContainer(), getColumn("CreatedBy"), true);
        UserIdQueryForeignKey.initColumn(schema.getUser(), getContainer(), getColumn("ModifiedBy"), true);
    }
}
