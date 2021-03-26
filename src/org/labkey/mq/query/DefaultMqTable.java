package org.labkey.mq.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerForeignKey;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.UserIdQueryForeignKey;
import org.labkey.mq.MqSchema;

class DefaultMqTable extends FilteredTable<MqSchema>
{
    DefaultMqTable(@NotNull TableInfo table, @NotNull MqSchema schema, ContainerFilter cf)
    {
        super(table, schema, cf);
        wrapAllColumns(true);
    }
}
