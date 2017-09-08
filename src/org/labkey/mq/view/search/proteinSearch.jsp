<%@ page import="org.labkey.api.util.UniqueID" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependency" %>
<%@ page import="org.labkey.mq.view.search.ProteinSearchBean" %>
<%@ page import="org.labkey.mq.MqController" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add(ClientDependency.fromPath("Ext4ClientApi"));
    }
%>
<%
    JspView<ProteinSearchBean> me = (JspView<ProteinSearchBean>) HttpView.currentView();
    ProteinSearchBean bean = me.getModelBean();

    String proteinId = bean.getForm().getProteinId() != null ? bean.getForm().getProteinId() : "";
    String majorityProteinId = bean.getForm().getMajorityProteinId() != null ? bean.getForm().getMajorityProteinId() : "";
    String proteinName = bean.getForm().getProteinName() != null ? bean.getForm().getProteinName() : null;
    String geneName = bean.getForm().getGeneName() != null ? bean.getForm().getGeneName() : "";

    ActionURL proteinSearchUrl = new ActionURL(MqController.ProteinSearchAction.class, getContainer());

    String renderId = "protein-search-form-" + UniqueID.getRequestScopedUID(HttpView.currentRequest());
%>
<div id=<%=q(renderId)%>></div>

<script type="text/javascript">

    Ext4.onReady(function(){

        var form = Ext4.create('Ext.form.Panel', {
            renderTo: <%=q(renderId)%>,
            standardSubmit: true,
            border: false, frame: false,
            defaults: {
                labelWidth: 150,
                labelStyle: 'background-color: #E0E6EA; padding: 5px;'
            },
            items: [
                { xtype: 'hidden', name: 'X-LABKEY-CSRF', value: LABKEY.CSRF },
                {
                    xtype: 'textfield',
                    fieldLabel: 'Protein ID',
                    name: 'proteinId',
                    value: <%=q(proteinId)%>
                },
                {
                    xtype: 'textfield',
                    fieldLabel: 'Majority Protein ID',
                    name: 'majorityProteinId',
                    value: <%=q(majorityProteinId)%>
                },
                {
                    xtype: 'textfield',
                    fieldLabel: 'Protein Name',
                    name: 'proteinName',
                    value: <%=q(proteinName)%>
                },
                {
                    xtype: 'textfield',
                    fieldLabel: 'Gene Name',
                    name: 'geneName',
                    value: <%=q(geneName)%>
                },
                {
                    xtype: 'checkbox',
                    name: 'includeSubfolders',
                    fieldLabel: 'Search in subfolders',
                    inputValue: true,
                    checked: <%=bean.getForm().isIncludeSubfolders()%>,
                }
            ],
            buttonAlign: 'left',
            buttons: [{
                text: 'Search',
                handler: function(btn) {
                    var values = form.getForm().getValues();
                    form.submit({
                        url: <%=q(proteinSearchUrl.getLocalURIString())%>,
                        method: 'GET',
                        params: values
                    });
                }
            }],
        });
    });

</script>


