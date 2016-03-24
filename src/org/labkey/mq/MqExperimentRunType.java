package org.labkey.mq;

import org.labkey.api.exp.ExperimentRunType;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.api.ExpProtocol;

/**
 * Created by vsharma on 2/11/2016.
 */
public class MqExperimentRunType extends ExperimentRunType
{
    private String[] _protocolPrefixes;

    public MqExperimentRunType()
    {
        super("Imported MaxQuant Results", MqSchema.NAME, MqSchema.TABLE_EXPERIMENT_GROUP);
        _protocolPrefixes = new String[] {MqModule.IMPORT_MQ_PROTOCOL_OBJECT_PREFIX};
    }

    @Override
    public Priority getPriority(ExpProtocol protocol)
    {
        Lsid lsid = new Lsid(protocol.getLSID());
        String objectId = lsid.getObjectId();

        for (String protocolPrefix : _protocolPrefixes)
        {
            if (objectId.startsWith(protocolPrefix))
            {
                return Priority.HIGH;
            }
        }

        return null;
    }
}
